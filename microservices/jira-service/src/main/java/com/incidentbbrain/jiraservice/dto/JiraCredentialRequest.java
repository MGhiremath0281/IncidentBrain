package com.incidentbbrain.jiraservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JiraCredentialRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "baseUrl is required")
    private String baseUrl;

    @NotBlank @Email(message = "userEmail must be a valid email")
    private String userEmail;

    @NotBlank(message = "apiToken is required")
    private String apiToken;

    @NotBlank(message = "projectKey is required")
    private String projectKey;

    private boolean active = true;
}
