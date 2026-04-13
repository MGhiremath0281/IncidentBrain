package com.incidentbbrain.correlationservice.kafka.event;

import com.incidentbbrain.correlationservice.entity.Severity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AlertEvent {
    private UUID id;
    private String serviceName;
    private Severity severity;
    private LocalDateTime timestamp;
}
