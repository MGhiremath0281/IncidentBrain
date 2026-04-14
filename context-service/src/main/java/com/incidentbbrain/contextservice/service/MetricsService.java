package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import com.incidentbbrain.incidentbraincommon.common.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Mock implementation of metrics retrieval service.
 * In production, this would query Prometheus, Datadog, or similar systems.
 */
@Slf4j
@Service
public class MetricsService {

    private final Random random = new Random();

    /**
     * Retrieves a metrics snapshot for the specified service.
     * Metrics are adjusted based on incident severity to simulate realistic conditions.
     *
     * @param service the service name
     * @param severity the incident severity (affects metric values)
     * @return a snapshot of current metrics
     */
    public MetricsSnapshot getMetricsSnapshot(String service, Severity severity) {
        log.debug("Fetching metrics snapshot for service={}, severity={}", service, severity);

        MetricsSnapshot snapshot = generateMetrics(severity);

        log.debug("Generated metrics for service={}: cpu={}%, memory={}%, errorRate={}%",
                service,
                String.format("%.1f", snapshot.getCpuUsagePercent()),
                String.format("%.1f", snapshot.getMemoryUsagePercent()),
                String.format("%.2f", snapshot.getErrorRatePercent()));

        return snapshot;
    }

    private MetricsSnapshot generateMetrics(Severity severity) {
        // Base values that get adjusted by severity
        double cpuBase, memoryBase, errorRateBase;
        long rpsBase;
        double responseTimeBase;

        switch (severity) {
            case CRITICAL -> {
                cpuBase = 85 + random.nextDouble() * 14;      // 85-99%
                memoryBase = 88 + random.nextDouble() * 11;   // 88-99%
                errorRateBase = 15 + random.nextDouble() * 35; // 15-50%
                rpsBase = 50 + random.nextInt(100);            // Low due to issues
                responseTimeBase = 2000 + random.nextDouble() * 8000; // 2-10s
            }
            case HIGH -> {
                cpuBase = 70 + random.nextDouble() * 20;      // 70-90%
                memoryBase = 75 + random.nextDouble() * 15;   // 75-90%
                errorRateBase = 5 + random.nextDouble() * 15;  // 5-20%
                rpsBase = 100 + random.nextInt(200);
                responseTimeBase = 800 + random.nextDouble() * 2200; // 0.8-3s
            }
            case MEDIUM -> {
                cpuBase = 50 + random.nextDouble() * 25;      // 50-75%
                memoryBase = 55 + random.nextDouble() * 20;   // 55-75%
                errorRateBase = 1 + random.nextDouble() * 5;   // 1-6%
                rpsBase = 200 + random.nextInt(300);
                responseTimeBase = 200 + random.nextDouble() * 600; // 200-800ms
            }
            default -> { // LOW
                cpuBase = 30 + random.nextDouble() * 25;      // 30-55%
                memoryBase = 40 + random.nextDouble() * 20;   // 40-60%
                errorRateBase = 0.1 + random.nextDouble() * 1; // 0.1-1.1%
                rpsBase = 300 + random.nextInt(500);
                responseTimeBase = 50 + random.nextDouble() * 150; // 50-200ms
            }
        }

        return MetricsSnapshot.builder()
                .cpuUsagePercent(Math.min(99.9, cpuBase))
                .memoryUsagePercent(Math.min(99.9, memoryBase))
                .errorRatePercent(Math.min(100.0, errorRateBase))
                .requestsPerSecond(rpsBase)
                .avgResponseTimeMs(responseTimeBase)
                .activeConnections(10 + random.nextInt(90))
                .capturedAt(LocalDateTime.now())
                .build();
    }
}
