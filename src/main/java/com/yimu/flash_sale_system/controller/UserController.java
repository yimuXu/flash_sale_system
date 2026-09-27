package com.yimu.flash_sale_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.yimu.flash_sale_system.dto.LoginRequest;
import com.yimu.flash_sale_system.dto.SignupRequest;
import com.yimu.flash_sale_system.dto.UserResponse;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.service.UserService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    // Inject UserService
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Define endpoints for user-related operations
    @PostMapping("/users/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody SignupRequest userRequest) {
        User user = userService.signup(userRequest);
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping("/users/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest);
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @RequestBody SignupRequest userRequest) {
        User user = userService.updateUser(userId, userRequest);
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.ok(userResponse);
    }

}