package com.incidentbbrain.alertservice.kafka;

import com.incidentbbrain.alertservice.model.Alert;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Alert alert) {
        log.info("Preparing to publish alert event for service: {} with severity: {}",
                alert.getServiceName(), alert.getSeverity());

        AlertEvent event = AlertEvent.builder()
                .alertId(alert.getId().toString())
                .serviceName(alert.getServiceName())
                .severity(alert.getSeverity().name())
                .message(alert.getMessage())
                .host(alert.getHost())
                .timestamp(Instant.now().toString())
                .build();

        try {
            kafkaTemplate.send("alerts.raw", alert.getServiceName(), event);
            log.info("Successfully sent alert event to Kafka topic 'alerts.raw' for Alert ID: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to publish alert event to Kafka for Alert ID: {}. Error: {}",
                    alert.getId(), e.getMessage(), e);
        }
    }
}

//kafka-console-consumer --bootstrap-server kafka:9092 --topic alerts.raw --partition 0 --offset 0