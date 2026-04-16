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
        if (startedAt == null) {
            log.warn("startedAt is null for service: {}. Using now.", service);
            startedAt = LocalDateTime.now();
        }

        String gte = startedAt.minusMinutes(5).toInstant(ZoneOffset.UTC).toString();
        String lte = startedAt.plusMinutes(1).toInstant(ZoneOffset.UTC).toString();

        String query = """
        {
          "query": {
            "bool": {
              "must": [{ "match": { "service": "%s" } }],
              "filter": [{ "range": { "@timestamp": { "gte": "%s", "lte": "%s" } } }]
            }
          },
          "size": 50
        }
        """.formatted(service, gte, lte);

        try {
            // FIX: Explicitly set Content-Type to application/json
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(query, headers);

            String rawResponse = restTemplate.postForObject(
                    registry.getElasticsearchUrl(),
                    entity, // Pass the entity with headers instead of just the string
                    String.class
            );

            return parseMessages(rawResponse);
        } catch (Exception e) {
            log.error("Failed to fetch logs from ES: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> parseMessages(String jsonResponse) {
        List<String> messages = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode hits = root.path("hits").path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    // Navigate to the message field based on your ES structure
                    JsonNode messageNode = hit.path("_source").path("message");
                    if (messageNode.isMissingNode()) {
                        // Fallback for some logstash structures
                        messageNode = hit.path("fields").path("message").get(0);
                    }
                    if (messageNode != null && !messageNode.isMissingNode()) {
                        messages.add(messageNode.asText());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing ES response: {}", e.getMessage());
        }
        return messages;
    }
}