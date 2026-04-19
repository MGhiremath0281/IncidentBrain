package com.incidentbbrain.correlationservice.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
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

    // The key is now "serviceName:reason" (e.g., "testing-service:HIGH_LATENCY")
    private final Map<String, List<AlertEvent>> windowMap = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public void process(AlertEvent alert) {
        // Step 1: Create a unique grouping key based on service AND reason
        String groupKey = alert.getServiceName() + ":" + alert.getReason();

        locks.putIfAbsent(groupKey, new Object());

        synchronized (locks.get(groupKey)) {
            windowMap
                    .computeIfAbsent(groupKey, k -> new ArrayList<>())
                    .add(alert);

            log.info("[ENGINE] Added alert to group={} total_in_window={}",
                    groupKey, windowMap.get(groupKey).size());
        }

        flushIfNeeded(groupKey);
    }

    private void flushIfNeeded(String groupKey) {
        Object lock = locks.get(groupKey);
        if (lock == null) return;

        synchronized (lock) {
            List<AlertEvent> alerts = windowMap.get(groupKey);

            if (alerts == null || alerts.isEmpty()) return;

            // Flush if we have at least 2 alerts OR the window time has expired
            if (alerts.size() < 2 && !isWindowExpired(alerts)) {
                return;
            }

            List<AlertEvent> toProcess = new ArrayList<>(alerts);
            windowMap.put(groupKey, new ArrayList<>());

            log.info("[ENGINE] Flushing {} alerts for group={}",
                    toProcess.size(), groupKey);

            // Extract the original service name from the key
            String serviceName = groupKey.split(":")[0];

            // Step 2: Create the incident with the grouped alerts
            Incident incident = incidentService.createIncident(serviceName, toProcess);

            // Step 3: Build the event for Kafka
            IncidentEvent event = IncidentEvent.builder()
                    .id(incident.getId())
                    .service(serviceName)
                    .severity(incident.getSeverity())
                    .status(incident.getStatus())
                    .alertIds(incident.getAlertIds())
                    .title(incident.getTitle())
                    .startedAt(incident.getStartedAt())
                    .resolvedAt(incident.getResolvedAt())
                    .build();

            producer.publish(event);
        }
    }

    private boolean isWindowExpired(List<AlertEvent> alerts) {
        if (alerts == null || alerts.isEmpty()) return false;

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

        // Set to 2 minutes for testing, usually 5 in prod
        return first.plusMinutes(2).isBefore(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledFlush() {
        log.info("[SCHEDULER] Checking all windows for expiration...");
        Set<String> keys = new HashSet<>(windowMap.keySet());

        for (String key : keys) {
            flushIfNeeded(key);
        }
    }
}