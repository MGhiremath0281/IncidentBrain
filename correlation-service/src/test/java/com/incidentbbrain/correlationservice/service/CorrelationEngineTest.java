package com.incidentbbrain.correlationservice.service;

import com.incidentbbrain.correlationservice.kafka.producer.IncidentProducer;
import com.incidentbbrain.correlationservice.kafka.event.IncidentEvent;
import com.incidentbbrain.incidentbraincommon.common.AlertEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.mockito.Mockito.*;

class CorrelationEngineTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private IncidentProducer producer;

    @InjectMocks
    private CorrelationEngine correlationEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private AlertEvent createAlert(String service, String message) {
        AlertEvent alert = mock(AlertEvent.class);
        when(alert.getServiceName()).thenReturn(service);
        when(alert.getMessage()).thenReturn(message);
        when(alert.getTimestamp()).thenReturn(java.time.LocalDateTime.now().toString());
        return alert;
    }

    @Test
    void shouldGroupAlertsAndCreateIncidentWhenThresholdReached() {

        AlertEvent a1 = createAlert("orders", "DB timeout");
        AlertEvent a2 = createAlert("orders", "Connection pool exhausted");

        when(incidentService.createIncident(eq("orders"), anyList()))
                .thenReturn(mock(com.incidentbbrain.correlationservice.entity.Incident.class));

        correlationEngine.process(a1);
        correlationEngine.process(a2);

        verify(incidentService, times(1))
                .createIncident(eq("orders"), anyList());

        verify(producer, times(1))
                .publish(any(IncidentEvent.class));
    }

    @Test
    void shouldNotMixDifferentServices() {

        AlertEvent a1 = createAlert("orders", "DB error");
        AlertEvent a2 = createAlert("payment", "Gateway error");

        correlationEngine.process(a1);
        correlationEngine.process(a2);

        verify(incidentService, never()).createIncident(any(), anyList());
        verify(producer, never()).publish(any());
    }

    @Test
    void shouldFlushPerServiceIndependently() {

        AlertEvent o1 = createAlert("orders", "DB error");
        AlertEvent o2 = createAlert("orders", "Timeout");

        AlertEvent p1 = createAlert("payment", "Gateway error");
        AlertEvent p2 = createAlert("payment", "Auth failed");

        when(incidentService.createIncident(any(), anyList()))
                .thenAnswer(inv -> mock(com.incidentbbrain.correlationservice.entity.Incident.class));

        correlationEngine.process(o1);
        correlationEngine.process(o2);

        correlationEngine.process(p1);
        correlationEngine.process(p2);

        verify(incidentService, times(2)).createIncident(any(), anyList());
        verify(producer, times(2)).publish(any());
    }
}