package com.incidentbbrain.contextservice.config;

import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter @Setter
public class DynamicEndpointRegistry {
    private String elasticsearchUrl;
    private String actuatorTemplate;

    public boolean isConfigured() {
        return elasticsearchUrl != null && !elasticsearchUrl.isBlank() &&
                actuatorTemplate != null && !actuatorTemplate.isBlank();
    }
}