package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import com.incidentbbrain.contextservice.service.IncidentContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final IncidentContextService contextService;

    @GetMapping("/all")
    public ResponseEntity<List<EnrichedIncident>> getAll() {
        return ResponseEntity.ok(contextService.getAllEnrichedIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrichedIncident> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(contextService.getIncidentById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/service/{name}")
    public ResponseEntity<List<EnrichedIncident>> getByService(@PathVariable String name) {
        return ResponseEntity.ok(contextService.getIncidentsByService(name));
    }
}