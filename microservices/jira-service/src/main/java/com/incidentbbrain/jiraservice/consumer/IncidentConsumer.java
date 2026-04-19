package com.incidentbbrain.jiraservice.consumer;

import com.incidentbbrain.jiraservice.dto.AnalysisEvent;
import com.incidentbbrain.jiraservice.service.JiraOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentConsumer {
    private final JiraOrchestrator orchestrator;

    @KafkaListener(
            topics = "analysis.completed",
            groupId = "jira-orchestrator-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(AnalysisEvent event) {
        log.info("Received event for Incident: {}", event.getId());
        orchestrator.processIncident(event);
    }
}