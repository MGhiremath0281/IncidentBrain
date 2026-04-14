package com.incidentbbrain.incidentbraincommon.common;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEvent {

    private UUID id;
    private String service;
    private Severity severity;
    private String status;
    private List<UUID> alertIds;

    private String title;

    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
}