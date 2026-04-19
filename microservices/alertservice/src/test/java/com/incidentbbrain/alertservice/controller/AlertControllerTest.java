package com.incidentbbrain.alertservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.AlertResponse;
import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @InjectMocks
    private AlertController controller;

    @Mock
    private AlertService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // Fix for serialization errors: Register JavaTime and Page support
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter) // This prevents the 500 error
                .build();
    }

    @Test
    void shouldIngestAlert_returnAlertIdAndStatus() throws Exception {
        UUID generatedId = UUID.randomUUID();
        AlertResponse response = AlertResponse.builder()
                .id(generatedId)
                .status(AlertStatus.RESOLVED)
                .build();

        when(service.ingest(any(AlertRequest.class))).thenReturn(response);

        mockMvc.perform(post("/alerts/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "serviceName": "auth",
                        "severity": "HIGH",
                        "message": "CPU spike",
                        "host": "localhost"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(generatedId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldSearchAlerts() throws Exception {
        AlertResponse response = AlertResponse.builder()
                .id(UUID.randomUUID())
                .build();
        when(service.search(eq("auth"), eq(Severity.HIGH), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/alerts")
                        .param("serviceName", "auth")
                        .param("severity", "HIGH")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        UUID id = UUID.randomUUID();
        AlertResponse response = AlertResponse.builder()
                .id(id)
                .status(AlertStatus.RESOLVED)
                .build();

        when(service.updateStatus(eq(id), eq(AlertStatus.RESOLVED)))
                .thenReturn(response);

        mockMvc.perform(patch("/alerts/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}