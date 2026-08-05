package com.example.documents.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.documents.security.CurrentUser;
import com.example.documents.service.DocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload-intents")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadIntentResponse createUploadIntent(
            @Valid @RequestBody UploadIntentRequest request,
            Authentication authentication) {
        return documentService.createUploadIntent(CurrentUser.from(authentication), request);
    }

    @GetMapping
    public List<DocumentRecordResponse> listDocuments(Authentication authentication) {
        return documentService.listDocuments(CurrentUser.from(authentication));
    }

    @PostMapping("/{documentId}/review")
    public DocumentRecordResponse reviewDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody ReviewDocumentRequest request,
            Authentication authentication) {
        return documentService.reviewDocument(CurrentUser.from(authentication), documentId, request);
    }
}
