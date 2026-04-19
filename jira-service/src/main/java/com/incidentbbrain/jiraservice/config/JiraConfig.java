package com.incidentbbrain.jiraservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Configuration
public class JiraConfig {

    @Value("${jira.base-url}")
    private String baseUrl;

    @Value("${jira.user-email}")
    private String email;

    @Value("${jira.api-token}")
    private String token;

    @Bean
    public RestClient jiraRestClient() {
        String auth = email + ":" + token;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        // Added a request factory for timeout management
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}