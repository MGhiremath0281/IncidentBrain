package com.incidentbbrain.jiraservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jira_incidents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JiraIncident {
    @Id private UUID id;
    private String jiraTicketId;
    private LocalDateTime processedAt;
}