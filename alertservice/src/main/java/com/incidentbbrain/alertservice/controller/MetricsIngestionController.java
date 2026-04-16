package com.incidentbbrain.alertservice.controller;

import com.incidentbbrain.alertservice.service.MetricsIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class MetricsIngestionController {

    private final MetricsIngestionService ingestionService;

    @PostMapping("/prometheus")
    public ResponseEntity<String> ingest(@RequestParam String url) {
        log.info("Received request to monitor Prometheus URL: {}", url);

        // This now registers the URL for continuous background polling
        ingestionService.subscribe(url);

        return ResponseEntity.ok("Continuous monitoring started for: " + url);
    }
}