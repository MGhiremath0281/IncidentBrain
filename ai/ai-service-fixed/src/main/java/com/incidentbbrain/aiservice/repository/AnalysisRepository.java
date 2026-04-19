package com.incidentbbrain.aiservice.repository;

import com.incidentbbrain.aiservice.entity.IncidentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<IncidentAnalysis, UUID> {
}