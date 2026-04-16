package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.MetricPoint;
import com.incidentbbrain.alertservice.enums.Severity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEvaluationService {

    public AlertRequest evaluate(List<MetricPoint> metrics) {

        double cpu = 0;
        double requests = 0;

        for (MetricPoint m : metrics) {
            if (m.getName().contains("process_cpu_usage")) {
                cpu = m.getValue();
            }

            if (m.getName().contains("http_server_requests_seconds_count")) {
                requests += m.getValue();
            }
        }

        // Rule 1: High CPU usage
        if (cpu > 0.7) {
            return AlertRequest.builder()
                    .serviceName("testing-service")
                    .severity(Severity.CRITICAL) // Use the Enum constant here
                    .message("CPU usage high: " + cpu)
                    .host("metrics-engine")
                    .build();
        }

        // Rule 2: High Request Load
        if (requests > 10) {
            return AlertRequest.builder()
                    .serviceName("testing-service")
                    .severity(Severity.MEDIUM) // Use the Enum constant here
                    .message("High request load detected: " + requests)
                    .host("metrics-engine") // Added host for consistency
                    .build();
        }

        return null;
    }
}