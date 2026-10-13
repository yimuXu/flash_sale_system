package com.yimu.flash_sale_system.dto;

public record LoginResponse(String token, Long userId, String role) {
    
}
