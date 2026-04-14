package com.incidentbbrain.contextservice.controller;

import com.incidentbbrain.contextservice.dto.IncidentEvent;
import com.incidentbbrain.contextservice.kafka.ContextConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ContextTestController {

    private final ContextConsumer contextConsumer;

    @PostMapping("/incident")
    public String sendTestIncident() {

        IncidentEvent event = IncidentEvent.builder()
                .id(UUID.randomUUID())
                .service("payment-service")
                .severity("CRITICAL")
                .status("OPEN")
                .title("Payment failure spike")
                .startedAt(LocalDateTime.now())
                .build();

        contextConsumer.consume(event);

        return "Test incident sent → check Kafka topic context.ready";
    }
}