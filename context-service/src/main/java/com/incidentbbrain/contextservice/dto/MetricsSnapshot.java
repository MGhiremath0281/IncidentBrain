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
public class MetricsSnapshot {
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private double errorRatePercent;
    private long requestsPerSecond;
    private double avgResponseTimeMs;
    private long activeConnections;
    private LocalDateTime capturedAt;
}
