package com.incidentbbrain.alertservice.client;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.service.AlertService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsClient {

    private final PrometheusClient prometheusClient;
    private final AlertService alertService;

    @CircuitBreaker(name = "prometheusCB", fallbackMethod = "fallbackFetch")
    public String fetch(String url) {
        String response = prometheusClient.fetch(url);

        if (response == null || response.isBlank()) {
            throw new RuntimeException("Empty Prometheus response");
        }

        return response;
    }

    public String fallbackFetch(String url, Throwable ex) {
        log.error("CircuitBreaker OPEN for Prometheus [{}]: {}", url, ex.getMessage());
        return "CB_OPEN";
    }

    /**
     * Persists a triggered alert directly via AlertService.
     * Previously this called POST /alerts/ingest on localhost:8081 — which is
     * this same service — causing a self-loop. Injecting AlertService directly
     * is correct and avoids an unnecessary HTTP round-trip.
     */
    @CircuitBreaker(name = "alertPushCB", fallbackMethod = "fallbackAlertPush")
    public void push(AlertRequest alert) {
        alertService.ingest(alert);
    }

    public void fallbackAlertPush(AlertRequest alert, Throwable ex) {
        log.error("CircuitBreaker OPEN for Alert Push [{}]: {}", alert.getMessage(), ex.getMessage());
    }
}