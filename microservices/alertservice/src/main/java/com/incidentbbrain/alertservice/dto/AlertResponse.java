package com.incidentbbrain.alertservice.dto;

import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private UUID id;
    private String serviceName;
    private Severity severity;
    private String message;
    private AlertStatus status;
    private Instant createdAt;
}