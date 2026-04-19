package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import com.incidentbbrain.contextservice.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentContextService {

    private final IncidentRepository repository;

    public List<EnrichedIncident> getAllEnrichedIncidents() {
        log.debug("Fetching all enriched incidents from database");
        return repository.findAll();
    }

    public EnrichedIncident getIncidentById(UUID incidentId) {
        return repository.findById(incidentId)
                .orElseThrow(() -> {
                    log.warn("Incident context not found for ID: {}", incidentId);
                    return new RuntimeException("Incident not found");
                });
    }

    public List<EnrichedIncident> getIncidentsByService(String serviceName) {
        return repository.findByService(serviceName);
    }
}