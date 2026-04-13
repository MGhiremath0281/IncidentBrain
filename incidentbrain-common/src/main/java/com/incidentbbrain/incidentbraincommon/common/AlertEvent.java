package com.incidentbbrain.incidentbraincommon.common;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertEvent {

    private String alertId;
    private String serviceName;
    private String severity;
    private String message;
    private String host;
    private String timestamp;
}