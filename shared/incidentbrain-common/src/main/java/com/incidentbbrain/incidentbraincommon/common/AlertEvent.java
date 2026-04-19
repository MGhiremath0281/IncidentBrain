package com.incidentbbrain.incidentbraincommon.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent implements Serializable {

    private String alertId;
    private String serviceName;
    private String severity;
    private String message;
    private String host;
    private String timestamp;

    private String alertType;

    private String source;

    private String reason;
}