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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedIncident {

    @Id
    @Column(name = "incident_id", updatable = false, nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<UUID> alertIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> logs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private MetricsSnapshot metrics;

    @Column(name = "incident_started_at")
    private LocalDateTime incidentStartedAt;

    @Column(name = "enriched_at")
    private LocalDateTime enrichedAt;
}