package com.example.capstone.scheduler.job;

import com.example.capstone.scheduler.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_jobs")
public class JobRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    private Instant runAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Column(length = 1000)
    private String lastError;

    @Column(length = 160)
    private String lockedBy;

    private Instant lockedUntil;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JobRecord() {
    }

    public JobRecord(String name, String payload, Instant runAt, String idempotencyKey, int maxAttempts) {
        this.name = name;
        this.payload = payload;
        this.runAt = runAt;
        this.idempotencyKey = idempotencyKey;
        this.maxAttempts = maxAttempts;
    }

    public void start(String workerId, Instant now) {
        if (status == JobStatus.SUCCEEDED || status == JobStatus.FAILED) {
            throw new DomainException("Completed jobs cannot be started");
        }
        this.status = JobStatus.RUNNING;
        this.lockedBy = workerId;
        this.lockedUntil = now.plusSeconds(30);
    }

    public void markSucceeded(Instant now) {
        this.status = JobStatus.SUCCEEDED;
        this.completedAt = now;
        this.lastError = null;
        this.lockedUntil = now;
    }

    public void markFailure(String error, Instant now) {
        this.attempts++;
        this.lastError = error;
        this.lockedUntil = now;
        if (attempts >= maxAttempts) {
            this.status = JobStatus.FAILED;
            this.completedAt = now;
            return;
        }
        this.status = JobStatus.RETRY_SCHEDULED;
        long delaySeconds = Math.min(3600, (long) Math.pow(2, attempts - 1) * 15L);
        this.runAt = now.plusSeconds(delaySeconds);
    }

    public void runNow(Instant now) {
        if (status == JobStatus.SUCCEEDED) {
            throw new DomainException("Succeeded jobs are already complete");
        }
        this.status = JobStatus.QUEUED;
        this.runAt = now;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPayload() {
        return payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
