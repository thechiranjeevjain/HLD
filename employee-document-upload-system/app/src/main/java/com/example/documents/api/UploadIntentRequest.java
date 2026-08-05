package com.example.documents.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UploadIntentRequest(
        @NotBlank String ownerUserId,
        @NotBlank String originalFilename,
        @NotBlank String contentType,
        @Positive @Max(25_000_000) long contentLengthBytes) {
}
