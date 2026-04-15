package com.incidentbbrain.contextservice.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchLogService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ELASTICSEARCH_URL =
            "http://elasticsearch:9200/incidentbrain-logs-*/_search";

    public List<String> getLogsForService(String service, UUID incidentId, LocalDateTime since) {

        try {
            String query = buildQuery(service, since);

            String response = restTemplate.postForObject(
                    ELASTICSEARCH_URL,
                    query,
                    String.class
            );

            return parseLogs(response, service);

        } catch (Exception e) {
            log.error("Failed to fetch logs from Elasticsearch for service={}", service, e);
            return List.of("ERROR: Unable to fetch logs from Elasticsearch");
        }
    }

    private String buildQuery(String service, LocalDateTime since) {

        long epochMillis = since.toInstant(ZoneOffset.UTC).toEpochMilli();

        return """
        {
          "size": 50,
          "sort": [
            { "@timestamp": "desc" }
          ],
          "query": {
            "bool": {
              "must": [
                { "term": { "service": "%s" } },
                { "range": { "@timestamp": { "gte": %d } } }
              ]
            }
          }
        }
        """.formatted(service, epochMillis);
    }

    private List<String> parseLogs(String response, String service) throws Exception {

        List<String> logs = new ArrayList<>();

        JsonNode root = objectMapper.readTree(response);
        JsonNode hits = root.path("hits").path("hits");

        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");

            String timestamp = source.path("@timestamp").asText();
            String level = source.path("level").asText("INFO");
            String message = source.path("message").asText("");

            logs.add(String.format("[%s] %s %s - %s",
                    service.toUpperCase(),
                    timestamp,
                    level,
                    message));
        }

        return logs;
    }
}