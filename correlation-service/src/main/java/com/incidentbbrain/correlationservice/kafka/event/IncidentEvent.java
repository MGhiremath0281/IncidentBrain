package com.incidentbbrain.correlationservice.kafka.event;

import com.incidentbbrain.correlationservice.entity.Severity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class IncidentEvent {
    private UUID id;
    private String service;
    private Severity severity;
    private List<UUID> alertIds;
}