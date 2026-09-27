package com.yimu.flash_sale_system.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.yimu.flash_sale_system.config.SecurityConfig;
import com.yimu.flash_sale_system.dto.LoginRequest;
import com.yimu.flash_sale_system.dto.SignupRequest;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;
import com.yimu.flash_sale_system.exception.InvalidCredentialsException;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(SignupRequest userRequest) {
        Optional<User> existuser = userRepository.findByEmail(userRequest.getEmail());
        if (existuser.isPresent()) {
            throw new RuntimeException("User already exists");
        }
        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setRole(UserRoles.USER);
        return userRepository.save(user);
    }
    public User login(LoginRequest loginRequest) {
        Optional<User> user = userRepository.findByEmail(loginRequest.getEmail());
        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            return user.get();
        }
        throw new InvalidCredentialsException("Invalid email or password");
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(Long userId, SignupRequest userRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        return userRepository.save(user);
    }

    
}
