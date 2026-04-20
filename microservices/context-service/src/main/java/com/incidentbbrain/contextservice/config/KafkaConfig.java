package com.incidentbbrain.contextservice.config;

import com.incidentbbrain.contextservice.dto.ContextPayload;
import com.incidentbbrain.incidentbraincommon.common.IncidentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // =========================
    // CONSUMER (IncidentEvent)
    // =========================
    @Bean
    public ConsumerFactory<String, IncidentEvent> consumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // BUG FIX: Added fetch tuning — without these, the consumer polls very
        // conservatively and waits up to 500ms between fetches even when messages
        // are available, causing slow/laggy processing under load.
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<IncidentEvent> deserializer =
                new JsonDeserializer<>(IncidentEvent.class);

        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("com.incidentbbrain.incidentbraincommon.common");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IncidentEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, IncidentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // BUG FIX: concurrency was 1 (default) — single-threaded processing means
        // every incident queues up behind the previous one. Each enrichment does
        // 4 sequential HTTP calls (ES + 3 actuator) so a single slow target service
        // stalls the entire pipeline. Set to 3 to match typical partition count.
        factory.setConcurrency(3);

        // BUG FIX: BATCH ack mode lets the consumer commit offsets after processing
        // a batch rather than after each single record, reducing broker round-trips.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        return factory;
    }

    // =========================
    // PRODUCER (ContextPayload)
    // =========================
    @Bean
    public ProducerFactory<String, ContextPayload> producerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, ContextPayload> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // BUG FIX: Plain new RestTemplate() has NO timeouts — if Elasticsearch or any
    // Actuator endpoint hangs, the thread blocks forever. This stalls the Kafka
    // listener thread, causing the consumer to stop polling and lag to build up.
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);  // 3s to establish connection
        factory.setReadTimeout(5_000);     // 5s to wait for response body
        return new RestTemplate(factory);
    }
}