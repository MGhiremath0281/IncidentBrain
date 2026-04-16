package com.incidentbbrain.contextservice.repository;

import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<EnrichedIncident, UUID> {
}