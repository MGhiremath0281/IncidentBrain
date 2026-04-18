package com.incidentbbrain.aiservice.engine;

import com.incidentbbrain.aiservice.dto.IncidentAnalysis;
import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AnalysisEngine {

    private final ChatClient chatClient;

    public AnalysisEngine(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public IncidentAnalysis analyze(String service,
                                    List<String> logs,
                                    MetricsSnapshot metrics) {

        log.info("Starting AI analysis for service: {}", service);

        String logContext = (logs != null && !logs.isEmpty())
                ? String.join("\n", logs)
                : "No logs provided";

        // RAG DISABLED (no past data / no embeddings available)
        String historicalContext = "No historical data available yet.";

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
            log.error("Gemini analysis failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI Analysis failed to generate report", e);
        }
    }
}