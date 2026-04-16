package com.incidentbbrain.testingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}