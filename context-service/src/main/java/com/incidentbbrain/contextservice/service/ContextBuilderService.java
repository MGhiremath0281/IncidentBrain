package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import com.incidentbbrain.contextservice.repository.IncidentRepository;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates the context enrichment process by combining data from
 * log, and metrics services, then persists to PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderService {

    private final ElasticsearchLogService logService;
    private final ActuatorMetricsService metricsService;
    private final IncidentRepository repository;

    public ContextPayload enrich(IncidentEvent event) {
        log.info("Enriching context for incident: {}", event.getId());

        // 1. Fetch logs using the 5-minute time window logic
        List<String> logs = logService.getLogs(event.getService(), event.getStartedAt());

        // 2. Fetch metrics using the dynamic Actuator URL
        MetricsSnapshot metrics = metricsService.getMetrics(event.getService());

        // 3. Construct the Payload
        ContextPayload payload = ContextPayload.builder()
                .incidentId(event.getId())
                .service(event.getService())
                .severity(event.getSeverity()) // Added to match your DTO
                .status(event.getStatus())     // Added to match your DTO
                .title(event.getTitle())       // Added to match your DTO
                .incidentStartedAt(event.getStartedAt())
                .resolvedAt(event.getResolvedAt())
                .logs(logs)
                .metricsSnapshot(metrics)
                .enrichedAt(LocalDateTime.now())
                .build();

        // 4. Save to Postgres for reference
        try {
            repository.save(mapToEntity(payload, event));
            log.info("Successfully persisted enriched incident {} to database", event.getId());
        } catch (Exception e) {
            log.error("Failed to save enriched incident to database: {}", e.getMessage());
        }

        return payload;
    }

    private EnrichedIncident mapToEntity(ContextPayload p, IncidentEvent e) {
        return EnrichedIncident.builder()
                .incidentId(p.getIncidentId())
                .service(p.getService())
                .severity(p.getSeverity())
                .logs(p.getLogs())
                .metrics(p.getMetricsSnapshot())
                .incidentStartedAt(e.getStartedAt())
                .enrichedAt(p.getEnrichedAt())
                .build();
    }
}