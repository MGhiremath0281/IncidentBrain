package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.MetricPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrometheusParserService {
    public List<MetricPoint> parse(String raw) {
        List<MetricPoint> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) continue;

                String fullMetric = parts[0];
                double value = Double.parseDouble(parts[1]);
                String name;
                String labels = "";

                if (fullMetric.contains("{")) {
                    name = fullMetric.substring(0, fullMetric.indexOf("{"));
                    labels = fullMetric.substring(fullMetric.indexOf("{") + 1, fullMetric.lastIndexOf("}"));
                } else {
                    name = fullMetric;
                }
                list.add(new MetricPoint(name, value, labels));
            } catch (Exception e) {
            }
        }
        return list;
    }
}