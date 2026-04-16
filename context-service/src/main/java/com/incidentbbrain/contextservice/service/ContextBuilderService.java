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

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextBuilderService {

    private final ElasticsearchLogService logService;
    private final ActuatorMetricsService metricsService;
    private final IncidentRepository repository;

    public ContextPayload enrich(IncidentEvent event) {
        log.info("Starting context enrichment for incident: {} from service: {}",
                event.getId(), event.getService());

        List<String> logs = logService.getLogs(event.getService(), event.getStartedAt());
        log.debug("Captured {} log lines from Elasticsearch", logs.size());

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
            log.info("Successfully persisted enriched context for incident ID: {}", p.getIncidentId());
        } catch (Exception ex) {
            log.error("CRITICAL: Database persistence failed for incident {}: {}",
                    p.getIncidentId(), ex.getMessage());
        }
    }
}