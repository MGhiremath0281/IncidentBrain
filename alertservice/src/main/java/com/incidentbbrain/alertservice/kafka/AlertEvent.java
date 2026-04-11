package com.incidentbbrain.alertservice.kafka;

import com.incidentbbrain.alertservice.enums.Severity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertEvent {
    private String alertId;
    private String serviceName;
    private Severity severity;
    private String message;
    private String host;
    private String timestamp;
}