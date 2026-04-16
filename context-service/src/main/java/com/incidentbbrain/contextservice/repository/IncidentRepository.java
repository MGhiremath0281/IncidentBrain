package com.incidentbbrain.contextservice.repository;

import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<EnrichedIncident, UUID> {
    List<EnrichedIncident> findByService(String service);
}