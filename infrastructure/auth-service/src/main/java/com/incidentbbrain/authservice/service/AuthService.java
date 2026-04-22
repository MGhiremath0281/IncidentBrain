package com.incidentbbrain.authservice.service;

import com.incidentbbrain.authservice.dto.AuthResponse;
import com.incidentbbrain.authservice.dto.ChangePasswordRequest;
import com.incidentbbrain.authservice.dto.LoginRequest;
import com.incidentbbrain.authservice.dto.RegisterRequest;
import com.incidentbbrain.authservice.entity.Team;
import com.incidentbbrain.authservice.entity.User;
import com.incidentbbrain.authservice.repository.TeamRepository;
import com.incidentbbrain.authservice.repository.UserRepository;
import com.incidentbbrain.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String register(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        Team team = teamRepository.findByName(request.getTeamName())
                .orElseGet(() -> teamRepository.save(
                        Team.builder().name(request.getTeamName()).build()
                ));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .team(team)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getTeam().getName());
        return new AuthResponse(token);
    }

    public String changePassword(ChangePasswordRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return "Password updated successfully";
    }
}