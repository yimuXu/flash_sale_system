package com.yimu.flash_sale_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;
import com.yimu.flash_sale_system.repository.UserRepository;

@Configuration
public class AdminSeedConfig {
    // Create the admin account on startup, only when ADMIN_PASSWORD is provided
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${admin.email:admin@flashsale.local}") String email,
                                @Value("${admin.password:}") String password) {
        return args -> {
            if (password.isBlank() || users.findByEmail(email).isPresent()) return;
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(encoder.encode(password));
            admin.setRole(UserRoles.ADMIN);
            users.save(admin);
        };
    }
}
