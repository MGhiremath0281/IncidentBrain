package com.incidentbbrain.aiservice.engine;

import com.incidentbbrain.aiservice.entity.IncidentAnalysis;
import com.incidentbbrain.incidentbraincommon.common.MetricsSnapshot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AnalysisEngine {

    private final ChatClient chatClient;

    @Value("${spring.ai.google.genai.api-key:NOT_FOUND}")
    private String apiKey;

    public AnalysisEngine(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostConstruct
    public void startupCheck() {
        log.info("==================================================");
        log.info("AI SERVICE STARTUP CHECK");
        log.info("Gemini API Key Present: {}", !"NOT_FOUND".equals(apiKey));
        log.info("Gemini API Key Length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("Gemini API Key Value: {}", apiKey);
        log.info("==================================================");
    }

    public IncidentAnalysis analyze(String service,
                                    List<String> logs,
                                    MetricsSnapshot metrics) {

        log.info("==================================================");
        log.info("Starting AI forensic analysis for service: {}", service);
        log.info("==================================================");

        log.info("Service: {}", service);
        log.info("Metrics Null: {}", metrics == null);
        log.info("Logs Count: {}", logs == null ? 0 : logs.size());

        // By using inline ternary conditions, these variables are assigned
        // exactly once and are now safely "effectively final" for the lambda.
        final String cpu = (metrics != null && metrics.getSystemCpuUsage() != null)
                ? String.valueOf(metrics.getSystemCpuUsage())
                : "UNKNOWN";

        final String mem = (metrics != null && metrics.getJvmMemoryUsed() != null)
                ? String.valueOf(metrics.getJvmMemoryUsed())
                : "UNKNOWN";

        final String http = (metrics != null && metrics.getHttpRequests() != null)
                ? String.valueOf(metrics.getHttpRequests())
                : "UNKNOWN";

        final String health = (metrics != null && metrics.getHealthStatus() != null)
                ? String.valueOf(metrics.getHealthStatus())
                : "UNKNOWN";

        if (metrics != null) {
            log.info("CPU Usage: {}", cpu);
            log.info("JVM Memory Used: {}", mem);
            log.info("HTTP Requests: {}", http);
            log.info("Health Status: {}", health);
        }

        String logContext = (logs != null && !logs.isEmpty())
                ? String.join("\n", logs)
                : "No logs available.";

        try {

            log.info("Sending prompt to Gemini...");

            IncidentAnalysis result = chatClient.prompt()
                    .user(u -> u.text("""
                            Role: Senior SRE & Forensic Analyst

                            Task:
                            Analyze the incident for {service} and return a structured JSON report.

                            Metrics:
                            CPU Usage: {cpu}
                            JVM Memory Used: {mem}
                            HTTP Requests: {http}
                            Health Status: {health}

                            Logs:
                            {logs}

                            Requirements:
                            1. Identify the most likely root cause.
                            2. Give a confidence score from 0 to 100.
                            3. Suggest remediation actions.
                            4. Provide a short executive summary.
                            5. Return ONLY valid JSON matching IncidentAnalysis.
                            """)
                            .param("service",
                                    service != null ? service : "UNKNOWN")

                            .param("cpu", cpu)
                            .param("mem", mem)
                            .param("http", http)
                            .param("health", health)
                            .param("logs", logContext))
                    .call()
                    .entity(IncidentAnalysis.class);

            log.info("Gemini analysis completed successfully.");

            return result;

        } catch (Exception e) {

            log.error("==================================================");
            log.error("Gemini API call failed!", e);
            log.error("Exception Type: {}", e.getClass().getName());
            log.error("Exception Message: {}", e.getMessage());
            log.error("==================================================");

            return IncidentAnalysis.builder()
                    .rootCause("Analysis paused: " + e.getMessage())
                    .confidenceScore(0.0)
                    .suggestedActions(List.of(
                            "Check Gemini API configuration",
                            "Verify metrics collection",
                            "Inspect AI service logs",
                            "Perform manual incident investigation"
                    ))
                    .summary("AI analysis failed and fallback response was generated.")
                    .suspectedComponent(
                            service != null ? service : "UNKNOWN")
                    .build();
        }
    }
}