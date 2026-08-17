package com.example.documents.api;

import com.example.documents.domain.DocumentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewDocumentRequest(
        @NotNull DocumentStatus status,
        @Size(max = 1000) String note) {
}
