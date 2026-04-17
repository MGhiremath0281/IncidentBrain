package com.incidentbbrain.alertservice.model;

import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    private String message;

    private String host;

    private String alertType;   // INFRA | APPLICATION
    private String source;      // PROMETHEUS | LOGS | ACTUATOR
    private String reason;      // SERVICE_DOWN | HIGH_LATENCY | ERROR_RATE

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private Instant createdAt;

    private UUID incidentId;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = AlertStatus.OPEN;
        }
    }
}