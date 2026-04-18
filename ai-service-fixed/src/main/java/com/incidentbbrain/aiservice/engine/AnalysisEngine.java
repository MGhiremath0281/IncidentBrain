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

        log.info("Starting AI forensic analysis for service: {}", service);

        String logContext = (logs != null && !logs.isEmpty())
                ? String.join("\n", logs)
                : "No logs available.";

        try {
            // SINGLE REQUEST CALL
            return chatClient.prompt()
                    .user(u -> u.text("""
                            Role: Senior SRE & Forensic Analyst
                            Task: Analyze the incident for {service} and return a structured JSON report.
                            
                            Metrics: CPU {cpu}%, JVM {mem} bytes, Requests {http}, Health {health}
                            Logs:
                            {logs}
                            
                            Requirement: Return ONLY a valid JSON mapping to IncidentAnalysis.
                            """)
                            .param("service", service)
                            .param("cpu", metrics.getSystemCpuUsage())
                            .param("mem", metrics.getJvmMemoryUsed())
                            .param("http", metrics.getHttpRequests())
                            .param("health", metrics.getHealthStatus())
                            .param("logs", logContext))
                    .call()
                    .entity(IncidentAnalysis.class);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());

            // Fallback object prevents the Kafka Consumer from crashing on 429 Quota errors
            return new IncidentAnalysis(
                    "Analysis paused: " + e.getMessage(),
                    0.0,
                    List.of("Check system logs manually", "Wait for API quota reset"),
                    "AI Quota Reached",
                    service
            );
        }
    }
}