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
        String title = event.getTitle() != null ? event.getTitle() : "";
        String reason = title.contains(" ") ? title.split(" ")[0] : "GENERIC_ERROR";

        log.info(">>>> [ENRICHMENT START] Incident: {} | Service: {}", event.getId(), event.getService());

        List<String> logs = logService.getLogs(event.getService(), event.getStartedAt(), reason);
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
        log.info("<<<< [ENRICHMENT COMPLETE] Incident: {} has been fully context-enriched.", event.getId());
        return payload;
    }

    private void saveToDatabase(ContextPayload p) {
        try {
            EnrichedIncident entity = EnrichedIncident.builder()
                    .incidentId(p.getIncidentId())
                    .service(p.getService())
                    .status(p.getStatus())
                    .severity(p.getSeverity())
                    .alertIds(p.getAlertIds())
                    .logs(p.getLogs())
                    .metrics(p.getMetricsSnapshot())
                    .incidentStartedAt(p.getIncidentStartedAt())
                    .enrichedAt(p.getEnrichedAt())
                    .build();

            repository.save(entity);
            log.info("[DATABASE] Persistence successful for Incident: {}", p.getIncidentId());
        } catch (Exception ex) {
            log.error("[DATABASE] CRITICAL Persistence failure: {}", ex.getMessage());
        }
    }
}