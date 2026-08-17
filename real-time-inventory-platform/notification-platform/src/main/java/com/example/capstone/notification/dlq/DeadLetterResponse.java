package com.example.capstone.notification.dlq;

import com.example.capstone.notification.notification.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public record DeadLetterResponse(
        UUID id,
        UUID notificationId,
        NotificationChannel channel,
        String recipient,
        String reason,
        Instant createdAt
) {

    public static DeadLetterResponse from(DeadLetterRecord record) {
        return new DeadLetterResponse(
                record.getId(),
                record.getNotificationId(),
                record.getChannel(),
                record.getRecipient(),
                record.getReason(),
                record.getCreatedAt()
        );
    }
}
