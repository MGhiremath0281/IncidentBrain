package com.incidentbbrain.contextservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentInfo {
    private String version;
    private LocalDateTime deployedAt;
    private String environment;
    private String deployedBy;
    private String commitHash;
    private String artifactId;
}
