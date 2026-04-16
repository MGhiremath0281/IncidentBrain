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
        // Safety check to prevent NPE before logging
        if (incident == null) {
            log.error("RECEIVED NULL PAYLOAD | Topic: {}, Offset: {}", topic, offset);
            return;
        }

        log.info("RECEIVED INCIDENT | Topic: {}, Partition: {}, Offset: {} | IncidentId: {}, Service: {}, StartedAt: {}",
                topic, partition, offset, incident.getId(), incident.getService(), incident.getStartedAt());

        try {
            // 1. Logic triggers: Fetch Logs, Metrics, and Persistence
            ContextPayload contextPayload = contextBuilderService.enrich(incident);

            // 2. Publish enriched snapshot
            contextProducer.publishContext(contextPayload);

            log.info("SUCCESS | Enriched and persisted incidentId: {}", incident.getId());

        } catch (Exception e) {
            log.error("ERROR | Failed to process incidentId: {} | Reason: {}",
                    incident.getId(), e.getMessage());
            // Re-throw so Kafka ErrorHandler (Backoff) can kick in
            throw e;
        }
    }
}