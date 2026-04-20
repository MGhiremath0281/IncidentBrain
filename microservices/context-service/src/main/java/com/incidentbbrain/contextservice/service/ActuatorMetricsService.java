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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActuatorMetricsService {

    private final DynamicEndpointRegistry registry;
    private final RestTemplate restTemplate;

    // BUG FIX: The original code made 4 sequential HTTP calls (health + cpu + memory
    // + requests) one after the other. Each call can take up to the 5s read timeout,
    // meaning a single getMetrics() call could block for 20s in the worst case.
    // Using a small thread pool to fire all 4 requests in parallel cuts this down
    // to ~5s max (the slowest single call) instead of 20s.
    private final ExecutorService metricsFetcher = Executors.newFixedThreadPool(4);

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
            // Fire all 4 HTTP calls in parallel
            CompletableFuture<String> healthFuture = CompletableFuture.supplyAsync(
                    () -> fetchHealthStatus(actuatorBase + "/health"), metricsFetcher);

            CompletableFuture<Double> cpuFuture = CompletableFuture.supplyAsync(
                    () -> fetchMetricValue(actuatorBase + "/metrics/system.cpu.usage"), metricsFetcher);

            CompletableFuture<Double> memFuture = CompletableFuture.supplyAsync(
                    () -> fetchMetricValue(actuatorBase + "/metrics/jvm.memory.used"), metricsFetcher);

            CompletableFuture<Double> reqFuture = CompletableFuture.supplyAsync(
                    () -> fetchMetricValue(actuatorBase + "/metrics/http.server.requests"), metricsFetcher);

            CompletableFuture<Double> threadsFuture = CompletableFuture.supplyAsync(
                    () -> fetchMetricValue(actuatorBase + "/metrics/jvm.threads.live"), metricsFetcher);

            // Wait for all to complete
            CompletableFuture.allOf(healthFuture, cpuFuture, memFuture, reqFuture, threadsFuture).join();

            String status  = healthFuture.join();
            Double cpu     = cpuFuture.join();
            Double memory  = memFuture.join();
            Double requests = reqFuture.join();
            Double threads = threadsFuture.join();

            log.info("[METRICS] Stats fetched - CPU: {}, Memory: {}, Requests: {}, Health: {}", cpu, memory, requests, status);

            Map<String, Object> extras = new HashMap<>();
            extras.put("jvm.threads.live", threads);

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
                return response.path("measurements").get(0).path("value").asDouble();
            }
        } catch (Exception e) {
            log.trace("[METRICS] Metric not found at {}", url);
        }
        return 0.0;
    }
}