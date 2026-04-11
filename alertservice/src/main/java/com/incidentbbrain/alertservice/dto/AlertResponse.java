package com.incidentbbrain.alertservice.dto;

import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AlertResponse {
    private UUID id;
    private String serviceName;
    private Severity severity;
    private String message;
    private AlertStatus status;
    private Instant createdAt;
}