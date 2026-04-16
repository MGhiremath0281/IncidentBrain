package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.config.DynamicEndpointRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<String> getLogs(String service, LocalDateTime startedAt) {
        long gte = startedAt.minusMinutes(5).toInstant(ZoneOffset.UTC).toEpochMilli();
        long lte = startedAt.plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli();

        String query = """
        {
          "query": {
            "bool": {
              "must": [{ "match": { "service": "%s" } }],
              "filter": [{ "range": { "@timestamp": { "gte": %d, "lte": %d } } }]
            }
          },
          "size": 50
        }
        """.formatted(service, gte, lte);

        log.info("Fetching logs from ES for service: {} in time window [{} to {}]", service, gte, lte);

        try {
            return restTemplate.postForObject(registry.getElasticsearchUrl(), query, List.class);
        } catch (Exception e) {
            log.error("Failed to fetch logs from Elasticsearch: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list to keep the pipeline moving
        }
    }
}