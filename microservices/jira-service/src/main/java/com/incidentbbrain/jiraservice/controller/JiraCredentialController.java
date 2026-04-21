package com.incidentbbrain.jiraservice.controller;

import com.incidentbbrain.jiraservice.dto.JiraCredentialRequest;
import com.incidentbbrain.jiraservice.dto.JiraMetricsDto;
import com.incidentbbrain.jiraservice.model.JiraCredential;
import com.incidentbbrain.jiraservice.service.JiraCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jira/credentials")
@RequiredArgsConstructor
public class JiraCredentialController {

    private final JiraCredentialService service;

    /**
     * POST /api/jira/credentials
     * Register a new Jira credential set. The apiToken is AES-256 encrypted before storage.
     */
    @PostMapping
    public ResponseEntity<JiraCredential> create(@Valid @RequestBody JiraCredentialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    /**
     * GET /api/jira/credentials
     * List all saved credential sets. Tokens are always redacted in responses.
     */
    @GetMapping
    public List<JiraCredential> getAll() {
        return service.getAll();
    }

    /**
     * GET /api/jira/credentials/{id}
     * Get one credential set by ID. Token is redacted.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JiraCredential> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * PUT /api/jira/credentials/{id}
     * Update a credential set. New apiToken is re-encrypted with AES-256.
     */
    @PutMapping("/{id}")
    public ResponseEntity<JiraCredential> update(
            @PathVariable Long id,
            @Valid @RequestBody JiraCredentialRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /**
     * DELETE /api/jira/credentials/{id}
     * Remove a credential set.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/jira/credentials/{id}/test
     * Validate credentials by calling GET /rest/api/2/myself on Jira.
     * Returns "SUCCESS — connected as: <display name>" or "FAILED — <reason>".
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<String> test(@PathVariable Long id) {
        return ResponseEntity.ok(service.testConnection(id));
    }

    /**
     * GET /api/jira/credentials/metrics
     * Returns ticket and credential set counts for the dashboard.
     */
    @GetMapping("/metrics")
    public ResponseEntity<JiraMetricsDto> metrics() {
        return ResponseEntity.ok(service.getMetrics());
    }
}
