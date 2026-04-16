package com.incidentbbrain.contextservice.entity;

import com.incidentbbrain.contextservice.dto.MetricsSnapshot;
import com.incidentbbrain.incidentbraincommon.common.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "enriched_incidents")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class EnrichedIncident {
    @Id
    private UUID incidentId;
    private String service;

    @Enumerated(EnumType.STRING)
    private Severity severity;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> alertIds;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> logs;

    @JdbcTypeCode(SqlTypes.JSON)
    private MetricsSnapshot metrics;

    private LocalDateTime incidentStartedAt;
    private LocalDateTime enrichedAt;
}