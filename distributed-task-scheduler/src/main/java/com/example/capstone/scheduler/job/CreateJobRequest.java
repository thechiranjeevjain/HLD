package com.example.capstone.scheduler.job;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateJobRequest(
        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 4000)
        String payload,

        @FutureOrPresent
        Instant runAt,

        @NotBlank
        @Size(max = 160)
        String idempotencyKey,

        @Min(1)
        @Max(10)
        Integer maxAttempts
) {
}
