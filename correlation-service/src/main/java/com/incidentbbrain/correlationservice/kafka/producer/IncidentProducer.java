package com.incidentbbrain.correlationservice.kafka.producer;

import com.incidentbbrain.correlationservice.kafka.event.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentProducer {

    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;

    public void publish(IncidentEvent event) {
        log.info("[KAFKA-PRODUCER] Sending incident: {}", event.getId());
        kafkaTemplate.send("incidents.created", event);
    }
}