package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    public MetricsSnapshot getMetrics(String service) {

        return MetricsSnapshot.builder()
                .cpuUsage(83.4)
                .memoryUsage(76.1)
                .errorRate(0.69)
                .build();
    }
}