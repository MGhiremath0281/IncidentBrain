package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.AlertResponse;
import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.kafka.AlertKafkaProducer;
import com.incidentbbrain.alertservice.model.Alert;
import com.incidentbbrain.alertservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository repo;
    private final AlertKafkaProducer producer;

    public AlertResponse ingest(AlertRequest req) {
        log.info("Ingesting new alert from host: {} for service: {}", req.getHost(), req.getServiceName());

        Alert alert = Alert.builder()
                .serviceName(req.getServiceName())
                .severity(req.getSeverity())
                .message(req.getMessage())
                .host(req.getHost())
                .source("manual")
                .build();

        Alert saved = repo.save(alert);
        log.debug("Alert saved to database with ID: {}", saved.getId());

        producer.publish(saved);

        return map(saved);
    }

    public AlertResponse get(UUID id) {
        log.debug("Fetching alert details for ID: {}", id);
        Alert alert = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Alert not found for ID: {}", id);
                    return new RuntimeException("Alert not found");
                });
        return map(alert);
    }

    public Page<AlertResponse> search(String service, Severity severity, Pageable pageable) {
        log.info("Searching alerts for service: {} and severity: {}", service, severity);
        return repo.findByServiceNameAndSeverity(service, severity, pageable)
                .map(this::map);
    }

    public AlertResponse updateStatus(UUID id, AlertStatus status) {
        log.info("Updating status for alert ID: {} to {}", id, status);

        Alert alert = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Update failed: Alert ID {} not found", id);
                    return new RuntimeException("Alert not found");
                });

        AlertStatus oldStatus = alert.getStatus();
        alert.setStatus(status);
        Alert updated = repo.save(alert);

        log.info("Alert ID {} status changed from {} to {}", id, oldStatus, status);
        return map(updated);
    }
    public Page<AlertResponse> findAll(Pageable pageable) {
        log.info("Fetching all alerts");
        return repo.findAll(pageable).map(this::map);
    }

    private AlertResponse map(Alert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .serviceName(a.getServiceName())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}