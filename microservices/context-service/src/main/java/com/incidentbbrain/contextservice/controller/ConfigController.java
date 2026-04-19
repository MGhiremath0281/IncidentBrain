package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final DynamicEndpointRegistry registry;

    public ConfigController(DynamicEndpointRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/endpoints")
    public String updateEndpoints(@RequestBody ConfigRequest request) {
        registry.setElasticsearchUrl(request.getEsUrl());
        registry.setActuatorTemplate(request.getMetricsTemplate());
        return "Endpoints updated successfully!";
    }

    // Simple DTO to map the JSON body
    @Data
    public static class ConfigRequest {
        private String esUrl;
        private String metricsTemplate;
    }
}