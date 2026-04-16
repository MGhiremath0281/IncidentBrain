package com.incidentbbrain.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricPoint {
    private String name;
    private double value;
    private String labels;
}