package com.incidentbbrain.jiraservice.config;

import com.incidentbbrain.jiraservice.dto.AnalysisEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, AnalysisEvent> consumerFactory(KafkaProperties props) {
        JsonDeserializer<AnalysisEvent> jsonDeserializer = new JsonDeserializer<>(AnalysisEvent.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<AnalysisEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props.buildConsumerProperties(null),
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnalysisEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AnalysisEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, AnalysisEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}