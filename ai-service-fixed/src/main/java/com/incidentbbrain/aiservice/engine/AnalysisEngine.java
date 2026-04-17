package com.incidentbbrain.aiservice.engine;

import com.incidentbbrain.aiservice.dto.IncidentAnalysis;
import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisEngine {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AnalysisEngine(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public IncidentAnalysis analyze(String service, List<String> logs, MetricsSnapshot metrics) {
        log.info("Starting AI analysis for service: {}", service);

        // logContext is effectively final because it's assigned once here
        String logContext = (logs != null && !logs.isEmpty()) ? String.join("\n", logs) : "No logs provided";

        // Step 1: Fetch historical context
        // By using a helper method, this result is assigned once, making it effectively final
        String historicalContext = fetchHistoricalContext(logContext);

        // Step 2: Prompting Gemini via ChatClient
        try {
            return chatClient.prompt()
                    .user(u -> u.text("""
                        System: IncidentBrain AI Forensic Analyst
                        Task: Analyze the incident and provide root cause.
                        
                        Service: {service}
                        
                        Metrics Vital Signs:
                        - CPU Usage: {cpu}%
                        - JVM Memory: {mem}MB
                        - HTTP Requests: {http}
                        - Health Status: {health}
                        
                        Current Logs:
                        {logs}
                        
                        Relevant Past Resolutions (RAG):
                        {history}
                        
                        Requirement: 
                        1. Diagnose the Root Cause.
                        2. Provide specific remediation steps.
                        3. Return response in valid JSON format mapping to IncidentAnalysis.
                        """)
                            .param("service", service)
                            .param("cpu", metrics.getSystemCpuUsage())
                            .param("mem", metrics.getJvmMemoryUsed())
                            .param("http", metrics.getHttpRequests())
                            .param("health", metrics.getHealthStatus())
                            .param("logs", logContext)
                            .param("history", historicalContext))
                    .call()
                    .entity(IncidentAnalysis.class);
        } catch (Exception e) {
            log.error("Gemini analysis failed: {}", e.getMessage());
            throw new RuntimeException("AI Analysis failed to generate report", e);
        }
    }

    /**
     * Helper method to handle Vector Store search.
     * This isolates the try-catch block so the main method's variables remain effectively final.
     */
    private String fetchHistoricalContext(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(3)
                    .build();

            List<Document> history = vectorStore.similaritySearch(searchRequest);

            if (history == null || history.isEmpty()) {
                return "No historical matches found.";
            }

            return history.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("Failed to retrieve historical context from vector store: {}", e.getMessage());
            return "No prior context available due to system error.";
        }
    }
}