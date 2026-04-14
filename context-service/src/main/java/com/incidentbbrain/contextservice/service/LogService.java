package com.incidentbbrain.contextservice.service;

import org.springframework.stereotype.Service;
import java.util.Arrays; // Add this import
import java.util.List;

@Service
public class LogService {

    public List<String> getLogs(String service, String severity) {
        return Arrays.asList(
                severity + " ERROR in " + service + ": DB timeout",
                "WARN: latency spike detected",
                "INFO: retry successful"
        );
    }
}