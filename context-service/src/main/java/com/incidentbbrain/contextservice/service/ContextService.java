package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.dto.IncidentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final LogService logService;
    private final MetricsService metricsService;
    private final DeploymentService deploymentService;

    public ContextPayload build(IncidentEvent event) {

        return ContextPayload.builder()
                .incidentId(event.getId())
                .service(event.getService())
                .severity(event.getSeverity())
                .title(event.getTitle())
                .startedAt(event.getStartedAt())

                .logs(logService.getLogs(event.getService(), event.getSeverity()))

                .metrics(metricsService.getMetrics(event.getService()))

                .deployment(deploymentService.getDeployment(event.getService()))

                .build();
    }
}