package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.dto.DeploymentInfo;
import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates the context enrichment process by combining data from
 * log, deployment, and metrics services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderService {

    private final ElasticsearchLogService logService;
    private final ActuatorMetricsService metricsService;
    private final DeploymentService deploymentService;


    public ContextPayload buildContext(IncidentEvent incident) {

        log.info("Building context for incidentId={}, service={}, severity={}",
                incident.getId(), incident.getService(), incident.getSeverity());

        long startTime = System.currentTimeMillis();

        // 1. Fetch logs
        List<String> logs = logService.getLogsForService(
                incident.getService(),
                incident.getId(),
                incident.getStartedAt()
        );

        // 2. Fetch deployment info
        DeploymentInfo deploymentInfo = deploymentService.getDeploymentInfo(
                incident.getService()
        );

        // 3. Fetch metrics
        MetricsSnapshot metricsSnapshot = metricsService.getMetricsSnapshot(
                incident.getService(),
                incident.getSeverity()
        );

        // 4. Build payload
        ContextPayload payload = ContextPayload.builder()
                .incidentId(incident.getId())
                .service(incident.getService())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .title(incident.getTitle())
                .alertIds(incident.getAlertIds())   // ✅ IMPORTANT (you were missing this mapping)
                .incidentStartedAt(incident.getStartedAt())
                .resolvedAt(incident.getResolvedAt()) // ✅ important for lifecycle tracking
                .logs(logs)
                .deploymentInfo(deploymentInfo)
                .metricsSnapshot(metricsSnapshot)
                .enrichedAt(LocalDateTime.now())
                .build();

        long duration = System.currentTimeMillis() - startTime;

        log.info("Context built for incidentId={} in {}ms: logs={}, deploymentVersion={}, metrics=captured",
                incident.getId(),
                duration,
                logs.size(),
                deploymentInfo.getVersion()
        );

        return payload;
    }
}