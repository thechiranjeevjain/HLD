package com.example.capstone.auth.config;

import com.example.capstone.auth.user.Role;
import com.example.capstone.auth.user.UserAccount;
import com.example.capstone.auth.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)
                || userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        userRepository.save(new UserAccount(
                adminEmail.trim().toLowerCase(),
                "Admin",
                passwordEncoder.encode(adminPassword),
                Role.ADMIN,
                null,
                null
        ));
    }
}
