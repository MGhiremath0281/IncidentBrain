package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.client.PrometheusClient;
import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.MetricPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsIngestionService {

    private final PrometheusClient prometheusClient;
    private final PrometheusParserService parserService;
    private final RuleEvaluationService ruleService;
    private final RestTemplate restTemplate;

    private final Map<String, TargetConfig> monitoringTargets = new ConcurrentHashMap<>();
    private final Map<String, LiveStatus> liveStatusMap = new ConcurrentHashMap<>();
    private static final String ALERT_INGEST_URL = "http://localhost:8081/alerts/ingest";

    public record TargetConfig(String url, String name, Double threshold) {}

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LiveStatus {
        private String name;
        private boolean up;
        private double latency;
        private double dbUsagePercent;
        private String lastUpdate;
    }

    public void subscribe(String url, String name, Double threshold) {
        monitoringTargets.put(url, new TargetConfig(url, name, threshold));
        log.info("Dashboard: Subscribed new target [{}] at URL: {}", name, url);
    }

    public void unsubscribe(String url) {
        TargetConfig removed = monitoringTargets.remove(url);
        liveStatusMap.remove(url);
        if (removed != null) {
            log.info("Dashboard: Unsubscribed target [{}]", removed.name());
        }
    }

    public Map<String, LiveStatus> getAllStatuses() {
        return liveStatusMap;
    }

    @Scheduled(fixedRate = 10000)
    public void continuousMonitor() {
        if (monitoringTargets.isEmpty()) {
            log.debug("No targets to monitor. Waiting...");
            return;
        }
        log.info(" Starting scrape cycle for {} targets", monitoringTargets.size());
        monitoringTargets.values().forEach(this::process);
    }

    private void process(TargetConfig config) {
        try {
            log.info("Scraping metrics for: {}", config.name());
            String raw = prometheusClient.fetch(config.url());

            if (raw == null || raw.isBlank()) {
                throw new RuntimeException("Empty response from Prometheus");
            }

            List<MetricPoint> metrics = parserService.parse(raw);

            updateLiveStatus(config, metrics);

            AlertRequest alert = ruleService.evaluate(metrics, config.name(), config.threshold());
            if (alert != null) {
                log.warn("⚠ ALERT TRIGGERED for [{}]: {}", config.name(), alert.getMessage());
                restTemplate.postForObject(ALERT_INGEST_URL, alert, String.class);
            } else {
                boolean isCurrentlyFailing = ruleService.getActiveAlerts().keySet().stream()
                        .anyMatch(key -> key.startsWith(config.name()));

                if (isCurrentlyFailing) {
                    log.info("[{}] is STILL FAILING (Alert suppressed/stateful)", config.name());
                } else {
                    log.info("{} is within healthy limits.", config.name());
                }
            }

        } catch (Exception e) {
            log.error(" FAILED to scrape [{}]: {}", config.name(), e.getMessage());
            liveStatusMap.put(config.url(), LiveStatus.builder()
                    .name(config.name())
                    .up(false)
                    .latency(0.0)
                    .dbUsagePercent(0.0)
                    .lastUpdate(java.time.LocalTime.now().toString())
                    .build());
        }
    }

    private void updateLiveStatus(TargetConfig config, List<MetricPoint> metrics) {
        double sum = 0, count = 0, active = 0, max = 0;
        for (MetricPoint m : metrics) {
            switch (m.getName()) {
                case "http_server_requests_seconds_sum" -> sum = m.getValue();
                case "http_server_requests_seconds_count" -> count = m.getValue();
                case "hikaricp_connections_active" -> active = m.getValue();
                case "hikaricp_connections_max" -> max = m.getValue();
            }
        }

        LiveStatus status = LiveStatus.builder()
                .name(config.name())
                .up(true)
                .latency(count > 0 ? sum / count : 0)
                .dbUsagePercent(max > 0 ? (active / max) * 100 : 0)
                .lastUpdate(java.time.LocalTime.now().toString())
                .build();

        liveStatusMap.put(config.url(), status);
        log.info("Status Updated for [{}]: Latency={}s, DB={} %",
                config.name(), String.format("%.4f", status.getLatency()), status.getDbUsagePercent());
    }
}