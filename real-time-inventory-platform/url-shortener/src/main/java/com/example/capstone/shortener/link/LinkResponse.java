package com.example.capstone.shortener.link;

import java.time.Instant;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        String code,
        String originalUrl,
        String ownerKey,
        long clickCount,
        boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static LinkResponse from(ShortLink link) {
        return new LinkResponse(
                link.getId(),
                link.getCode(),
                link.getOriginalUrl(),
                link.getOwnerKey(),
                link.getClickCount(),
                link.isActive(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
