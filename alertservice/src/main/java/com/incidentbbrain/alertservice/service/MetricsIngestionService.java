package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.client.PrometheusClient;
import com.incidentbbrain.alertservice.dto.AlertRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsIngestionService {

    private final PrometheusClient prometheusClient;
    private final PrometheusParserService parserService;
    private final RuleEvaluationService ruleService;
    private final AlertService alertService;
    private final RestTemplate restTemplate;

    private static final String ALERT_INGEST_URL =
            "http://localhost:8081/alerts/ingest";

    public void process(String prometheusUrl) {

        log.info("Fetching metrics from {}", prometheusUrl);

        // 1. Fetch
        String raw = prometheusClient.fetch(prometheusUrl);

        // 2. Parse
        List<?> metrics = parserService.parse(raw);

        // 3. Evaluate
        AlertRequest alert = ruleService.evaluate((List) metrics);

        if (alert == null) {
            log.info("No alert generated");
            return;
        }

        log.warn("Alert generated: {}", alert);

        // 4. Store + Kafka (your existing system)
        alertService.ingest(alert);

        // 5. Send to central ingest API
        restTemplate.postForObject(ALERT_INGEST_URL, alert, String.class);

        log.info("Alert sent to ingestion service");
    }
}