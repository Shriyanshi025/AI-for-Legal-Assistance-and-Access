package com.legalassist.controller;

import com.legalassist.dto.DocumentChunkResponse;
import com.legalassist.dto.DocumentPageResponse;
import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.dto.LegalAskRequest;
import com.legalassist.dto.SimilaritySearchRequest;
import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.service.DocumentService;
import com.legalassist.service.rag.LegalQaService;
import com.legalassist.service.search.SemanticSearchService;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final SemanticSearchService semanticSearchService;
    private final LegalQaService legalQaService;

    public DocumentController(
            DocumentService documentService,
            SemanticSearchService semanticSearchService,
            LegalQaService legalQaService
    ) {
        this.documentService = documentService;
        this.semanticSearchService = semanticSearchService;
        this.legalQaService = legalQaService;
    }


    private UUID resolveUserId(UUID queryUserId, UUID headerUserId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.legalassist.security.UserPrincipal principal) {
            return principal.getId();
        }
        return queryUserId != null ? queryUserId : headerUserId;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        DocumentResponse document = documentService.getDocument(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> getUserDocuments(
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        UUID effectiveUserId = resolveUserId(userId, headerUserId);
        if (effectiveUserId == null) {
            throw new IllegalArgumentException("userId is required to list documents");
        }
        List<DocumentSummaryResponse> documents = documentService.getUserDocuments(effectiveUserId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        DocumentResponse response = documentService.uploadDocument(file, resolveUserId(userId, headerUserId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/extract")
    public ResponseEntity<DocumentResponse> extractDocumentText(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        DocumentResponse response = documentService.extractAndSaveDocumentText(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<DocumentPageResponse>> getDocumentPages(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        List<DocumentPageResponse> pages = documentService.getDocumentPages(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(pages);
    }

    @PostMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> chunkDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        List<DocumentChunkResponse> chunks = documentService.chunkAndSaveDocument(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        List<DocumentChunkResponse> chunks = documentService.getDocumentChunks(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/embeddings")
    public ResponseEntity<List<DocumentChunkResponse>> generateEmbeddings(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        List<DocumentChunkResponse> chunks = documentService.generateAndSaveEmbeddings(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/search")
    public ResponseEntity<List<SimilaritySearchResultResponse>> searchSimilarChunks(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SimilaritySearchRequest request,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        UUID effectiveUserId = resolveUserId(userId, headerUserId);
        documentService.getDocument(id, effectiveUserId); // Ownership validation gate
        List<SimilaritySearchResultResponse> results = semanticSearchService.searchSimilarChunks(
                id,
                request.query(),
                request.getTopKOrDefault()
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/ask")
    public ResponseEntity<LegalAnswerResponse> askMultiDocumentQuestion(
            @Valid @RequestBody LegalAskRequest request,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        List<UUID> docIds = request.documentIds();
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one document before asking a question.");
        }
        UUID effectiveUserId = resolveUserId(userId, headerUserId);
        LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(docIds, request, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<LegalAnswerResponse> askQuestion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody LegalAskRequest request,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        UUID effectiveUserId = resolveUserId(userId, headerUserId);
        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(request.documentIds(), request, effectiveUserId);
            return ResponseEntity.ok(response);
        }
        LegalAnswerResponse response = legalQaService.askQuestion(id, request, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewDocumentFile(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        UUID effectiveUserId = resolveUserId(userId, headerUserId);
        DocumentResponse document = documentService.getDocument(id, effectiveUserId);
        byte[] pdfBytes = documentService.downloadDocumentFile(id, effectiveUserId);

        String filename = document.filename() != null ? document.filename() : "document.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        documentService.deleteDocument(id, resolveUserId(userId, headerUserId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> replaceDocument(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        DocumentResponse response = documentService.replaceDocument(id, file, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> replaceDocumentPost(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) UUID headerUserId
    ) {
        DocumentResponse response = documentService.replaceDocument(id, file, resolveUserId(userId, headerUserId));
        return ResponseEntity.ok(response);
    }
}

