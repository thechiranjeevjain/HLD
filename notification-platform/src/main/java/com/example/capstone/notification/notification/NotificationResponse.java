package com.example.capstone.notification.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationChannel channel,
        String recipient,
        String subject,
        NotificationStatus status,
        int attempts,
        int maxAttempts,
        Instant nextAttemptAt,
        String lastError,
        Instant sentAt,
        Instant createdAt
) {

    public static NotificationResponse from(NotificationRecord notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getStatus(),
                notification.getAttempts(),
                notification.getMaxAttempts(),
                notification.getNextAttemptAt(),
                notification.getLastError(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
