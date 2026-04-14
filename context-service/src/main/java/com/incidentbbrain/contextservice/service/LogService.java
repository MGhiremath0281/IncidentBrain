package com.incidentbbrain.contextservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of log retrieval service.
 * In production, this would query Elasticsearch or a similar log aggregation system.
 */
@Slf4j
@Service
public class LogService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final List<String> ERROR_TEMPLATES = List.of(
            "ERROR [%s] Connection timeout after 30000ms to database host db-primary.internal:5432",
            "ERROR [%s] Failed to process request: NullPointerException at PaymentService.processTransaction(PaymentService.java:142)",
            "ERROR [%s] Circuit breaker 'inventory-service' is OPEN; fallback executed",
            "ERROR [%s] OutOfMemoryError: Java heap space - consider increasing -Xmx",
            "ERROR [%s] SSL handshake failed: certificate expired for upstream service"
    );

    private static final List<String> WARN_TEMPLATES = List.of(
            "WARN  [%s] Response time exceeded threshold: 2847ms (threshold: 2000ms)",
            "WARN  [%s] Connection pool near capacity: 47/50 active connections",
            "WARN  [%s] Retry attempt 3/5 for downstream service 'notification-service'",
            "WARN  [%s] Cache miss rate elevated: 34%% (normal: <10%%)",
            "WARN  [%s] Request queue depth: 156 (threshold: 100)"
    );

    private static final List<String> INFO_TEMPLATES = List.of(
            "INFO  [%s] Health check completed: status=degraded, latency=450ms",
            "INFO  [%s] Auto-scaling triggered: increasing replicas from 3 to 5",
            "INFO  [%s] Feature flag 'new-checkout-flow' evaluated: enabled=false",
            "INFO  [%s] Batch job 'daily-reconciliation' started, processing 12,847 records",
            "INFO  [%s] Graceful shutdown initiated, draining 23 active requests"
    );

    private final Random random = new Random();

    /**
     * Retrieves mock log entries for the given service and incident timeframe.
     *
     * @param service the service name to fetch logs for
     * @param incidentId the incident ID for correlation
     * @param since the start time for log retrieval
     * @return a list of formatted log entries
     */
    public List<String> getLogsForService(String service, UUID incidentId, LocalDateTime since) {
        log.debug("Fetching logs for service={}, incidentId={}, since={}", service, incidentId, since);

        List<String> logs = new ArrayList<>();
        LocalDateTime logTime = since != null ? since : LocalDateTime.now().minusMinutes(5);

        // Generate 3-5 log entries with realistic timestamps
        int logCount = 3 + random.nextInt(3);

        for (int i = 0; i < logCount; i++) {
            logTime = logTime.plusSeconds(random.nextInt(30) + 5);
            String timestamp = logTime.format(TIMESTAMP_FORMAT);

            String logEntry = selectLogEntry(timestamp, i, logCount);
            logs.add(String.format("[%s] %s", service.toUpperCase(), logEntry));
        }

        log.debug("Retrieved {} log entries for incidentId={}", logs.size(), incidentId);
        return logs;
    }

    private String selectLogEntry(String timestamp, int index, int total) {
        // First log is typically an ERROR that triggered the incident
        if (index == 0) {
            return String.format(
                    ERROR_TEMPLATES.get(random.nextInt(ERROR_TEMPLATES.size())),
                    timestamp
            );
        }

        // Last log might be INFO showing system response
        if (index == total - 1 && random.nextBoolean()) {
            return String.format(
                    INFO_TEMPLATES.get(random.nextInt(INFO_TEMPLATES.size())),
                    timestamp
            );
        }

        // Middle logs are a mix of WARN and ERROR
        if (random.nextBoolean()) {
            return String.format(
                    WARN_TEMPLATES.get(random.nextInt(WARN_TEMPLATES.size())),
                    timestamp
            );
        } else {
            return String.format(
                    ERROR_TEMPLATES.get(random.nextInt(ERROR_TEMPLATES.size())),
                    timestamp
            );
        }
    }
}
