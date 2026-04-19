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
        log.info("Ingesting new {} alert for service: {} (Reason: {})",
                req.getAlertType(), req.getServiceName(), req.getReason());

        Alert alert = Alert.builder()
                .serviceName(req.getServiceName())
                .severity(req.getSeverity())
                .message(req.getMessage())
                .host(req.getHost())
                .alertType(req.getAlertType())
                .source(req.getSource() != null ? req.getSource() : "manual")
                .reason(req.getReason())
                .status(AlertStatus.OPEN)
                .build();

        Alert saved = repo.save(alert);
        log.debug("Alert saved with ID: {}", saved.getId());

        // Publish enriched event to Kafka
        producer.publish(saved);

        return map(saved);
    }

    public AlertResponse get(UUID id) {
        Alert alert = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        return map(alert);
    }

    public Page<AlertResponse> search(String service, Severity severity, Pageable pageable) {
        return repo.findByServiceNameAndSeverity(service, severity, pageable).map(this::map);
    }

    public AlertResponse updateStatus(UUID id, AlertStatus status) {
        Alert alert = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setStatus(status);
        return map(repo.save(alert));
    }

    public Page<AlertResponse> findAll(Pageable pageable) {
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