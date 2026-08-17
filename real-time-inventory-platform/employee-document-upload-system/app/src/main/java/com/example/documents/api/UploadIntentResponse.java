package com.example.documents.api;

import java.time.Instant;
import java.util.UUID;

public record UploadIntentResponse(
        UUID documentId,
        String bucket,
        String key,
        String uploadUrl,
        Instant expiresAt) {
}
