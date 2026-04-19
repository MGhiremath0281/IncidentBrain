package com.incidentbbrain.jiraservice.service;

import com.incidentbbrain.jiraservice.client.JiraClient;
import com.incidentbbrain.jiraservice.dto.AnalysisEvent;
import com.incidentbbrain.jiraservice.dto.JiraIssueRequest;
import com.incidentbbrain.jiraservice.model.JiraIncident;
import com.incidentbbrain.jiraservice.repository.JiraIncidentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JiraOrchestrator {

    private final JiraIncidentRepository repository;
    private final JiraClient jiraClient;

    @Value("${jira.project-key:KAN}")
    private String projectKey;

    @Transactional
    public String processIncident(AnalysisEvent event) {
        if (event.getConfidenceScore() < 0.6) return "REJECTED";
        if (repository.existsById(event.getId())) return "DUPLICATE";

        // Map Event to Jira Request
        JiraIssueRequest request = JiraIssueRequest.builder()
                .fields(JiraIssueRequest.Fields.builder()
                        .project(JiraIssueRequest.Project.builder().key(projectKey).build())
                        .summary("[" + event.getSuspectedComponent() + "] Incident Analysis")
                        .description(formatDescription(event))
                        .issuetype(JiraIssueRequest.IssueType.builder().name("Bug").build())
                        .labels(List.of("incident-brain", "ai-generated"))
                        .build())
                .build();

        // Call Client
        String jiraKey = jiraClient.postIssue(request);

        // Save to DB
        repository.save(new JiraIncident(event.getId(), jiraKey, LocalDateTime.now()));

        return jiraKey;
    }

    private String formatDescription(AnalysisEvent event) {
        return "h2. AI Observations\n" + event.getSummary() + "\n\n" +
                "h2. Suggested Actions\n" +
                event.getSuggestedActions().stream().map(a -> "* " + a).collect(Collectors.joining("\n"));
    }
}