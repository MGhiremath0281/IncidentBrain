package com.incidentbbrain.correlationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentbbrain.correlationservice.dto.ResolveIncidentRequest;
import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.entity.Severity;
import com.incidentbbrain.correlationservice.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService service;

    @Test
    void shouldReturnIncidentById() throws Exception {

        UUID id = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(id)
                .affectedService("orders")
                .severity(Severity.CRITICAL)
                .status("OPEN")
                .startedAt(LocalDateTime.now())
                .build();

        when(service.getIncident(id)).thenReturn(incident);

        mockMvc.perform(get("/incidents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.service").value("orders"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(service, times(1)).getIncident(id);
    }

    @Test
    void shouldReturnAllIncidents() throws Exception {

        Incident i1 = Incident.builder()
                .id(UUID.randomUUID())
                .affectedService("orders")
                .severity(Severity.HIGH)
                .status("OPEN")
                .build();

        Incident i2 = Incident.builder()
                .id(UUID.randomUUID())
                .affectedService("payment")
                .severity(Severity.CRITICAL)
                .status("OPEN")
                .build();

        when(service.getAllIncidents()).thenReturn(List.of(i1, i2));

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).getAllIncidents();
    }

    @Test
    void shouldResolveIncident() throws Exception {

        UUID id = UUID.randomUUID();

        ResolveIncidentRequest request = new ResolveIncidentRequest();
        request.setResolvedAt(LocalDateTime.now());

        Incident resolved = Incident.builder()
                .id(id)
                .affectedService("orders")
                .status("RESOLVED")
                .resolvedAt(request.getResolvedAt())
                .build();

        when(service.resolveIncident(eq(id), any())).thenReturn(resolved);

        mockMvc.perform(patch("/incidents/" + id + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(service).resolveIncident(eq(id), any());
    }
}