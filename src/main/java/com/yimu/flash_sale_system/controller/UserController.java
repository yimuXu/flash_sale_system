package com.yimu.flash_sale_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.yimu.flash_sale_system.dto.LoginRequest;
import com.yimu.flash_sale_system.dto.LoginResponse;
import com.yimu.flash_sale_system.dto.SignupRequest;
import com.yimu.flash_sale_system.dto.UserResponse;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.service.JwtService;
import com.yimu.flash_sale_system.service.UserService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    // Inject UserService
    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    // Define endpoints for user-related operations
    @PostMapping("/users/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody SignupRequest userRequest) {
        User user = userService.signup(userRequest);
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping("/users/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest);
        String token = jwtService.generateToken(user);
        LoginResponse loginResponse = new LoginResponse(token, user.getId(), user.getRole().name());
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Long userId) {

        UserResponse userResponse = UserResponse.fromUser(userService.getUserById(userId));
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/users/me")
    public ResponseEntity<UserResponse> updateUser(@AuthenticationPrincipal Long userId, @RequestBody SignupRequest userRequest) {
        UserResponse userResponse = UserResponse.fromUser(userService.updateUser(userId, userRequest));
        return ResponseEntity.ok(userResponse);
    }

}