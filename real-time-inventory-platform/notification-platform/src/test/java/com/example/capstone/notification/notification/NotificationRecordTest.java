package com.example.capstone.notification.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationRecordTest {

    @Test
    void failureSchedulesRetryBeforeMaxAttempts() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        NotificationRecord notification = new NotificationRecord(
                NotificationChannel.EMAIL,
                "user@example.com",
                "Subject",
                "Body",
                3,
                now
        );

        notification.markFailure("temporary failure", now);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRY_SCHEDULED);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));
    }

    @Test
    void failureMovesToDeadLetterAtMaxAttempts() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        NotificationRecord notification = new NotificationRecord(
                NotificationChannel.SMS,
                "+1555000",
                null,
                "Body",
                2,
                now
        );

        notification.markFailure("first failure", now);
        notification.markFailure("second failure", now.plusSeconds(30));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DEAD_LETTER);
        assertThat(notification.getAttempts()).isEqualTo(2);
        assertThat(notification.getLastError()).isEqualTo("second failure");
    }
}
