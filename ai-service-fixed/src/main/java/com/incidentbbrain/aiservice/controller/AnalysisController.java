package com.incidentbbrain.aiservice.controller;

import com.incidentbbrain.aiservice.entity.IncidentAnalysis;
import com.incidentbbrain.aiservice.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRepository repository;

    @GetMapping("/all")
    public List<IncidentAnalysis> getAllAnalyses() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentAnalysis> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAll() {
        repository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}