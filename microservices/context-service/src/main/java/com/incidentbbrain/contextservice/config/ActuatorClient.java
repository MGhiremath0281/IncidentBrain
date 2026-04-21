package com.incidentbbrain.contextservice.config;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActuatorClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "actuatorCB", fallbackMethod = "fallbackJson")
    public JsonNode getJson(String url) {
        return restTemplate.getForObject(url, JsonNode.class);
    }

    public JsonNode fallbackJson(String url, Exception ex) {
        log.error("CircuitBreaker OPEN for Actuator [{}]: {}", url, ex.getMessage());
        return null;
    }
}