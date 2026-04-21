package com.incidentbbrain.jiraservice.client;

import com.incidentbbrain.jiraservice.dto.JiraIssueRequest;
import com.incidentbbrain.jiraservice.service.JiraCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class JiraClient {

    private final JiraCredentialService credentialService;

    public String postIssue(JiraIssueRequest request) {
        try {
            RestClient jiraRestClient = credentialService.buildActiveRestClient();

            Map<String, Object> response = jiraRestClient.post()
                    .uri("/rest/api/2/issue")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("key")) {
                return (String) response.get("key");
            }

            throw new RuntimeException("Jira response missing ticket key");

        } catch (Exception e) {
            log.error("Jira API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create Jira ticket", e);
        }
    }
}