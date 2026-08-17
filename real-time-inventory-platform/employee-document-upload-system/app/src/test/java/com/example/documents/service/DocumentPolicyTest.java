package com.example.documents.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.example.documents.domain.DocumentRecord;
import com.example.documents.security.AuthenticatedUser;

class DocumentPolicyTest {

    private final DocumentPolicy policy = new DocumentPolicy();

    @Test
    void employeeCanCreateUploadForSelf() {
        AuthenticatedUser user = new AuthenticatedUser("employee-123", Set.of("EMPLOYEE"));

        assertDoesNotThrow(() -> policy.assertCanCreateUpload(user, "employee-123"));
    }

    @Test
    void employeeCannotCreateUploadForAnotherEmployee() {
        AuthenticatedUser user = new AuthenticatedUser("employee-123", Set.of("EMPLOYEE"));

        assertThrows(AccessDeniedException.class, () -> policy.assertCanCreateUpload(user, "employee-999"));
    }

    @Test
    void auditorCanReadMetadataButCannotReview() {
        AuthenticatedUser auditor = new AuthenticatedUser("auditor-1", Set.of("AUDITOR"));
        DocumentRecord document = documentFor("employee-123");

        assertDoesNotThrow(() -> policy.assertCanRead(auditor, document));
        assertThrows(AccessDeniedException.class, () -> policy.assertCanReview(auditor));
    }

    @Test
    void hrReviewerCanReviewDocuments() {
        AuthenticatedUser reviewer = new AuthenticatedUser("hr-1", Set.of("HR_REVIEWER"));

        assertDoesNotThrow(() -> policy.assertCanReview(reviewer));
    }

    private DocumentRecord documentFor(String ownerUserId) {
        return new DocumentRecord(
                UUID.randomUUID(),
                ownerUserId,
                "passport.pdf",
                "application/pdf",
                4096,
                "employee-documents",
                "employee/%s/passport.pdf".formatted(ownerUserId),
                Instant.parse("2026-08-05T00:00:00Z"));
    }
}
