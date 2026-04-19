package com.incidentbbrain.correlationservice.service;

import com.incidentbbrain.correlationservice.entity.Incident;
import com.incidentbbrain.correlationservice.repository.IncidentRepository;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository repository;

    @InjectMocks
    private IncidentService incidentService;

    private AlertEvent createAlert(String id, String severity) {
        AlertEvent alert = mock(AlertEvent.class);
        when(alert.getAlertId()).thenReturn(id);
        when(alert.getSeverity()).thenReturn(severity);
        return alert;
    }

    @Test
    void shouldCreateIncidentWithMaxSeverity() {

        AlertEvent a1 = createAlert(UUID.randomUUID().toString(), "LOW");
        AlertEvent a2 = createAlert(UUID.randomUUID().toString(), "CRITICAL");

        Incident savedIncident = Incident.builder()
                .id(UUID.randomUUID())
                .affectedService("orders")
                .severity(Severity.CRITICAL)
                .build();

        when(repository.save(any(Incident.class))).thenReturn(savedIncident);

        Incident result = incidentService.createIncident("orders", List.of(a1, a2));

        assertNotNull(result);
        assertEquals(Severity.CRITICAL, result.getSeverity());

        verify(repository, times(1)).save(any(Incident.class));
    }

    @Test
    void shouldResolveIncidentSuccessfully() {

        UUID id = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(id)
                .status("OPEN")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(incident));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Incident result = incidentService.resolveIncident(id, null);

        assertEquals("RESOLVED", result.getStatus());
        assertNotNull(result.getResolvedAt());

        verify(repository).save(any(Incident.class));
    }

    @Test
    void shouldThrowExceptionWhenIncidentNotFound() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> incidentService.getIncident(id));

        assertEquals("Incident not found", ex.getMessage());
    }

    @Test
    void shouldReturnIncidentById() {

        UUID id = UUID.randomUUID();

        Incident incident = Incident.builder()
                .id(id)
                .status("OPEN")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(incident));

        Incident result = incidentService.getIncident(id);

        assertEquals(id, result.getId());
    }
}