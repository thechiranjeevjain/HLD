package com.example.capstone.auth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MockOAuth2LoginRequest(
        @NotBlank
        @Size(max = 64)
        String provider,

        @NotBlank
        @Size(max = 160)
        String providerSubject,

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(max = 160)
        String displayName
) {
}
