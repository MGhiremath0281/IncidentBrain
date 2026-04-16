package com.incidentbbrain.contextservice.config;

import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter @Setter
public class DynamicEndpointRegistry {
    private String elasticsearchUrl = "http://localhost:9200/logs-*/_search";
    private String actuatorTemplate = "http://%s:8084/actuator";
}