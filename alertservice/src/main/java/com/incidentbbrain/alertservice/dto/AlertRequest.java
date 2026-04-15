package com.incidentbbrain.alertservice.dto;

import com.incidentbbrain.alertservice.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertRequest {
    @NotBlank
    private String serviceName;
    @NotNull
    private Severity severity;
    @NotBlank private String message;
    private String host;
}