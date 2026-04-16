package com.incidentbbrain.alertservice.controller;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.AlertResponse;
import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@Valid @RequestBody AlertRequest req) {

        AlertResponse res = service.ingest(req);

        return Map.of(
                "alertId", res.getId(),
                "status", "PUBLISHED"
        );
    }

    @GetMapping("/{id}")
    public AlertResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<AlertResponse> search(
            @RequestParam String serviceName,
            @RequestParam Severity severity,
            Pageable pageable
    ) {
        return service.search(serviceName, severity, pageable);
    }

    @PatchMapping("/{id}/status")
    public AlertResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        AlertStatus status = AlertStatus.valueOf(body.get("status"));
        return service.updateStatus(id, status);
    }

    @GetMapping("/all")
    public Page<AlertResponse> getAll(Pageable pageable) {
        return service.findAll(pageable);
    }
}