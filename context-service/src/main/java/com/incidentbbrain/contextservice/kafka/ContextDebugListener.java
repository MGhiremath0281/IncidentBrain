package com.incidentbbrain.contextservice.kafka;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContextDebugListener {

    @KafkaListener(topics = "context.ready", groupId = "debug-group")
    public void listen(ContextPayload payload) {
        log.info("=== CONTEXT READY RECEIVED ===");
        log.info("Incident ID: {}", payload.getIncidentId());
        log.info("Service:     {}", payload.getService());
        log.info("Severity:    {}", payload.getSeverity());
        log.info("Logs Count:  {}", payload.getLogs() != null ? payload.getLogs().size() : 0);
        log.info("==============================");
    }
}