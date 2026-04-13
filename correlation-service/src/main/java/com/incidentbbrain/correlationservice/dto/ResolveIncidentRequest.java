package com.incidentbbrain.correlationservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResolveIncidentRequest {
    private LocalDateTime resolvedAt;
}
