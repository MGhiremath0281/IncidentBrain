package com.incidentbbrain.aiservice.dto;


import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
import com.incidentbbrain.incidentbraincommon.common.Severity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class EnrichedIncidentDTO {
    private UUID incidentId;
    private String service;
    private Severity severity;
    private List<UUID> alertIds;
    private List<String> logs;

    @JsonProperty("metricsSnapshot")
    private MetricsSnapshot metrics;

    private LocalDateTime incidentStartedAt;
}