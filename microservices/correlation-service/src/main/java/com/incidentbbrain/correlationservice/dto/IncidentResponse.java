package com.incidentbbrain.correlationservice.dto;


import com.incidentbbrain.incidentbraincommon.common.Severity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class IncidentResponse {
    private UUID id;
    private String service;
    private Severity severity;
    private String status;
    private List<UUID> alertIds;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private String title;
}
