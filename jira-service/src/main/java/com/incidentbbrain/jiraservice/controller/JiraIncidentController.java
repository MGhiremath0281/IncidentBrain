package com.incidentbbrain.jiraservice.controller;

import com.incidentbbrain.jiraservice.dto.AnalysisEvent;
import com.incidentbbrain.jiraservice.repository.JiraIncidentRepository;
import com.incidentbbrain.jiraservice.service.JiraOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jira")
@RequiredArgsConstructor
public class JiraIncidentController {
    private final JiraOrchestrator orchestrator;
    private final JiraIncidentRepository repository;

    @PostMapping("/trigger")
    public ResponseEntity<String> manualTrigger(@RequestBody AnalysisEvent event) {
        return ResponseEntity.ok(orchestrator.processIncident(event));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> getStatus(@PathVariable UUID id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}