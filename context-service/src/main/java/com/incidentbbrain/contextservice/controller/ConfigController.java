package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final DynamicEndpointRegistry registry;

    public ConfigController(DynamicEndpointRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/endpoints")
    public void updateEndpoints(@RequestParam String esUrl, @RequestParam String metricsTemplate) {
        registry.setElasticsearchUrl(esUrl);
        registry.setActuatorTemplate(metricsTemplate);
    }
}