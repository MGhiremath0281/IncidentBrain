package com.incidentbbrain.aiservice.dto;

import java.util.List;

public record IncidentAnalysis(
        String rootCause,
        double confidenceScore,
        List<String> suggestedActions,
        String summary,
        String suspectedComponent
) {}