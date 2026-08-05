package com.example.documents.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_documents")
public class DocumentRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String ownerUserId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long contentLengthBytes;

    @Column(nullable = false)
    private String storageBucket;

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    private String reviewedBy;

    @Column(length = 1000)
    private String reviewNote;

    protected DocumentRecord() {
    }

    public DocumentRecord(
            UUID id,
            String ownerUserId,
            String originalFilename,
            String contentType,
            long contentLengthBytes,
            String storageBucket,
            String storageKey,
            Instant createdAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.contentLengthBytes = contentLengthBytes;
        this.storageBucket = storageBucket;
        this.storageKey = storageKey;
        this.status = DocumentStatus.PENDING_REVIEW;
        this.createdAt = createdAt;
    }

    public void review(DocumentStatus status, String reviewerUserId, String note, Instant reviewedAt) {
        if (status != DocumentStatus.APPROVED && status != DocumentStatus.REJECTED) {
            throw new IllegalArgumentException("Review status must be APPROVED or REJECTED.");
        }
        this.status = status;
        this.reviewedBy = reviewerUserId;
        this.reviewNote = note;
        this.reviewedAt = reviewedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLengthBytes() {
        return contentLengthBytes;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewNote() {
        return reviewNote;
    }
}
