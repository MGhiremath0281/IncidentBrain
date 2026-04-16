package com.incidentbbrain.contextservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActuatorMetricsService {

    private final DynamicEndpointRegistry registry;
    private final RestTemplate restTemplate;

    public MetricsSnapshot getMetrics(String serviceName) {
        String actuatorBase = String.format(registry.getActuatorTemplate(), serviceName);
        String metricsUrl = actuatorBase + "/metrics";
        String healthUrl = actuatorBase + "/health";

        log.info("Scraping context for service: {}", serviceName);

        try {
            // 1. Fetch Health Status (UP/DOWN/OUT_OF_SERVICE)
            String status = fetchHealthStatus(healthUrl);

            // 2. Fetch Performance Metrics
            Double cpu = fetchMetricValue(metricsUrl + "/system.cpu.usage");
            Double memory = fetchMetricValue(metricsUrl + "/jvm.memory.used");
            Double requests = fetchMetricValue(metricsUrl + "/http.server.requests");

            // 3. Optional: Add extra data like active threads
            Map<String, Object> extras = new HashMap<>();
            extras.put("jvm.threads.live", fetchMetricValue(metricsUrl + "/jvm.threads.live"));

            return MetricsSnapshot.builder()
                    .systemCpuUsage(cpu)
                    .jvmMemoryUsed(memory)
                    .httpRequests(requests)
                    .healthStatus(status)
                    .details(extras)
                    .fetchedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch full metrics for {}: {}", serviceName, e.getMessage());
            return MetricsSnapshot.builder()
                    .healthStatus("UNKNOWN")
                    .fetchedAt(LocalDateTime.now())
                    .build();
        }
    }

    private String fetchHealthStatus(String url) {
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            return response != null ? response.path("status").asText("UNKNOWN") : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Health status unavailable at {}: {}", url, e.getMessage());
            return "UNREACHABLE";
        }
    }

    private Double fetchMetricValue(String url) {
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("measurements")) {
                return response.path("measurements").get(0).path("value").asDouble();
            }
        } catch (Exception e) {
            log.trace("Metric not found at {}", url);
        }
        return 0.0;
    }
}