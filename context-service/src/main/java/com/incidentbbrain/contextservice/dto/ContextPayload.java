package com.incidentbbrain.contextservice.dto;

import com.incidentbbrain.incidentbraincommon.common.Severity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextPayload {

    private UUID incidentId;
    private String service;
    private Severity severity;
    private String status;
    private String title;

    private List<UUID> alertIds;
    private LocalDateTime incidentStartedAt;
    private LocalDateTime resolvedAt;

    private List<String> logs;
    private DeploymentInfo deploymentInfo;
    private MetricsSnapshot metricsSnapshot;

    private LocalDateTime enrichedAt;
}