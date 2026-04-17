package com.incidentbbrain.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LearningService {

    private final VectorStore vectorStore;

    @KafkaListener(topics = "incident.resolved", groupId = "ai-learning-group")
    public void learnFromResolution(Map<String, String> resolutionData) {
        try {
            String logs = resolutionData.get("logs");
            String resolution = resolutionData.get("resolution");

            if (logs != null && resolution != null) {
                String knowledgeContent = String.format(
                        "Problem Pattern: %s \nVerified Resolution: %s",
                        logs, resolution
                );

                // Use the constructor or builder to ensure text is set for pgvector
                Document doc = new Document(knowledgeContent);

                // Step 6: Save to pgvector knowledge base
                // This triggers the embedding model to turn text into a vector
                vectorStore.accept(List.of(doc));

                log.info("AI Service successfully learned from a new resolution pattern.");
            }
        } catch (Exception e) {
            log.error("Failed to ingest learning data into Vector DB: {}", e.getMessage());
        }
    }
}