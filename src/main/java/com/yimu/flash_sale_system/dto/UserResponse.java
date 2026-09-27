package com.yimu.flash_sale_system.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;

public class UserResponse {
    private Long id;
    private String email;
    private UserRoles role;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRoles getRole() {
        return role;
    }

    public void setRole(UserRoles role) {
        this.role = role;
    }

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }

    public static List<UserResponse> fromUsers(List<User> users) {
        return users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }
}
