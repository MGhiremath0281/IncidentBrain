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

    public List<String> getLogs(String service, LocalDateTime startedAt, String reason) {
        LocalDateTime time = (startedAt != null) ? startedAt : LocalDateTime.now();

        // Expanding window slightly to 5 minutes to account for ingestion lag
        String gte = time.minusMinutes(5).toInstant(ZoneOffset.UTC).toString();
        String lte = time.plusMinutes(2).toInstant(ZoneOffset.UTC).toString();

        // Broader keywords to ensure we don't miss "DuplicateUserException" or other stack traces
        String keywordQuery = switch (reason) {
            case "DATABASE_EXHAUSTED" -> "hikari OR connection OR database OR sql OR pool OR exhausted";
            case "HIGH_LATENCY" -> "timeout OR slow OR latency OR duration OR delay OR exception OR error";
            case "SERVICE_DOWN" -> "error OR crash OR stop OR oom OR fatal OR " + service;
            default -> "error OR exception OR fail OR stacktrace";
        };

        // Added a "minimum_should_match" style logic via query_string
        String query = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "service": "%s" } }
              ],
              "should": [
                { "query_string": { "query": "%s" } }
              ],
              "filter": [
                { "range": { "@timestamp": { "gte": "%s", "lte": "%s" } } }
              ],
              "minimum_should_match": 0
            }
          },
          "sort": [{ "@timestamp": "desc" }],
          "size": 50
        }
        """.formatted(service, keywordQuery, gte, lte);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(query, headers);

            log.info("Querying ES for logs. Service: {}, Reason: {}, Keywords: {}", service, reason, keywordQuery);

            String rawResponse = restTemplate.postForObject(
                    registry.getElasticsearchUrl(),
                    entity,
                    String.class
            );

            return parseMeaningfulLogs(rawResponse);
        } catch (Exception e) {
            log.error("Elasticsearch query failed for reason {}: {}", reason, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> parseMeaningfulLogs(String jsonResponse) {
        List<String> logs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode hits = root.path("hits").path("hits");

            if (hits.isMissingNode() || hits.isEmpty()) {
                log.warn("No logs found in Elasticsearch for the given criteria.");
                return logs;
            }

            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                String level = source.path("level").asText("INFO");
                String message = source.path("message").asText("");
                String timestamp = source.path("@timestamp").asText("N/A");

                // We want to capture the log if it's an Error/Warn OR if it's the actual Exception message
                String formattedLog = String.format("[%s] %s - %s", timestamp, level.toUpperCase(), message);

                if (!logs.contains(formattedLog)) {
                    logs.add(formattedLog);
                }

                if (logs.size() >= 20) break; // Increased to 20 for better AI context
            }
        } catch (Exception e) {
            log.error("Error parsing ES JSON: {}", e.getMessage());
        }
        return logs;
    }
}