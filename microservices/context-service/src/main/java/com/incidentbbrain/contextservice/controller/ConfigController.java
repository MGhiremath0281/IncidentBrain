package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final DynamicEndpointRegistry registry;

    public ConfigController(DynamicEndpointRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/endpoints")
    public ResponseEntity<String> updateEndpoints(@RequestBody ConfigRequest request) {
        registry.setElasticsearchUrl(request.getEsUrl());
        registry.setActuatorTemplate(request.getMetricsTemplate());
        return ResponseEntity.ok("Infrastructure ports injected. Context Service is now active.");
    }

    @GetMapping("/current")
    public ResponseEntity<DynamicEndpointRegistry> getCurrent() {
        return ResponseEntity.ok(registry);
    }

    @Data
    public static class ConfigRequest {
        private String esUrl;
        private String metricsTemplate;
    }
}