package com.incidentbbrain.contextservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import com.incidentbbrain.incidentbraincommon.common.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActuatorMetricsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "http://testing-service:8084";

    public MetricsSnapshot getMetricsSnapshot(String service, Severity severity) {

        try {
            double cpu = fetchMetric("jvm.memory.used");
            double memory = fetchMetric("jvm.memory.max");
            double requests = fetchMetric("http.server.requests");

            return MetricsSnapshot.builder()
                    .cpuUsagePercent(cpu % 100) // basic approximation
                    .memoryUsagePercent(memory % 100)
                    .errorRatePercent(0.0) // you can enhance later
                    .requestsPerSecond((long) requests)
                    .avgResponseTimeMs(100.0) // placeholder
                    .activeConnections(10)
                    .capturedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch metrics from actuator", e);

            return MetricsSnapshot.builder()
                    .cpuUsagePercent(0)
                    .memoryUsagePercent(0)
                    .errorRatePercent(0)
                    .requestsPerSecond(0)
                    .avgResponseTimeMs(0)
                    .activeConnections(0)
                    .capturedAt(LocalDateTime.now())
                    .build();
        }
    }

    private double fetchMetric(String metricName) {
        try {
            String url = BASE_URL + "/actuator/metrics/" + metricName;

            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode measurements = root.path("measurements");

            if (measurements.isArray() && measurements.size() > 0) {
                return measurements.get(0).path("value").asDouble();
            }

        } catch (Exception e) {
            log.warn("Failed to fetch metric: {}", metricName);
        }

        return 0.0;
    }
}