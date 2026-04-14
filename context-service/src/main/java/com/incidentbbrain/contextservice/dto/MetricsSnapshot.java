package com.incidentbbrain.contextservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MetricsSnapshot {

    private double cpuUsage;
    private double memoryUsage;
    private double errorRate;
}