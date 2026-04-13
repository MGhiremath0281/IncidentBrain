package com.incidentbbrain.correlationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue
    private UUID id;

    private String affectedService;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String status = "OPEN";

    @ElementCollection
    private List<UUID> alertIds;

    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    private String rootCause;
    private String title;
}
