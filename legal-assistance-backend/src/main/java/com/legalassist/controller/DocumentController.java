package com.legalassist.controller;

import com.legalassist.dto.DocumentChunkResponse;
import com.legalassist.dto.DocumentPageResponse;
import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.dto.SimilaritySearchRequest;
import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.dto.LegalAskRequest;
import com.legalassist.security.UserPrincipal;
import com.legalassist.service.DocumentService;
import com.legalassist.service.rag.LegalQaService;
import com.legalassist.service.search.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BadCredentialsException("Unauthenticated user");
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable("id") UUID id
    ) {
        DocumentResponse document = documentService.getDocument(id, resolveUserId());
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> getUserDocuments() {
        UUID effectiveUserId = resolveUserId();
        List<DocumentSummaryResponse> documents = documentService.getUserDocuments(effectiveUserId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.uploadDocument(file, resolveUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/extract")
    public ResponseEntity<DocumentResponse> extractDocumentText(
            @PathVariable("id") UUID id
    ) {
        DocumentResponse response = documentService.extractAndSaveDocumentText(id, resolveUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<DocumentPageResponse>> getDocumentPages(
            @PathVariable("id") UUID id
    ) {
        List<DocumentPageResponse> pages = documentService.getDocumentPages(id, resolveUserId());
        return ResponseEntity.ok(pages);
    }

    @PostMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> chunkDocument(
            @PathVariable("id") UUID id
    ) {
        List<DocumentChunkResponse> chunks = documentService.chunkAndSaveDocument(id, resolveUserId());
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(
            @PathVariable("id") UUID id
    ) {
        List<DocumentChunkResponse> chunks = documentService.getDocumentChunks(id, resolveUserId());
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/embeddings")
    public ResponseEntity<List<DocumentChunkResponse>> generateEmbeddings(
            @PathVariable("id") UUID id
    ) {
        List<DocumentChunkResponse> chunks = documentService.generateAndSaveEmbeddings(id, resolveUserId());
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/search")
    public ResponseEntity<List<SimilaritySearchResultResponse>> searchSimilarChunks(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SimilaritySearchRequest request
    ) {
        UUID effectiveUserId = resolveUserId();
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
            @Valid @RequestBody LegalAskRequest request
    ) {
        List<UUID> docIds = request.documentIds();
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one document before asking a question.");
        }
        UUID effectiveUserId = resolveUserId();
        LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(docIds, request, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<LegalAnswerResponse> askQuestion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody LegalAskRequest request
    ) {
        UUID effectiveUserId = resolveUserId();
        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(request.documentIds(), request, effectiveUserId);
            return ResponseEntity.ok(response);
        }
        LegalAnswerResponse response = legalQaService.askQuestion(id, request, effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewDocumentFile(
            @PathVariable("id") UUID id
    ) {
        UUID effectiveUserId = resolveUserId();
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
            @PathVariable("id") UUID id
    ) {
        documentService.deleteDocument(id, resolveUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> replaceDocument(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.replaceDocument(id, file, resolveUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> replaceDocumentPost(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.replaceDocument(id, file, resolveUserId());
        return ResponseEntity.ok(response);
    }
}

