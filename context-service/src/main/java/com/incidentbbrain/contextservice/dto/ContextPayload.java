package com.incidentbbrain.contextservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class ContextPayload {

    private UUID incidentId;
    private String service;
    private String severity;
    private String title;

    private List<String> logs;

    private MetricsSnapshot metrics;

    private DeploymentInfo deployment;

    private LocalDateTime startedAt;
}