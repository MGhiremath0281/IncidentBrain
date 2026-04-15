package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.MetricPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrometheusParserService {

    public List<MetricPoint> parse(String raw) {

        List<MetricPoint> list = new ArrayList<>();

        if (raw == null) return list;

        String[] lines = raw.split("\n");

        for (String line : lines) {

            if (line.startsWith("#") || line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(" ");
                if (parts.length < 2) continue;

                String metric = parts[0];
                double value = Double.parseDouble(parts[1]);

                String name = metric.contains("{")
                        ? metric.substring(0, metric.indexOf("{"))
                        : metric;

                String labels = metric.contains("{")
                        ? metric.substring(metric.indexOf("{") + 1, metric.indexOf("}"))
                        : "";

                list.add(new MetricPoint(name, value, labels));

            } catch (Exception ignored) {}
        }

        return list;
    }
}