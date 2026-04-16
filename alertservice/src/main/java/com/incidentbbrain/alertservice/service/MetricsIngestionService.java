package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.client.PrometheusClient;
import com.incidentbbrain.alertservice.dto.AlertRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsIngestionService {

    private final PrometheusClient prometheusClient;
    private final PrometheusParserService parserService;
    private final RuleEvaluationService ruleService;
    private final AlertService alertService;
    private final RestTemplate restTemplate;

    // Thread-safe set to store URLs received from Postman
    private final Set<String> monitoringTargets = ConcurrentHashMap.newKeySet();

    private static final String ALERT_INGEST_URL = "http://localhost:8081/alerts/ingest";

    /**
     * Adds a URL to the monitoring list (called by Controller)
     */
    public void subscribe(String url) {
        monitoringTargets.add(url);
        log.info("URL added to monitor list. Total targets: {}", monitoringTargets.size());
    }

    /**
     * Continuous Loop: Runs every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void continuousMonitor() {
        if (monitoringTargets.isEmpty()) {
            return;
        }

        log.debug("Checking metrics for {} registered targets...", monitoringTargets.size());
        monitoringTargets.forEach(this::process);
    }

    public void process(String prometheusUrl) {
        try {
            log.info("Fetching metrics from {}", prometheusUrl);

            // 1. Fetch
            String raw = prometheusClient.fetch(prometheusUrl);
            if (raw == null || raw.isBlank()) return;

            // 2. Parse
            List<?> metrics = parserService.parse(raw);

            // 3. Evaluate
            AlertRequest alert = ruleService.evaluate((List) metrics);

            if (alert == null) {
                log.info("No threshold breached for {}", prometheusUrl);
                return;
            }

            log.warn("ALERT TRIGGERED: {}", alert.getMessage());

            // 4. Store + Kafka
            alertService.ingest(alert);

            // 5. Push to external Alert API
            restTemplate.postForObject(ALERT_INGEST_URL, alert, String.class);
            log.info("Alert successfully dispatched to central API");

        } catch (Exception e) {
            log.error("Error processing metrics for {}: {}", prometheusUrl, e.getMessage());
        }
    }
}