package com.incidentbbrain.jiraservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisEvent {
    private UUID id;
    private String rootCause;
    private double confidenceScore;
    private List<String> suggestedActions;
    private String summary;
    private String suspectedComponent;
}