package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import com.incidentbbrain.contextservice.service.ContextBuilderService;
import com.incidentbbrain.contextservice.service.IncidentContextService;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/context/dashboard")
@RequiredArgsConstructor
public class ContextDashboardController {

    private final IncidentContextService contextService;
    private final ContextBuilderService builderService;

    @GetMapping("/active")
    public ResponseEntity<List<EnrichedIncident>> getActiveIncidents() {
        List<EnrichedIncident> all = contextService.getAllEnrichedIncidents();
        List<EnrichedIncident> active = all.stream()
                .filter(i -> !"RESOLVED".equalsIgnoreCase(i.getStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(active);
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<EnrichedIncident> getAnalysis(@PathVariable UUID id) {
        return ResponseEntity.ok(contextService.getIncidentById(id));
    }

    @GetMapping("/history/{service}")
    public ResponseEntity<List<EnrichedIncident>> getHistory(@PathVariable String service) {
        return ResponseEntity.ok(contextService.getIncidentsByService(service));
    }

    @PostMapping("/re-enrich/{id}")
    public ResponseEntity<ContextPayload> reEnrich(@PathVariable UUID id) {
        EnrichedIncident existing = contextService.getIncidentById(id);

        IncidentEvent event = new IncidentEvent();
        event.setId(existing.getIncidentId());
        event.setService(existing.getService());
        event.setSeverity(existing.getSeverity());
        event.setTitle(existing.getService() + " Manual Refresh");
        event.setStartedAt(existing.getIncidentStartedAt());

        return ResponseEntity.ok(builderService.enrich(event));
    }
}