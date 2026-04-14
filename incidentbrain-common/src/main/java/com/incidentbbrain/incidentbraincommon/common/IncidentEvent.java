package com.incidentbbrain.correlationservice.kafka.event;

import com.incidentbbrain.correlationservice.entity.Severity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;


@Builder
@Data
public class IncidentEvent {

    @Id
    private UUID id;
    private String service;
    private Severity severity;
    private List<UUID> alertIds;
}