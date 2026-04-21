package com.incidentbbrain.jiraservice.repository;

import com.incidentbbrain.jiraservice.model.JiraCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JiraCredentialRepository extends JpaRepository<JiraCredential, Long> {
    Optional<JiraCredential> findFirstByActiveTrue();
    boolean existsByName(String name);
}
