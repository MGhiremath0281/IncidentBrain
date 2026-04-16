package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActuatorMetricsService {

    private final DynamicEndpointRegistry registry;
    private final RestTemplate restTemplate;

    public MetricsSnapshot getMetrics(String serviceName) {
        String url = String.format(registry.getActuatorTemplate(), serviceName) + "/metrics";

        log.info("Fetching metrics from dynamic URL: {}", url);

        try {
            return restTemplate.getForObject(url, MetricsSnapshot.class);
        } catch (Exception e) {
            log.error("Failed to fetch metrics from {}: {}", url, e.getMessage());
            return new MetricsSnapshot();
        }
    }
}