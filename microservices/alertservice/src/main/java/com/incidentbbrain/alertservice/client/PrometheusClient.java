package com.incidentbbrain.alertservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class PrometheusClient {

    private final RestTemplate restTemplate;

    public String fetch(String url) {
        return restTemplate.getForObject(url, String.class);
    }
}