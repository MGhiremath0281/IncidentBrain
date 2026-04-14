package com.incidentbbrain.testingservice.service;

import com.incidentbbrain.testingservice.dto.UserRequest;
import com.incidentbbrain.testingservice.dto.UserResponse;
import com.incidentbbrain.testingservice.entity.User;
import com.incidentbbrain.testingservice.exception.DuplicateUserException;
import com.incidentbbrain.testingservice.exception.UserNotFoundException;
import com.incidentbbrain.testingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final Random random = new Random();

    public UserResponse createUser(UserRequest request) {

        log.info("Creating user email={}", request.getEmail());

        if (random.nextInt(10) < 2) {
            log.error("Random failure while creating user");
            throw new RuntimeException("Internal service error");
        }

        repository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new DuplicateUserException("Email already exists");
        });

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .createdAt(LocalDateTime.now())
                .build();

        User saved = repository.save(user);

        log.info("User created successfully id={}", saved.getId());

        return map(saved);
    }

    public UserResponse getUser(String id) {

        log.info("Fetching user id={}", id);

        simulateLatency();

        User user = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found id={}", id);
                    return new UserNotFoundException("User not found");
                });

        return map(user);
    }

    public UserResponse updateUser(String id, UserRequest request) {

        log.info("Updating user id={}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User updated = repository.save(user);

        log.info("User updated id={}", id);

        return map(updated);
    }

    public void deleteUser(String id) {

        log.warn("Deleting user id={}", id);

        if (!repository.existsById(id)) {
            throw new UserNotFoundException("User not found");
        }

        repository.deleteById(id);

        log.info("User deleted id={}", id);
    }

    private void simulateLatency() {
        try {
            if (random.nextInt(10) < 3) {
                Thread.sleep(1000 + random.nextInt(2000));
                log.warn("Slow response simulated");
            }
        } catch (InterruptedException ignored) {}
    }

    private UserResponse map(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
