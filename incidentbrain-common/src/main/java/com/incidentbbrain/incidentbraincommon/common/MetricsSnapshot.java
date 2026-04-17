package com.incidentbbrain.incidentbraincommon.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricsSnapshot {
    private Double systemCpuUsage;
    private Double jvmMemoryUsed;
    private Double httpRequests;
    private String healthStatus;
    private LocalDateTime fetchedAt;

    private Map<String, Object> details;
}