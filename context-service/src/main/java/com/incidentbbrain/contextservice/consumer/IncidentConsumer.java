package com.incidentbbrain.contextservice.consumer;

import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.producer.ContextProducer;
import com.incidentbbrain.contextservice.service.ContextBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for newly created incidents and triggers
 * the context enrichment pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentConsumer {

    private final ContextBuilderService contextBuilderService;
    private final ContextProducer contextProducer;

    @KafkaListener(
            topics = "${context.kafka.topics.incidents-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIncident(
            @Payload IncidentEvent incident,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received incident from topic={}, partition={}, offset={}: incidentId={}, service={}",
                topic, partition, offset, incident.getId(), incident.getService());

        try {
            // Build enriched context using IncidentEvent directly
            ContextPayload contextPayload = contextBuilderService.buildContext(incident);

            // Publish to downstream topic
            contextProducer.publishContext(contextPayload);

            log.info("Successfully processed incidentId={}", incident.getId());

        } catch (Exception e) {
            log.error("Failed to process incidentId={}: {}", incident.getId(), e.getMessage(), e);
            throw e;
        }
    }
}