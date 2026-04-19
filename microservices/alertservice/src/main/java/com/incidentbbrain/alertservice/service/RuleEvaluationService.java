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
    private static final double LATENCY_THRESHOLD = 0.006;

    public AlertRequest evaluate(List<MetricPoint> metrics) {
        String serviceName = "testing-service";

        double sumTime = 0, countRequests = 0;
        double activeConns = 0, maxConns = 0;
        boolean upMetricPresent = false;
        boolean isUp = true;

        // 1. Collect all metrics from the list
        for (MetricPoint m : metrics) {
            switch (m.getName()) {
                case "up" -> {
                    isUp = (m.getValue() == 1.0);
                    upMetricPresent = true;
                }
                case "http_server_requests_seconds_sum" -> sumTime = m.getValue();
                case "http_server_requests_seconds_count" -> countRequests = m.getValue();
                case "hikaricp_connections_active" -> activeConns = m.getValue();
                case "hikaricp_connections_max" -> maxConns = m.getValue();
            }
        }

        // --- RULE 1: SERVICE DOWN (Only if metric is actually present) ---
        if (upMetricPresent && !isUp) {
            return processStatefulAlert(serviceName, "SERVICE_DOWN", "Critical: Service is unreachable", Severity.CRITICAL);
        }

        // --- RULE 2: DATABASE EXHAUSTION ---
        // Using your metrics: activeConns=0.0, maxConns=10.0
        if (maxConns > 0 && (activeConns / maxConns) >= 0.9) {
            return processStatefulAlert(serviceName, "DATABASE_EXHAUSTED",
                    String.format("DB Pool nearly full: %.0f/%.0f", activeConns, maxConns), Severity.HIGH);
        }

        // --- RULE 3: HIGH LATENCY (Your working logic) ---
        double avgLatency = (countRequests > 0) ? (sumTime / countRequests) : 0;
        String latencyKey = serviceName + "_HIGH_LATENCY";

        if (avgLatency > LATENCY_THRESHOLD) {
            return processStatefulAlert(serviceName, "HIGH_LATENCY",
                    String.format("Latency breached: %.4f s", avgLatency), Severity.MEDIUM);
        } else if (avgLatency > 0) {
            if (activeAlerts.remove(latencyKey) != null) {
                log.info("RECOVERY: Latency for {} returned to normal", serviceName);
            }
        }

        return null;
    }

    private AlertRequest processStatefulAlert(String service, String reason, String message, Severity sev) {
        String key = service + "_" + reason;
        Instant now = Instant.now();
        Instant firstDetected = activeAlerts.get(key);

        if (firstDetected == null) {
            activeAlerts.put(key, now);
            log.info("STATE CHANGE: {} {} detected", service, reason);
            return buildRequest(service, reason, message, sev);
        }

        // Re-alert after 5 minutes (Your working reminder logic)
        if (Duration.between(firstDetected, now).toMinutes() >= 5) {
            activeAlerts.put(key, now); // Reset timer
            return buildRequest(service, reason, "STILL ACTIVE: " + message, sev);
        }

        return null;
    }

    private AlertRequest buildRequest(String service, String reason, String message, Severity sev) {
        return AlertRequest.builder()
                .serviceName(service)
                .severity(sev)
                .alertType("APPLICATION")
                .reason(reason)
                .source("PROMETHEUS")
                .message(message)
                .host("metrics-engine")
                .build();
    }
}