package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.client.PrometheusClient;
import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.MetricPoint;
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

    private final Set<String> monitoringTargets = ConcurrentHashMap.newKeySet();
    private static final String ALERT_INGEST_URL = "http://localhost:8081/alerts/ingest";

    public void subscribe(String url) {
        monitoringTargets.add(url);
        log.info("Subscribed: {}", url);
    }

    @Scheduled(fixedRate = 10000)
    public void continuousMonitor() {
        if (monitoringTargets.isEmpty()) return;
        monitoringTargets.forEach(this::process);
    }

    public void process(String prometheusUrl) {
        try {
            String raw = prometheusClient.fetch(prometheusUrl);
            if (raw == null || raw.isBlank()) return;

            List<MetricPoint> metrics = parserService.parse(raw);
            AlertRequest alert = ruleService.evaluate(metrics);

            if (alert != null) {
                log.warn("ALERT: [{}] {}", alert.getReason(), alert.getMessage());
                alertService.ingest(alert);
                restTemplate.postForObject(ALERT_INGEST_URL, alert, String.class);
            }
        } catch (Exception e) {
            log.error("Failed processing {}: {}", prometheusUrl, e.getMessage());
        }
    }
}