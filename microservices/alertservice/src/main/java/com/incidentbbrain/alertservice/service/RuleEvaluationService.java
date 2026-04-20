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

    // Storage for active alerts to show on the dashboard
    private final Map<String, Instant> activeAlerts = new ConcurrentHashMap<>();
    private static final Duration REPEAT_INTERVAL = Duration.ofMinutes(5);

    public Map<String, Instant> getActiveAlerts() {
        return activeAlerts;
    }

    public AlertRequest evaluate(List<MetricPoint> metrics, String serviceName, Double customThreshold) {
        double sumTime = 0, countRequests = 0;
        double activeConns = 0, maxConns = 0;
        boolean upMetricPresent = false;
        boolean isUp = true;

        // 1. Parse metrics into local variables
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

        // --- RULE 1: SERVICE DOWN (Critical) ---
        if (upMetricPresent && !isUp) {
            return processStatefulAlert(serviceName, "SERVICE_DOWN",
                    "Critical: Service is unreachable", Severity.CRITICAL);
        }

        // --- RULE 2: DATABASE EXHAUSTION (High) ---
        if (maxConns > 0 && (activeConns / maxConns) >= 0.9) {
            return processStatefulAlert(serviceName, "DATABASE_EXHAUSTED",
                    String.format("DB Pool full: %.0f/%.0f", activeConns, maxConns), Severity.HIGH);
        }

        // --- RULE 3: HIGH LATENCY (Medium - uses dashboard-defined threshold) ---
        double avgLatency = (countRequests > 0) ? (sumTime / countRequests) : 0;
        String latencyKey = serviceName + "_HIGH_LATENCY";

        if (avgLatency > customThreshold) {
            return processStatefulAlert(serviceName, "HIGH_LATENCY",
                    String.format("Latency %.4f s (Threshold: %.4f s)", avgLatency, customThreshold), Severity.MEDIUM);
        } else if (avgLatency > 0) {
            if (activeAlerts.remove(latencyKey) != null) {
                log.info("RECOVERY: {} is back to normal performance", serviceName);
            }
        }

        return null;
    }

    private AlertRequest processStatefulAlert(String service, String reason, String message, Severity sev) {
        String key = service + "_" + reason;
        Instant now = Instant.now();
        Instant firstDetected = activeAlerts.get(key);

        // New Alert Detected
        if (firstDetected == null) {
            activeAlerts.put(key, now);
            log.info("STATE CHANGE: {} {} detected", service, reason);
            return buildRequest(service, reason, message, sev);
        }

        // Reminder Logic: Re-send alert every 5 minutes if still broken
        if (Duration.between(firstDetected, now).compareTo(REPEAT_INTERVAL) >= 0) {
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
                .host("incident-brain-engine")
                .build();
    }
}