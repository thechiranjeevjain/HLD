package com.example.capstone.scheduler.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
public class JobExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Column(nullable = false, length = 1000)
    private String result;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected JobExecutionRecord() {
    }

    public JobExecutionRecord(UUID jobId, String idempotencyKey, String result) {
        this.jobId = jobId;
        this.idempotencyKey = idempotencyKey;
        this.result = result;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
