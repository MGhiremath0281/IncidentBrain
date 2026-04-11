package com.incidentbbrain.alertservice.service;

import com.incidentbbrain.alertservice.dto.AlertRequest;
import com.incidentbbrain.alertservice.dto.AlertResponse;
import com.incidentbbrain.alertservice.enums.AlertStatus;
import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.kafka.AlertKafkaProducer;
import com.incidentbbrain.alertservice.model.Alert;
import com.incidentbbrain.alertservice.repository.AlertRepository;
import org.junit.jupiter.api.Test; // Required for JUnit 5
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @InjectMocks
    private AlertService service;

    @Mock
    private AlertRepository repo;

    @Mock
    private AlertKafkaProducer producer;

    @Test
    void shouldIngestAlert_saveToDb_andSendToKafka() {

        AlertRequest req = new AlertRequest();
        req.setServiceName("auth-service");
        req.setSeverity(Severity.HIGH);
        req.setMessage("CPU spike");
        req.setHost("Localhost");

        UUID mockId = UUID.randomUUID();
        Alert savedAlert = Alert.builder()
                .id(mockId)
                .serviceName(req.getServiceName())
                .severity(req.getSeverity())
                .message(req.getMessage())
                .host(req.getHost())
                .build();

        when(repo.save(any(Alert.class))).thenReturn(savedAlert);

        AlertResponse response = service.ingest(req);

        verify(repo, times(1)).save(any(Alert.class));

        verify(producer, times(1)).publish(any(Alert.class));

        assertNotNull(response);
        assertEquals(mockId, response.getId());
        assertEquals("auth-service", response.getServiceName());
        assertEquals(Severity.HIGH, response.getSeverity());
        assertEquals("CPU spike", response.getMessage());
    }

    @Test
    void shouldReturnAlert_whenIdExists(){

        UUID id = UUID.randomUUID();

        Alert alert = Alert.builder()
                .id(id)
                .serviceName("auth")
                .severity(Severity.HIGH)
                .message("ok")
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(alert));

        AlertResponse response = service.get(id);

        assertEquals(id,response.getId());
        assertEquals("auth",response.getServiceName());
    }

    @Test
    void shouldThrowException_whenAlertNotFound(){
        UUID id = UUID.randomUUID();

        when(repo.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.get(id));

        assertEquals("Alert not found", ex.getMessage());
    }

    @Test
    void shouldUpdateStatus_successfully_clean() {

        UUID id = UUID.randomUUID();

        Alert alert = Alert.builder()
                .id(id)
                .status(AlertStatus.OPEN)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(alert));

        when(repo.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setStatus(AlertStatus.RESOLVED);
            return saved;
        });

        AlertResponse response = service.updateStatus(id, AlertStatus.RESOLVED);

        assertEquals(AlertStatus.RESOLVED, response.getStatus());
    }

    @Test
    void shouldSearchAlerts() {

        Alert alert = Alert.builder()
                .serviceName("auth")
                .severity(Severity.HIGH)
                .build();

        Page<Alert> page = new PageImpl<>(List.of(alert));

        when(repo.findByServiceNameAndSeverity(
                eq("auth"),
                eq(Severity.HIGH),
                any(Pageable.class)
        )).thenReturn(page);

        Page<AlertResponse> result = service.search("auth", Severity.HIGH, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
    }

}