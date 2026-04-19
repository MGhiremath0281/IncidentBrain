package com.incidentbbrain.aiservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incident_analysis")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    private double confidenceScore;

    @ElementCollection
    @CollectionTable(name = "analysis_actions", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "action")
    private List<String> suggestedActions;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String suspectedComponent;
}