package com.incidentbbrain.alertservice.kafka;

import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.model.Alert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertKafkaProducerTest {

    @InjectMocks
    private AlertKafkaProducer producer;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldPublishEventToKafka_successfully() {

        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .serviceName("auth-service")
                .severity(Severity.HIGH)
                .message("CPU spike")
                .host("localhost")
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(null);

        producer.publish(alert);

        verify(kafkaTemplate, times(1))
                .send(
                        eq("alerts.raw"),
                        eq("auth-service"),
                        any()
                );
    }
}