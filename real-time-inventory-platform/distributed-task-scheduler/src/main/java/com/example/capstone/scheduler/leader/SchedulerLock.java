package com.example.capstone.scheduler.leader;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "scheduler_locks")
public class SchedulerLock {

    @Id
    @Column(name = "lock_name", length = 120)
    private String lockName;

    @Column(nullable = false, length = 160)
    private String ownerId;

    @Column(nullable = false)
    private Instant lockedUntil;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SchedulerLock() {
    }

    public SchedulerLock(String lockName, String ownerId, Instant lockedUntil) {
        this.lockName = lockName;
        this.ownerId = ownerId;
        this.lockedUntil = lockedUntil;
    }

    public void renew(String ownerId, Instant lockedUntil) {
        this.ownerId = ownerId;
        this.lockedUntil = lockedUntil;
    }

    @PrePersist
    @PreUpdate
    void onWrite() {
        this.updatedAt = Instant.now();
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
