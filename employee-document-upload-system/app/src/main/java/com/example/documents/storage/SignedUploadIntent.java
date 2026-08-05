package com.example.documents.storage;

import java.time.Instant;

public record SignedUploadIntent(String bucket, String key, String uploadUrl, Instant expiresAt) {
}
