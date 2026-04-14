package com.incidentbbrain.contextservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DeploymentInfo {

    private String version;
    private LocalDateTime deployedAt;
}