package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
import com.incidentbbrain.contextservice.entity.EnrichedIncident;
import com.incidentbbrain.contextservice.repository.IncidentRepository;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderService {

    private final ElasticsearchLogService logService;
    private final ActuatorMetricsService metricsService;
    private final IncidentRepository repository;

    public ContextPayload enrich(IncidentEvent event) {
        // Extract reason (e.g., HIGH_LATENCY) from title for targeted forensics
        String title = event.getTitle() != null ? event.getTitle() : "";
        String reason = title.contains(" ") ? title.split(" ")[0] : "GENERIC_ERROR";

        log.info("Starting context enrichment | Incident: {} | Service: {} | Reason: {}",
                event.getId(), event.getService(), reason);

        // Fetch logs - passing 3 arguments: service, timestamp, and reason
        List<String> logs = logService.getLogs(event.getService(), event.getStartedAt(), reason);

        // Fetch performance metrics
        MetricsSnapshot metrics = metricsService.getMetrics(event.getService());

        ContextPayload payload = ContextPayload.builder()
                .incidentId(event.getId())
                .service(event.getService())
                .severity(event.getSeverity())
                .status(event.getStatus())
                .title(event.getTitle())
                .alertIds(event.getAlertIds())
                .incidentStartedAt(event.getStartedAt())
                .resolvedAt(event.getResolvedAt())
                .logs(logs)
                .metricsSnapshot(metrics)
                .enrichedAt(LocalDateTime.now())
                .build();

        saveToDatabase(payload);
        return payload;
    }

    private void saveToDatabase(ContextPayload p) {
        try {
            EnrichedIncident entity = EnrichedIncident.builder()
                    .incidentId(p.getIncidentId())
                    .service(p.getService())
                    .severity(p.getSeverity())
                    .alertIds(p.getAlertIds())
                    .logs(p.getLogs())
                    .metrics(p.getMetricsSnapshot())
                    .incidentStartedAt(p.getIncidentStartedAt())
                    .enrichedAt(p.getEnrichedAt())
                    .build();

            repository.save(entity);
            log.info("Successfully persisted enriched context for incident: {}", p.getIncidentId());
        } catch (Exception ex) {
            log.error("CRITICAL: DB persistence failed for incident {}: {}", p.getIncidentId(), ex.getMessage());
        }
    }
}