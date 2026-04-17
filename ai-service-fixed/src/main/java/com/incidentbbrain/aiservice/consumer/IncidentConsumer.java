package com.incidentbbrain.aiservice.consumer;

import com.incidentbbrain.aiservice.dto.EnrichedIncidentDTO;
import com.incidentbbrain.aiservice.dto.IncidentAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.incidentbbrain.aiservice.engine.AnalysisEngine;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentConsumer {

    private final AnalysisEngine engine;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "context.ready", groupId = "ai-service-group")
    public void onIncidentReceived(EnrichedIncidentDTO dto) {
        String lockKey = "ai_process_lock:" + dto.getIncidentId();

        // Step 2: Distributed Lock (Redis)
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            log.info("Incident {} is already being analyzed. Skipping.", dto.getIncidentId());
            return;
        }

        try {
            log.info("Analyzing incident {} for service {}", dto.getIncidentId(), dto.getService());

            IncidentAnalysis result = engine.analyze(dto.getService(), dto.getLogs(), dto.getMetrics());

            // Step 5: Distribute
            kafkaTemplate.send("analysis.completed", dto.getIncidentId().toString(), result);

        } catch (Exception e) {
            log.error("AI processing failed for {}: {}", dto.getIncidentId(), e.getMessage());
            redisTemplate.delete(lockKey); // Allow retry
        }
    }
}