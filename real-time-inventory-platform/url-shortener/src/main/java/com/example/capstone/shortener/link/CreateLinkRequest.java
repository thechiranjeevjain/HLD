package com.example.capstone.shortener.link;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "https?://.+", message = "must start with http:// or https://")
        String originalUrl,

        @Size(max = 160)
        String ownerKey,

        @Future
        Instant expiresAt
) {
}
