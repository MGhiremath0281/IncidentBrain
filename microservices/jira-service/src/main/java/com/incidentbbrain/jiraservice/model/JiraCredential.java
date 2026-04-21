package com.incidentbbrain.jiraservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jira_credentials")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;               // e.g. "production", "staging"

    @Column(nullable = false)
    private String baseUrl;            // e.g. https://myorg.atlassian.net

    @Column(nullable = false)
    private String userEmail;

    @Column(name = "api_token", columnDefinition = "TEXT")
    private String apiToken;

    @Column(nullable = false)
    private String projectKey;         // e.g. KAN

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
