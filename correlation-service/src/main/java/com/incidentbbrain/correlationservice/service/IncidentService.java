package com.incidentbbrain.correlationservice.service;


import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.entity.Severity;
import com.incidentbbrain.correlationservice.repository.IncidentRepository;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository repository;

    public Incident createIncident(String service, List<AlertEvent> alerts) {

        Incident incident = Incident.builder()
                .affectedService(service)
                .severity(findMaxSeverity(alerts))
                .alertIds(alerts.stream()
                        .map(alert -> UUID.fromString(alert.getAlertId()))
                        .toList())
                .title("Incident in " + service)
                .status("OPEN")
                .startedAt(LocalDateTime.now())
                .build();

        Incident saved = repository.save(incident);

        log.info("[SERVICE] Incident created id={} service={}",
                saved.getId(), service);

        return saved;
    }

    public Incident resolveIncident(UUID id, LocalDateTime resolvedAt) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(resolvedAt != null ? resolvedAt : LocalDateTime.now());

        log.info("[SERVICE] Incident resolved id={}", id);

        return repository.save(incident);
    }

    public Incident getIncident(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
    }

    private Severity findMaxSeverity(List<AlertEvent> alerts) {
        return alerts.stream()
                // FIX: Convert String severity to Enum Severity
                .map(alert -> {
                    try {
                        return Severity.valueOf(alert.getSeverity().toUpperCase());
                    } catch (Exception e) {
                        return Severity.LOW; // Fallback if String doesn't match Enum
                    }
                })
                .max(Comparator.naturalOrder())
                .orElse(Severity.LOW);
    }

    public List<Incident> getAllIncidents() {
        log.info("[SERVICE] Fetching all incidents");
        return repository.findAll();
    }
}