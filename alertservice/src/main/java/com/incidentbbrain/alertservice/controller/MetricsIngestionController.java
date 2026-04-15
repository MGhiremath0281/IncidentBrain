package com.incidentbbrain.alertservice.controller;

import com.incidentbbrain.alertservice.service.MetricsIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class MetricsIngestionController {

    private final MetricsIngestionService ingestionService;

    @PostMapping("/prometheus")
    public ResponseEntity<String> ingest(@RequestParam String url) {

        ingestionService.process(url);

        return ResponseEntity.ok("Ingestion started for: " + url);
    }
}