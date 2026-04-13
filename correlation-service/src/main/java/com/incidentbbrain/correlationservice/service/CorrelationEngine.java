package com.incidentbbrain.correlationservice.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.kafka.event.IncidentEvent;
import com.incidentbbrain.correlationservice.kafka.producer.IncidentProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.incidentbbrain.correlationservice.kafka.event.AlertEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class CorrelationEngine {

    private final IncidentService incidentService;
    private final IncidentProducer producer;

    private final Map<String, List<AlertEvent>> windowMap = new ConcurrentHashMap<>();

    public void process(AlertEvent alert) {
        String service = alert.getServiceName();

        windowMap.computeIfAbsent(service, k -> new ArrayList<>()).add(alert);

        log.info("[ENGINE] Added alert. service={} count={}",
                service, windowMap.get(service).size());

        flushIfNeeded(service);
    }

    private void flushIfNeeded(String service) {
        List<AlertEvent> alerts = windowMap.get(service);

        if (alerts == null || alerts.isEmpty()) return;

        if (alerts.size() >= 2 || isWindowExpired(alerts)) {
            log.info("[ENGINE] Flushing alerts for service={}", service);

            Incident incident = incidentService.createIncident(service, alerts);

            IncidentEvent event = IncidentEvent.builder()
                    .id(incident.getId())
                    .service(service)
                    .severity(incident.getSeverity())
                    .alertIds(incident.getAlertIds())
                    .build();

            producer.publish(event);

            windowMap.remove(service);
        }
    }

    private boolean isWindowExpired(List<AlertEvent> alerts) {
        LocalDateTime first = alerts.get(0).getTimestamp();
        return first.plusMinutes(5).isBefore(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledFlush() {
        log.info("[SCHEDULER] Running window flush...");

        for (String service : windowMap.keySet()) {
            flushIfNeeded(service);
        }
    }
}
