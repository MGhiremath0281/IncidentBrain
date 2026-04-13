package com.incidentbbrain.correlationservice.service;


import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.kafka.event.AlertEvent;
import com.incidentbbrain.correlationservice.repository.IncidentRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Builder
public class IncidentService {

    private final IncidentRepository repository;

    public Incident createIncident(String service, List<AlertEvent> alerts) {

        Incident incident = Incident.builder()
                .affectedService(service)
                .severity(findMaxSeverity(alerts))
                .alertIds(alerts.stream().map(AlertEvent::getId).toList())
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
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(resolvedAt != null ? resolvedAt : LocalDateTime.now());

        log.info("[SERVICE] Incident resolved id={}", id);

        return repository.save(incident);
    }

    public Incident getIncident(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    private String findMaxSeverity(List<AlertEvent> alerts) {
        return alerts.stream()
                .map(AlertEvent::getSeverity)
                .max(String::compareTo)
                .orElse("LOW");
    }
}
