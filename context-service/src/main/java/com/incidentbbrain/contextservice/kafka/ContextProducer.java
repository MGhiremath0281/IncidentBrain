package com.incidentbbrain.contextservice.kafka;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContextProducer {

    private final KafkaTemplate<String, ContextPayload> kafkaTemplate;

    public void send(ContextPayload payload) {

        kafkaTemplate.send(
                "context.ready",
                payload.getIncidentId().toString(),
                payload
        );
    }
}