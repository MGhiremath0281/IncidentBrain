package com.incidentbbrain.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    /**
     * We disable Spring Security's built-in auth here because
     * all JWT validation is handled by JwtAuthFilter (our GlobalFilter).
     * This avoids double-filtering and the 401 from Spring Security
     * conflicting with our own filter logic.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()  // JwtAuthFilter handles auth, not Spring Security
                );
        return http.build();
    }
}
