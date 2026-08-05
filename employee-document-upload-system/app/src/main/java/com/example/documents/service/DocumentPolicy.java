package com.example.documents.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.example.documents.domain.DocumentRecord;
import com.example.documents.security.AuthenticatedUser;

@Component
public class DocumentPolicy {

    public void assertCanCreateUpload(AuthenticatedUser user, String requestedOwnerUserId) {
        if (user.hasAnyRole("ADMIN", "HR_REVIEWER")) {
            return;
        }

        if (user.hasRole("EMPLOYEE") && user.subject().equals(requestedOwnerUserId)) {
            return;
        }

        throw new AccessDeniedException("User cannot create an upload intent for this employee.");
    }

    public void assertCanRead(AuthenticatedUser user, DocumentRecord document) {
        if (user.hasAnyRole("ADMIN", "HR_REVIEWER", "AUDITOR")) {
            return;
        }

        if (user.hasRole("EMPLOYEE") && user.subject().equals(document.getOwnerUserId())) {
            return;
        }

        throw new AccessDeniedException("User cannot read this document metadata.");
    }

    public boolean canListAll(AuthenticatedUser user) {
        return user.hasAnyRole("ADMIN", "HR_REVIEWER", "AUDITOR");
    }

    public void assertCanReview(AuthenticatedUser user) {
        if (user.hasAnyRole("ADMIN", "HR_REVIEWER")) {
            return;
        }

        throw new AccessDeniedException("User cannot review documents.");
    }
}
