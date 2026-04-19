package com.incidentbbrain.jiraservice.repository;

import com.incidentbbrain.jiraservice.model.JiraIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JiraIncidentRepository extends JpaRepository<JiraIncident, UUID> {}