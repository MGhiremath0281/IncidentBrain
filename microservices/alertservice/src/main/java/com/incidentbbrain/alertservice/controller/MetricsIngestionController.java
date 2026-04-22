package com.incidentbbrain.alertservice.controller;

import com.incidentbbrain.alertservice.service.MetricsIngestionService;
import com.incidentbbrain.alertservice.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricsIngestionController {

    private final MetricsIngestionService ingestionService;
    private final RuleEvaluationService ruleService;

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam String url,
                                            @RequestParam String name,
                                            @RequestParam Double threshold) {

        log.info("SUBSCRIBE API HIT");

        ingestionService.subscribe(url, name, threshold);

        return ResponseEntity.ok("Monitoring " + name);
    }

    // FEATURE 4: Remove Control
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String url) {
        ingestionService.unsubscribe(url);
        return ResponseEntity.ok("Removed " + url);
    }

    // FEATURE 1 & 2: Subscribed URLs + Live Gauges (Latency/DB)
    @GetMapping("/status")
    public ResponseEntity<Map<String, MetricsIngestionService.LiveStatus>> getStatus() {
        return ResponseEntity.ok(ingestionService.getAllStatuses());
    }

    // FEATURE 3: Active Alerts (What is currently broken)
    @GetMapping("/alerts/active")
    public ResponseEntity<Map<String, Instant>> getActiveAlerts() {
        return ResponseEntity.ok(ruleService.getActiveAlerts());
    }
}