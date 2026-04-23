package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final DynamicEndpointRegistry registry;

    public ConfigController(DynamicEndpointRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/endpoints")
    public ResponseEntity<String> updateEndpoints(@RequestBody ConfigRequest request) {
        log.info("Received request to update infrastructure endpoints.");

        // Logging the values (be careful not to log credentials if they are part of the URL)
        log.debug("New ES URL: {}, New Metrics Template: {}", request.getEsUrl(), request.getMetricsTemplate());

        try {
            registry.setElasticsearchUrl(request.getEsUrl());
            registry.setActuatorTemplate(request.getMetricsTemplate());

            log.info("Successfully updated DynamicEndpointRegistry. Context Service is now active.");
            return ResponseEntity.ok("Infrastructure ports injected. Context Service is now active.");
        } catch (Exception e) {
            log.error("Failed to update infrastructure endpoints: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update configuration.");
        }
    }

    @GetMapping("/current")
    public ResponseEntity<DynamicEndpointRegistry> getCurrent() {
        log.debug("Fetching current dynamic endpoint configuration.");
        return ResponseEntity.ok(registry);
    }

    @Data
    public static class ConfigRequest {
        private String esUrl;
        private String metricsTemplate;
    }
}