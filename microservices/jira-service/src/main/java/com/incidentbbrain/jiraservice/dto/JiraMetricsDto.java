package com.incidentbbrain.jiraservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JiraMetricsDto {
    private long totalTicketsCreated;
    private long activeCredentialSets;
    private long totalCredentialSets;
}
