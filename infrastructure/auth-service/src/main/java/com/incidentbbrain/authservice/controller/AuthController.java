package com.incidentbbrain.authservice.controller;

import com.incidentbbrain.authservice.dto.AuthResponse;
import com.incidentbbrain.authservice.dto.ChangePasswordRequest;
import com.incidentbbrain.authservice.dto.LoginRequest;
import com.incidentbbrain.authservice.dto.RegisterRequest;
import com.incidentbbrain.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }
}