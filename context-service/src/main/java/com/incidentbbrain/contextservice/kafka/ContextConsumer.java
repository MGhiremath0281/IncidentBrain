package com.incidentbbrain.contextservice.kafka;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.contextservice.dto.IncidentEvent;
import com.incidentbbrain.contextservice.service.ContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContextConsumer {

    private final ContextService contextService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "incidents.created", groupId = "context-group")
    public void consume(IncidentEvent event) {
        // Build the enriched payload
        ContextPayload payload = contextService.build(event);

        // Send to the next topic in the pipeline
        String key = (event.getId() != null) ? event.getId().toString() : null;

        kafkaTemplate.send(
                "context.ready",
                key,
                payload
        );
    }
}