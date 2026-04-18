package com.incidentbbrain.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LearningService {

    @Autowired(required = false)  // <-- THIS is the fix
    private VectorStore vectorStore;

    @KafkaListener(topics = "incident.resolved", groupId = "ai-learning-group")
    public void learnFromResolution(Map<String, String> resolutionData) {
        if (vectorStore == null) {
            log.warn("VectorStore not available, skipping learning ingestion.");
            return;
        }
        try {
            String logs = resolutionData.get("logs");
            String resolution = resolutionData.get("resolution");

            if (logs != null && resolution != null) {
                String knowledgeContent = String.format(
                        "Problem Pattern: %s \nVerified Resolution: %s",
                        logs, resolution
                );
                Document doc = new Document(knowledgeContent);
                vectorStore.accept(List.of(doc));
                log.info("AI Service successfully learned from a new resolution pattern.");
            }
        } catch (Exception e) {
            log.error("Failed to ingest learning data into Vector DB: {}", e.getMessage());
        }
    }
}