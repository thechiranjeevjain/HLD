package com.example.capstone.auth.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Role role,
        String provider,
        boolean enabled,
        Instant createdAt
) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getProvider(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
