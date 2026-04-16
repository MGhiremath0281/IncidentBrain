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
 * the dynamic enrichment pipeline (Logs, Metrics, Persistence).
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
        log.info("RECEIVED INCIDENT | Topic: {}, Partition: {}, Offset: {} | IncidentId: {}, Service: {}",
                topic, partition, offset, incident.getId(), incident.getService());

        try {
            // 1. Logic triggers: Fetch Logs (Time-Window), Fetch Metrics (Dynamic URL), and Save to Postgres
            ContextPayload contextPayload = contextBuilderService.enrich(incident);

            // 2. Publish enriched snapshot to downstream Kafka topic
            contextProducer.publishContext(contextPayload);

            log.info("SUCCESS | Enriched and persisted incidentId: {}", incident.getId());

        } catch (Exception e) {
            log.error("ERROR | Failed to process incidentId: {} | Reason: {}", incident.getId(), e.getMessage(), e);
            // Re-throwing allows Kafka to handle retries if configured
            throw e;
        }
    }
}