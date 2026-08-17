package com.example.capstone.notification.notification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull
        NotificationChannel channel,

        @NotBlank
        @Size(max = 320)
        String recipient,

        @Size(max = 240)
        String subject,

        @NotBlank
        @Size(max = 4000)
        String body,

        @Min(1)
        @Max(10)
        Integer maxAttempts
) {
}
