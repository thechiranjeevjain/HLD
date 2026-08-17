package com.example.capstone.notification.dlq;

import com.example.capstone.notification.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_notifications")
public class DeadLetterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DeadLetterRecord() {
    }

    public DeadLetterRecord(UUID notificationId, NotificationChannel channel, String recipient, String reason) {
        this.notificationId = notificationId;
        this.channel = channel;
        this.recipient = recipient;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
