package com.incidentbbrain.contextservice.service;

import com.incidentbbrain.contextservice.dto.DeploymentInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeploymentService {

    public DeploymentInfo getDeployment(String service) {

        return DeploymentInfo.builder()
                .version("v2.1.0")
                .deployedAt(LocalDateTime.now().minusMinutes(40))
                .build();
    }
}