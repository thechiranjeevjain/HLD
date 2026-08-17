package com.example.capstone.notification.notification;

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
@Table(name = "notifications")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(length = 240)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 1000)
    private String lastError;

    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(NotificationChannel channel, String recipient, String subject, String body, int maxAttempts, Instant now) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = now;
    }

    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    public void markFailure(String error, Instant now) {
        this.attempts++;
        this.lastError = error;
        if (attempts >= maxAttempts) {
            this.status = NotificationStatus.DEAD_LETTER;
            this.nextAttemptAt = now;
            return;
        }

        this.status = NotificationStatus.RETRY_SCHEDULED;
        long delaySeconds = Math.min(3600, (long) Math.pow(2, attempts - 1) * 30L);
        this.nextAttemptAt = now.plusSeconds(delaySeconds);
    }

    public void requeueManually(Instant now) {
        this.status = NotificationStatus.RETRY_SCHEDULED;
        this.nextAttemptAt = now;
        this.maxAttempts = Math.max(maxAttempts, attempts + 1);
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

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
