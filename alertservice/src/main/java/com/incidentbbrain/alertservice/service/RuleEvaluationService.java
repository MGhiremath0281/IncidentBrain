package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.MetricPoint;
import com.incidentbbrain.alertservice.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RuleEvaluationService {

    private final Map<String, Instant> activeAlerts = new ConcurrentHashMap<>();
    private static final Duration REPEAT_INTERVAL = Duration.ofMinutes(5);
    private static final double LATENCY_THRESHOLD = 0.006; // 6ms for testing change to 0.5 to production

    public AlertRequest evaluate(List<MetricPoint> metrics) {
        String serviceName = "testing-service";

        double sumTime = 0, countRequests = 0;
        double activeConns = -1, maxConns = -1;
        boolean isUp = true;

        for (MetricPoint m : metrics) {
            switch (m.getName()) {
                case "up" -> isUp = (m.getValue() == 1.0);
                case "http_server_requests_seconds_sum" -> sumTime = m.getValue();
                case "http_server_requests_seconds_count" -> countRequests = m.getValue();
                case "hikaricp_connections_active" -> activeConns = m.getValue();
                case "hikaricp_connections_max" -> maxConns = m.getValue();
            }
        }

        // --- RULE 1: SERVICE DOWN ---
        if (!isUp) {
            return processStatefulAlert(serviceName, "SERVICE_DOWN",
                    "CRITICAL: Service is unreachable", Severity.CRITICAL);
        }

        // --- RULE 2: DATABASE EXHAUSTION (The "Resume Flex") ---
        // Alert if active connections reach 90% of the pool maximum
        if (maxConns > 0 && (activeConns / maxConns) >= 0.9) {
            return processStatefulAlert(serviceName, "DATABASE_EXHAUSTED",
                    String.format("DB Pool bottleneck: %.0f/%.0f connections in use", activeConns, maxConns),
                    Severity.HIGH);
        }

        // --- RULE 3: HIGH LATENCY ---
        double avgLatency = (countRequests > 0) ? (sumTime / countRequests) : 0;
        String latencyKey = serviceName + "_HIGH_LATENCY";

        if (avgLatency > LATENCY_THRESHOLD) {
            return processStatefulAlert(serviceName, "HIGH_LATENCY",
                    String.format("Latency: %.4f s", avgLatency), Severity.MEDIUM);
        } else if (avgLatency > 0) {
            // Recovery logic
            if (activeAlerts.remove(latencyKey) != null) {
                log.info("RECOVERY: {} latency normalized to {}s", serviceName, String.format("%.4f", avgLatency));
            }
        }

        return null;
    }

    private AlertRequest processStatefulAlert(String service, String reason, String message, Severity severity) {
        String alertKey = service + "_" + reason;
        Instant now = Instant.now();
        Instant lastDetected = activeAlerts.get(alertKey);

        if (lastDetected == null) {
            // New alert
            activeAlerts.put(alertKey, now);
            log.info("STATE CHANGE: New Alert [{}] for {}", reason, service);
            return buildRequest(service, reason, message, severity);
        }

        if (Duration.between(lastDetected, now).compareTo(REPEAT_INTERVAL) >= 0) {
            activeAlerts.put(alertKey, now);
            log.warn("REMINDER: Alert [{}] still active for {}", reason, service);
            return buildRequest(service, reason, "STILL ACTIVE: " + message, severity);
        }

        return null; // Silent period
    }

    private AlertRequest buildRequest(String service, String reason, String message, Severity severity) {
        return AlertRequest.builder()
                .serviceName(service)
                .severity(severity)
                .alertType("APPLICATION")
                .reason(reason)
                .source("PROMETHEUS")
                .message(message)
                .host("metrics-engine")
                .build();
    }
}