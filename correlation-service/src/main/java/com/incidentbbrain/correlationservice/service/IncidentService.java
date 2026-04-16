package com.incidentbbrain.correlationservice.service;

import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.repository.IncidentRepository;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import com.incidentbbrain.incidentbraincommon.common.Severity;
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
        // Ensure we capture the start time immediately
        LocalDateTime now = LocalDateTime.now();

        Incident incident = Incident.builder()
                .affectedService(service)
                .severity(findMaxSeverity(alerts))
                .alertIds(alerts.stream()
                        .map(alert -> UUID.fromString(alert.getAlertId()))
                        .toList())
                .title("Incident in " + service)
                .status("OPEN")
                .startedAt(now) // Crucial: This populates the field for Kafka
                .build();

        Incident saved = repository.save(incident);

        log.info("[SERVICE] Incident created id={} service={} startedAt={}",
                saved.getId(), service, now);

        return saved;
    }

    public Incident resolveIncident(UUID id, LocalDateTime resolvedAt) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));

        incident.setStatus("RESOLVED");
        // Fallback to now if resolvedAt is null
        incident.setResolvedAt(resolvedAt != null ? resolvedAt : LocalDateTime.now());

        log.info("[SERVICE] Incident resolved id={} at {}", id, incident.getResolvedAt());

        return repository.save(incident);
    }

    public Incident getIncident(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));
    }

    private Severity findMaxSeverity(List<AlertEvent> alerts) {
        if (alerts == null || alerts.isEmpty()) return Severity.LOW;

        return alerts.stream()
                .map(alert -> {
                    try {
                        // Check if severity is already an Enum or a String
                        return Severity.valueOf(alert.getSeverity().toUpperCase());
                    } catch (Exception e) {
                        log.warn("Unknown severity '{}', defaulting to LOW", alert.getSeverity());
                        return Severity.LOW;
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