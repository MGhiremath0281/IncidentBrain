package com.incidentbbrain.alertservice.dto;

import com.incidentbbrain.alertservice.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {
    @NotBlank
    private String serviceName;

    @NotNull
    private Severity severity;

    @NotBlank
    private String message;

    private String host;

    private String alertType;
    private String source;
    private String reason;
}