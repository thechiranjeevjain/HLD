package com.example.documents.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.documents.api.DocumentRecordResponse;
import com.example.documents.api.ReviewDocumentRequest;
import com.example.documents.api.UploadIntentRequest;
import com.example.documents.api.UploadIntentResponse;
import com.example.documents.domain.DocumentRecord;
import com.example.documents.repository.DocumentRecordRepository;
import com.example.documents.security.AuthenticatedUser;
import com.example.documents.storage.ObjectStoragePort;
import com.example.documents.storage.SignedUploadIntent;

@Service
public class DocumentService {

    private final DocumentRecordRepository repository;
    private final ObjectStoragePort objectStorage;
    private final DocumentPolicy policy;
    private final Clock clock;

    public DocumentService(
            DocumentRecordRepository repository,
            ObjectStoragePort objectStorage,
            DocumentPolicy policy,
            Clock clock) {
        this.repository = repository;
        this.objectStorage = objectStorage;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public UploadIntentResponse createUploadIntent(AuthenticatedUser user, UploadIntentRequest request) {
        policy.assertCanCreateUpload(user, request.ownerUserId());

        UUID documentId = UUID.randomUUID();
        Instant createdAt = Instant.now(clock);
        String key = buildStorageKey(request.ownerUserId(), documentId, request.originalFilename(), createdAt);
        SignedUploadIntent signedUpload = objectStorage.createUploadIntent(
                key,
                request.contentType(),
                request.contentLengthBytes());

        DocumentRecord document = new DocumentRecord(
                documentId,
                request.ownerUserId(),
                request.originalFilename(),
                request.contentType(),
                request.contentLengthBytes(),
                signedUpload.bucket(),
                signedUpload.key(),
                createdAt);

        repository.save(document);

        return new UploadIntentResponse(
                documentId,
                signedUpload.bucket(),
                signedUpload.key(),
                signedUpload.uploadUrl(),
                signedUpload.expiresAt());
    }

    @Transactional(readOnly = true)
    public List<DocumentRecordResponse> listDocuments(AuthenticatedUser user) {
        if (policy.canListAll(user)) {
            return repository.findAllByOrderByCreatedAtDesc().stream()
                    .map(DocumentRecordResponse::from)
                    .toList();
        }

        return repository.findByOwnerUserIdOrderByCreatedAtDesc(user.subject()).stream()
                .peek(document -> policy.assertCanRead(user, document))
                .map(DocumentRecordResponse::from)
                .toList();
    }

    @Transactional
    public DocumentRecordResponse reviewDocument(AuthenticatedUser user, UUID documentId, ReviewDocumentRequest request) {
        policy.assertCanReview(user);

        DocumentRecord document = repository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));

        document.review(request.status(), user.subject(), request.note(), Instant.now(clock));

        return DocumentRecordResponse.from(document);
    }

    private String buildStorageKey(String ownerUserId, UUID documentId, String originalFilename, Instant createdAt) {
        LocalDate uploadDate = LocalDate.ofInstant(createdAt, clock.getZone());
        return "employee/%s/%s/%s-%s".formatted(
                sanitize(ownerUserId),
                uploadDate,
                documentId,
                sanitize(originalFilename));
    }

    private String sanitize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }
}
