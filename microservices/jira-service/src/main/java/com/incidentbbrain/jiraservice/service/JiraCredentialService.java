package com.incidentbbrain.jiraservice.service;

import com.incidentbbrain.jiraservice.dto.JiraCredentialRequest;
import com.incidentbbrain.jiraservice.dto.JiraMetricsDto;
import com.incidentbbrain.jiraservice.model.JiraCredential;
import com.incidentbbrain.jiraservice.repository.JiraCredentialRepository;
import com.incidentbbrain.jiraservice.repository.JiraIncidentRepository;
import com.incidentbbrain.jiraservice.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraCredentialService {

    private final JiraCredentialRepository credentialRepository;
    private final JiraIncidentRepository   incidentRepository;
    private final AesEncryptionUtil        aes;


    public JiraCredential create(JiraCredentialRequest req) {
        if (credentialRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("A credential set named '" + req.getName() + "' already exists");
        }
        JiraCredential credential = JiraCredential.builder()
                .name(req.getName())
                .baseUrl(req.getBaseUrl())
                .userEmail(req.getUserEmail())
                .apiToken(aes.encrypt(req.getApiToken()))   // ← encrypted before save
                .projectKey(req.getProjectKey())
                .active(req.isActive())
                .build();
        return credentialRepository.save(credential);
    }

    public List<JiraCredential> getAll() {
        // Never expose the raw token — mask it before returning
        return credentialRepository.findAll()
                .stream()
                .peek(c -> c.setApiToken("***REDACTED***"))
                .toList();
    }

    public JiraCredential getById(Long id) {
        JiraCredential c = findOrThrow(id);
        c.setApiToken("***REDACTED***");
        return c;
    }

    public JiraCredential update(Long id, JiraCredentialRequest req) {
        JiraCredential existing = findOrThrow(id);
        existing.setName(req.getName());
        existing.setBaseUrl(req.getBaseUrl());
        existing.setUserEmail(req.getUserEmail());
        existing.setApiToken(aes.encrypt(req.getApiToken()));  // re-encrypt on update
        existing.setProjectKey(req.getProjectKey());
        existing.setActive(req.isActive());
        return credentialRepository.save(existing);
    }

    public void delete(Long id) {
        credentialRepository.delete(findOrThrow(id));
    }


    public String testConnection(Long id) {
        JiraCredential cred = findOrThrow(id);
        String decryptedToken = aes.decrypt(cred.getApiToken());

        String auth = cred.getUserEmail() + ":" + decryptedToken;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes());

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(5));

        RestClient client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(cred.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            Map<?,?> response = client.get()
                    .uri("/rest/api/2/myself")
                    .retrieve()
                    .body(Map.class);

            String displayName = response != null ? (String) response.get("displayName") : "unknown";
            log.info("Test connection succeeded for credential '{}': logged in as {}", cred.getName(), displayName);
            return "SUCCESS — connected as: " + displayName;
        } catch (Exception e) {
            log.warn("Test connection failed for credential '{}': {}", cred.getName(), e.getMessage());
            return "FAILED — " + e.getMessage();
        }
    }


    public JiraMetricsDto getMetrics() {
        long totalCreated = incidentRepository.count();
        long activeCredentials = credentialRepository.findAll()
                .stream().filter(JiraCredential::isActive).count();

        return JiraMetricsDto.builder()
                .totalTicketsCreated(totalCreated)
                .activeCredentialSets(activeCredentials)
                .totalCredentialSets(credentialRepository.count())
                .build();
    }


    /** Returns a fully-built RestClient using the currently active credential. */
    public RestClient buildActiveRestClient() {
        JiraCredential cred = credentialRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No active Jira credentials found. Add credentials via POST /api/jira/credentials"));

        String decryptedToken = aes.decrypt(cred.getApiToken());
        String auth    = cred.getUserEmail() + ":" + decryptedToken;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes());

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(cred.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getActiveProjectKey() {
        return credentialRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active Jira credentials found"))
                .getProjectKey();
    }

    private JiraCredential findOrThrow(Long id) {
        return credentialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found with id: " + id));
    }
}
