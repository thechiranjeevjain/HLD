package com.example.capstone.shortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_links")
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(length = 160)
    private String ownerKey;

    @Column(nullable = false)
    private long clickCount;

    @Column(nullable = false)
    private boolean active = true;

    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ShortLink() {
    }

    public ShortLink(String code, String originalUrl, String ownerKey, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.ownerKey = ownerKey;
        this.expiresAt = expiresAt;
    }

    public void recordClick() {
        this.clickCount++;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
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

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public long getClickCount() {
        return clickCount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
