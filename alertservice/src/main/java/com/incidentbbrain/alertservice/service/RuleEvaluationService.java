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
    private static final double LATENCY_THRESHOLD = 0.006;// Testing purpose else set to 0.5 for the production

    public AlertRequest evaluate(List<MetricPoint> metrics) {
        String serviceName = "testing-service";
        double sumTime = 0;
        double countRequests = 0;

        for (MetricPoint m : metrics) {
            if (m.getName().equals("http_server_requests_seconds_sum")) sumTime = m.getValue();
            if (m.getName().equals("http_server_requests_seconds_count")) countRequests = m.getValue();
        }

        double avgLatency = (countRequests > 0) ? (sumTime / countRequests) : 0;
        String latencyKey = serviceName + "_HIGH_LATENCY";

        if (avgLatency > LATENCY_THRESHOLD) {
            Instant firstDetected = activeAlerts.get(latencyKey);

            if (firstDetected == null) {
                activeAlerts.put(latencyKey, Instant.now());
                log.info("STATE CHANGE: {} Latency ({}s) breached threshold ({}s)",
                        serviceName, String.format("%.4f", avgLatency), LATENCY_THRESHOLD);
                return buildRequest(serviceName, "HIGH_LATENCY", String.format("Latency: %.4f s", avgLatency));
            }

            long minutesActive = Duration.between(firstDetected, Instant.now()).toMinutes();
            if (minutesActive >= 5) {
                activeAlerts.put(latencyKey, Instant.now());
                log.warn("REMINDER: {} Latency still high (Active for {}m)", serviceName, minutesActive);
                return buildRequest(serviceName, "HIGH_LATENCY", String.format("STILL HIGH: %.4f s", avgLatency));
            }

            return null;

        } else if (avgLatency > 0 && avgLatency <= LATENCY_THRESHOLD) {
            if (activeAlerts.remove(latencyKey) != null) {
                log.info("RECOVERY: Latency for {} returned to normal (below {}s)", serviceName, LATENCY_THRESHOLD);
            }
        }

        return null;
    }

    private AlertRequest buildRequest(String service, String reason, String message) {
        return AlertRequest.builder()
                .serviceName(service)
                .severity(Severity.MEDIUM)
                .alertType("APPLICATION")
                .reason(reason)
                .source("PROMETHEUS")
                .message(message)
                .host("metrics-engine")
                .build();
    }
}