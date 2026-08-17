package com.example.capstone.scheduler.job;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String name,
        JobStatus status,
        Instant runAt,
        int attempts,
        int maxAttempts,
        String idempotencyKey,
        String lastError,
        String lockedBy,
        Instant lockedUntil,
        Instant completedAt,
        Instant createdAt
) {

    public static JobResponse from(JobRecord job) {
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getStatus(),
                job.getRunAt(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getIdempotencyKey(),
                job.getLastError(),
                job.getLockedBy(),
                job.getLockedUntil(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
