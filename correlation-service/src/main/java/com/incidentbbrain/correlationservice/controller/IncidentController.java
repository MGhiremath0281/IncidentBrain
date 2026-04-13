package com.incidentbbrain.correlationservice.controller;

import com.incidentbbrain.correlationservice.dto.IncidentResponse;
import com.incidentbbrain.correlationservice.dto.ResolveIncidentRequest;
import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final IncidentService service;

    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        Incident incident = service.getIncident(id);
        return map(incident);
    }

    @PatchMapping("/{id}/resolve")
    public IncidentResponse resolveIncident(
            @PathVariable UUID id,
            @RequestBody ResolveIncidentRequest request) {

        Incident incident = service.resolveIncident(id, request.getResolvedAt());
        return map(incident);
    }

    private IncidentResponse map(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .service(incident.getAffectedService())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .alertIds(incident.getAlertIds())
                .startedAt(incident.getStartedAt())
                .resolvedAt(incident.getResolvedAt())
                .title(incident.getTitle())
                .build();
    }
}