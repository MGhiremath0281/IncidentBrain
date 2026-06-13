git add infrastructure/auth-service/src/main/java/com/incidentbbrain/authservice/config/SecurityConfig.java

        git add microservices/alertservice/src/main/java/com/incidentbbrain/alertservice/config/SecurityConfig.java
        git add microservices/alertservice/src/main/java/com/incidentbbrain/alertservice/config/GatewayAuthFilter.java

        git add microservices/context-service/src/main/java/com/incidentbbrain/contextservice/config/SecurityConfig.java
        git add microservices/context-service/src/main/java/com/incidentbbrain/contextservice/config/GatewayAuthFilter.java

        git add microservices/jira-service/src/main/java/com/incidentbbrain/jiraservice/config/SecurityConfig.java
        git add microservices/jira-service/src/main/java/com/incidentbbrain/jiraservice/config/GatewayAuthFilter.java

        git commit -m "fix: expose actuator endpoints for Prometheus scraping"package com.incidentbbrain.contextservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/actuator/prometheus",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new GatewayAuthFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}