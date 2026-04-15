package com.incidentbbrain.alertservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertRequest {
    private String serviceName;
    private String severity;
    private String message;
    private String host;
}