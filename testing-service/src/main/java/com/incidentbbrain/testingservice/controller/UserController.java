package com.incidentbbrain.testingservice.controller;

import com.incidentbbrain.testingservice.dto.UserRequest;
import com.incidentbbrain.testingservice.dto.UserResponse;
import com.incidentbbrain.testingservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public UserResponse create(@RequestBody UserRequest request) {
        return service.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return service.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id,
                               @RequestBody UserRequest request) {
        return service.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deleteUser(id);
    }
}