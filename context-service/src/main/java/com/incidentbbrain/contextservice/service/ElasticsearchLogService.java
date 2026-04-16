package com.incidentbbrain.contextservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchLogService {

    private final DynamicEndpointRegistry registry;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<String> getLogs(String service, LocalDateTime startedAt) {
        LocalDateTime time = (startedAt != null) ? startedAt : LocalDateTime.now();

        // Looking +/- 2 minutes around the incident
        String gte = time.minusMinutes(2).toInstant(ZoneOffset.UTC).toString();
        String lte = time.plusMinutes(2).toInstant(ZoneOffset.UTC).toString();

        String query = """
        {
          "query": {
            "bool": {
              "must": [{ "match": { "service": "%s" } }],
              "filter": [{ "range": { "@timestamp": { "gte": "%s", "lte": "%s" } } }]
            }
          },
          "sort": [{ "@timestamp": "desc" }],
          "size": 50
        }
        """.formatted(service, gte, lte);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(query, headers);

            String rawResponse = restTemplate.postForObject(
                    registry.getElasticsearchUrl(),
                    entity,
                    String.class
            );

            return parseMeaningfulLogs(rawResponse);
        } catch (Exception e) {
            log.error("Elasticsearch query failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> parseMeaningfulLogs(String jsonResponse) {
        List<String> logs = new ArrayList<>();
        try {
            JsonNode hits = objectMapper.readTree(jsonResponse).path("hits").path("hits");
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                String level = source.path("level").asText("INFO");
                String message = source.path("message").asText("");

                if (isCritical(level, message)) {
                    String formattedLog = String.format("[%s] %s - %s",
                            source.path("@timestamp").asText("N/A"),
                            level.toUpperCase(),
                            message
                    );

                    if (!logs.contains(formattedLog)) {
                        logs.add(formattedLog);
                    }
                }

                // Keep it focused: 15 meaningful lines is plenty for the "Brain"
                if (logs.size() >= 15) break;
            }
        } catch (Exception e) {
            log.error("Error parsing ES JSON: {}", e.getMessage());
        }
        return logs;
    }

    private boolean isCritical(String level, String message) {
        if (message == null || message.isEmpty()) return false;

        String msg = message.toLowerCase();
        String lvl = level.toUpperCase();

        return lvl.equals("ERROR") ||
                lvl.equals("WARN") ||
                lvl.equals("FATAL") ||
                msg.contains("exception") ||
                msg.contains("cause") ||
                msg.contains("failed") ||
                msg.contains("timeout") ||
                msg.contains("denied") ||
                msg.contains("error"); // Catches errors buried in INFO messages
    }
}