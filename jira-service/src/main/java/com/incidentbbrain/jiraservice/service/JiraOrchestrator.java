package com.incidentbbrain.jiraservice.service;

import com.incidentbbrain.jiraservice.client.JiraClient;
import com.incidentbbrain.jiraservice.dto.AnalysisEvent;
import com.incidentbbrain.jiraservice.dto.JiraIssueRequest;
import com.incidentbbrain.jiraservice.model.JiraIncident;
import com.incidentbbrain.jiraservice.repository.JiraIncidentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraOrchestrator {

    private final JiraIncidentRepository repository;
    private final JiraClient jiraClient;

    @Value("${jira.project-key:KAN}")
    private String projectKey;

    @Transactional
    public String processIncident(AnalysisEvent event) {
        log.info("Processing Incident Event: {} [Score: {}]", event.getId(), event.getConfidenceScore());

        if (event.getConfidenceScore() < 0.2) {
            log.warn("Incident {} REJECTED: Confidence score too low.", event.getId());
            return "REJECTED";
        }

        if (repository.existsById(event.getId())) {
            log.info("Incident {} SKIPPED: Already exists in database.", event.getId());
            return "DUPLICATE";
        }

        log.info("Mapping incident to Jira request for project: {}", projectKey);

        // Map Event to Jira Request
        JiraIssueRequest request = JiraIssueRequest.builder()
                .fields(JiraIssueRequest.Fields.builder()
                        .project(JiraIssueRequest.Project.builder().key(projectKey).build())
                        .summary("[" + event.getSuspectedComponent() + "] Incident Analysis")
                        .description(formatDescription(event))
                        .issuetype(JiraIssueRequest.IssueType.builder().name("Task").build())
                        .labels(List.of("incident-brain", "ai-generated"))
                        .build())
                .build();

        try {
            String jiraKey = jiraClient.postIssue(request);
            log.info("Successfully created Jira Ticket: {}", jiraKey);

            repository.save(new JiraIncident(event.getId(), jiraKey, LocalDateTime.now()));
            log.info("Saved local record for Incident: {} -> {}", event.getId(), jiraKey);

            return jiraKey;
        } catch (Exception e) {
            log.error("Failed to complete orchestration for incident {}: {}", event.getId(), e.getMessage());
            throw e;
        }
    }

    private String formatDescription(AnalysisEvent event) {
        return "h2. AI Observations\n" + event.getSummary() + "\n\n" +
                "h2. Suggested Actions\n" +
                event.getSuggestedActions().stream().map(a -> "* " + a).collect(Collectors.joining("\n"));
    }
}