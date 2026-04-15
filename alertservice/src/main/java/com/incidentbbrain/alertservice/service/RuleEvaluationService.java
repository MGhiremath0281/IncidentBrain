package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.MetricPoint;
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

        if (cpu > 0.7) {
            return AlertRequest.builder()
                    .serviceName("testing-service")
                    .severity("HIGH")
                    .message("CPU usage high: " + cpu)
                    .host("metrics-engine")
                    .build();
        }

        if (requests > 10) {
            return AlertRequest.builder()
                    .serviceName("testing-service")
                    .severity("MEDIUM")
                    .message("High request load detected: " + requests)
                    .build();
        }

        return null;
    }
}