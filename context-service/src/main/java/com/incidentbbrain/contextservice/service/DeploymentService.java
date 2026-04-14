package com.incidentbbrain.contextservice.service;


import com.incidentbbrain.contextservice.dto.DeploymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of deployment information service.
 * In production, this would query a deployment database or CI/CD system.
 */
@Slf4j
@Service
public class DeploymentService {

    private static final String[] ENVIRONMENTS = {"production", "staging", "production-eu", "production-us"};
    private static final String[] DEPLOYERS = {"ci-pipeline", "deploy-bot", "release-manager"};

    // Cache to ensure consistent deployment info for the same service within a session
    private final Map<String, DeploymentInfo> deploymentCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Retrieves deployment information for the specified service.
     *
     * @param service the service name
     * @return deployment information including version, timestamp, and metadata
     */
    public DeploymentInfo getDeploymentInfo(String service) {
        log.debug("Fetching deployment info for service={}", service);

        return deploymentCache.computeIfAbsent(service, this::generateDeploymentInfo);
    }

    private DeploymentInfo generateDeploymentInfo(String service) {
        // Generate realistic version (semantic versioning)
        int major = 2 + random.nextInt(3);
        int minor = random.nextInt(15);
        int patch = random.nextInt(50);
        String version = String.format("%d.%d.%d", major, minor, patch);

        // Deployment happened sometime in the last 7 days
        LocalDateTime deployedAt = LocalDateTime.now()
                .minusDays(random.nextInt(7))
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));

        // Generate a realistic commit hash (short form)
        String commitHash = UUID.randomUUID().toString().substring(0, 7);

        DeploymentInfo info = DeploymentInfo.builder()
                .version(version)
                .deployedAt(deployedAt)
                .environment(ENVIRONMENTS[random.nextInt(ENVIRONMENTS.length)])
                .deployedBy(DEPLOYERS[random.nextInt(DEPLOYERS.length)])
                .commitHash(commitHash)
                .artifactId(service + "-" + version + ".jar")
                .build();

        log.debug("Generated deployment info for service={}: version={}, deployedAt={}",
                service, info.getVersion(), info.getDeployedAt());

        return info;
    }

    /**
     * Clears the deployment cache. Useful for testing.
     */
    public void clearCache() {
        deploymentCache.clear();
    }
}
