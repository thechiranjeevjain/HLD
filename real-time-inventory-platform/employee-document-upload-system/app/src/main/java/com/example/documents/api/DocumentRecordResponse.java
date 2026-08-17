package com.example.documents.api;

import java.time.Instant;
import java.util.UUID;

import com.example.documents.domain.DocumentRecord;
import com.example.documents.domain.DocumentStatus;

public record DocumentRecordResponse(
        UUID id,
        String ownerUserId,
        String originalFilename,
        String contentType,
        long contentLengthBytes,
        String storageBucket,
        String storageKey,
        DocumentStatus status,
        Instant createdAt,
        Instant reviewedAt,
        String reviewedBy,
        String reviewNote) {

    public static DocumentRecordResponse from(DocumentRecord document) {
        return new DocumentRecordResponse(
                document.getId(),
                document.getOwnerUserId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getContentLengthBytes(),
                document.getStorageBucket(),
                document.getStorageKey(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getReviewedAt(),
                document.getReviewedBy(),
                document.getReviewNote());
    }
}
