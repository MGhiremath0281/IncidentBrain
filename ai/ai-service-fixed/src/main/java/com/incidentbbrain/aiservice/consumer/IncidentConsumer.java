package com.incidentbbrain.aiservice.consumer;

import com.incidentbbrain.aiservice.dto.EnrichedIncidentDTO;
import com.incidentbbrain.aiservice.entity.IncidentAnalysis;
import com.incidentbbrain.aiservice.engine.AnalysisEngine;
import com.incidentbbrain.aiservice.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentConsumer {

    private final AnalysisEngine engine;
    private final AnalysisRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @KafkaListener(topics = "context.ready", groupId = "ai-service-group")
    public void onIncidentReceived(EnrichedIncidentDTO dto) {
        String lockKey = "ai_process_lock:" + dto.getIncidentId();

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            log.info("Incident {} is already being analyzed.", dto.getIncidentId());
            return;
        }

        try {
            log.info("Analyzing incident {} for service {}", dto.getIncidentId(), dto.getService());

            // 1. Get Analysis from Engine
            IncidentAnalysis result = engine.analyze(dto.getService(), dto.getLogs(), dto.getMetrics());

            // 2. FORCE NEW RECORD: Clear ID to ensure an INSERT happens, not an UPDATE
            result.setId(null);

            // 3. Save to Postgres
            IncidentAnalysis savedResult = repository.save(result);
            log.info("Analysis saved with UUID: {}", savedResult.getId());

            // 4. Send to Kafka
            kafkaTemplate.send("analysis.completed", dto.getIncidentId().toString(), savedResult);

        } catch (Exception e) {
            log.error("AI processing failed: {}", e.getMessage());
            redisTemplate.delete(lockKey);
        }
    }
}