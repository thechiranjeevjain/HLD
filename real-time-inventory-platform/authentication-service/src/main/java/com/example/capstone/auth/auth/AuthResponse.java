package com.example.capstone.auth.auth;

import com.example.capstone.auth.user.UserResponse;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}
