package com.incidentbbrain.correlationservice.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.kafka.event.IncidentEvent;
import com.incidentbbrain.correlationservice.kafka.producer.IncidentProducer;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CorrelationEngine {

    private final IncidentService incidentService;
    private final IncidentProducer producer;
    private final Map<String, List<AlertEvent>> windowMap = new ConcurrentHashMap<>();

    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public void process(AlertEvent alert) {
        String service = alert.getServiceName();

        locks.putIfAbsent(service, new Object());

        synchronized (locks.get(service)) {
            windowMap
                    .computeIfAbsent(service, k -> new ArrayList<>())
                    .add(alert);

            log.info("[ENGINE] Added alert service={} count={}",
                    service, windowMap.get(service).size());
        }

        flushIfNeeded(service);
    }

    private void flushIfNeeded(String service) {

        Object lock = locks.get(service);
        if (lock == null) return;

        synchronized (lock) {

            List<AlertEvent> alerts = windowMap.get(service);

            if (alerts == null || alerts.isEmpty()) return;

            if (alerts.size() < 2 && !isWindowExpired(alerts)) {
                return;
            }
            List<AlertEvent> toProcess = new ArrayList<>(alerts);
            windowMap.put(service, new ArrayList<>());

            log.info("[ENGINE] Flushing {} alerts for service={}",
                    toProcess.size(), service);

            Incident incident = incidentService.createIncident(service, toProcess);

            IncidentEvent event = IncidentEvent.builder()
                    .id(incident.getId())
                    .service(service)
                    .severity(incident.getSeverity())
                    .alertIds(incident.getAlertIds())
                    .build();

            producer.publish(event);
        }
    }

    private boolean isWindowExpired(List<AlertEvent> alerts) {
        if (alerts.isEmpty()) return false;

        Object rawTimestamp = alerts.get(0).getTimestamp();
        LocalDateTime first;

        if (rawTimestamp instanceof String) {
            first = LocalDateTime.parse(
                    (String) rawTimestamp,
                    DateTimeFormatter.ISO_DATE_TIME
            );
        } else {
            first = (LocalDateTime) rawTimestamp;
        }

        return first.plusMinutes(5).isBefore(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledFlush() {
        log.info("[SCHEDULER] Running window flush check...");

        Set<String> services = new HashSet<>(windowMap.keySet());
        for (String service : services) {
            flushIfNeeded(service);
        }
    }
}