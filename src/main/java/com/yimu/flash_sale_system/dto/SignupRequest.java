package com.yimu.flash_sale_system.dto;

import com.yimu.flash_sale_system.entity.UserRoles;

public class SignupRequest {
    private String email;
    private String password;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
}
