package com.example.capstone.auth.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String email,
        String role
) {
}
