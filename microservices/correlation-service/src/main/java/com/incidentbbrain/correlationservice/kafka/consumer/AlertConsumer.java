package com.incidentbbrain.correlationservice.kafka.consumer;

import com.incidentbbrain.correlationservice.service.CorrelationEngine;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConsumer {

    private final CorrelationEngine correlationEngine;

    @KafkaListener(topics = "alerts.raw", groupId = "correlation-group")
    public void consume(AlertEvent alert) {

        log.info("[KAFKA-CONSUMER] Received alert: {} for service: {}",
                alert.getAlertId(),
                alert.getServiceName());

        correlationEngine.process(alert);
    }
}