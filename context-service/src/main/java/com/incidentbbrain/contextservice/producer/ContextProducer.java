package com.incidentbbrain.contextservice.producer;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer that publishes enriched context payloads to the downstream topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextProducer {

    private final KafkaTemplate<String, ContextPayload> kafkaTemplate;

    @Value("${context.kafka.topics.context-ready}")
    private String contextReadyTopic;

    /**
     * Publishes an enriched context payload to the context.ready topic.
     * Uses the incident ID as the message key for partition affinity.
     *
     * @param payload the enriched context payload
     */
    public void publishContext(ContextPayload payload) {
        String key = payload.getIncidentId().toString();

        log.debug("Publishing context to topic={}, key={}", contextReadyTopic, key);

        CompletableFuture<SendResult<String, ContextPayload>> future =
                kafkaTemplate.send(contextReadyTopic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish context for incidentId={}: {}",
                        payload.getIncidentId(), ex.getMessage(), ex);
            } else {
                log.info("Published context to topic={}, partition={}, offset={}, incidentId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        payload.getIncidentId());
            }
        });
    }
}
