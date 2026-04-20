package com.incidentbbrain.contextservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
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
        if (!registry.isConfigured()) {
            log.warn("[METRICS] Aborting: No Actuator template configured in registry.");
            return MetricsSnapshot.builder().healthStatus("UNCONFIGURED").fetchedAt(LocalDateTime.now()).build();
        }

        String actuatorBase = registry.getActuatorTemplate().contains("%s")
                ? String.format(registry.getActuatorTemplate(), serviceName)
                : registry.getActuatorTemplate();

        log.info("[METRICS] Initiating scrape for service: {} via URL: {}", serviceName, actuatorBase);

        try {
            String status = fetchHealthStatus(actuatorBase + "/health");
            log.info("[METRICS] Service {} Health: {}", serviceName, status);

            Double cpu = fetchMetricValue(actuatorBase + "/metrics/system.cpu.usage");
            Double memory = fetchMetricValue(actuatorBase + "/metrics/jvm.memory.used");
            Double requests = fetchMetricValue(actuatorBase + "/metrics/http.server.requests");

            log.info("[METRICS] Stats fetched - CPU: {}, Memory: {}, Requests: {}", cpu, memory, requests);

            Map<String, Object> extras = new HashMap<>();
            extras.put("jvm.threads.live", fetchMetricValue(actuatorBase + "/metrics/jvm.threads.live"));

            return MetricsSnapshot.builder()
                    .systemCpuUsage(cpu).jvmMemoryUsed(memory).httpRequests(requests)
                    .healthStatus(status).details(extras).fetchedAt(LocalDateTime.now()).build();
        } catch (Exception e) {
            log.error("[METRICS] Critical failure fetching metrics for {}: {}", serviceName, e.getMessage());
            return MetricsSnapshot.builder().healthStatus("UNKNOWN").fetchedAt(LocalDateTime.now()).build();
        }
    }

    private String fetchHealthStatus(String url) {
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            return response != null ? response.path("status").asText("UNKNOWN") : "UNKNOWN";
        } catch (Exception e) {
            log.warn("[METRICS] Health check failed at {}: {}", url, e.getMessage());
            return "UNREACHABLE";
        }
    }

    private Double fetchMetricValue(String url) {
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("measurements")) {
                double val = response.path("measurements").get(0).path("value").asDouble();
                return val;
            }
        } catch (Exception e) {
            log.trace("[METRICS] Metric not found at {}", url);
        }
        return 0.0;
    }
}