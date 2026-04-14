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
public class IncidentEvent {

    private UUID id;
    private String service;
    private String severity;
    private String status;
    private List<UUID> alertIds;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private String title;
}