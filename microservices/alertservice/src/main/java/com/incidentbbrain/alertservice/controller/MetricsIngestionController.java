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
public class MetricsIngestionController {

    private final MetricsIngestionService ingestionService;
    private final RuleEvaluationService ruleService;

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam String url,
                                            @RequestParam String name,
                                            @RequestParam Double threshold,
                                            @RequestHeader("X-Username") String username) {

        log.info("User '{}' subscribed URL: {} with name: {}", username, url, name);
        ingestionService.subscribe(url, name, threshold);
        return ResponseEntity.ok(username + " is now monitoring " + name);
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String url,
                                              @RequestHeader("X-Username") String username) {

        log.info("User '{}' unsubscribed URL: {}", username, url);
        ingestionService.unsubscribe(url);
        return ResponseEntity.ok(username + " removed " + url);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, MetricsIngestionService.LiveStatus>> getStatus() {
        return ResponseEntity.ok(ingestionService.getAllStatuses());
    }

    @GetMapping("/alerts/active")
    public ResponseEntity<Map<String, Instant>> getActiveAlerts() {
        return ResponseEntity.ok(ruleService.getActiveAlerts());
    }
}