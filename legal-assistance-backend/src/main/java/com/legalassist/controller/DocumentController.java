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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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


    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable("id") UUID id) {
        DocumentResponse document = documentService.getDocument(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> getUserDocuments(@RequestParam("userId") UUID userId) {
        // Pre-auth contract: userId query parameter is temporary until security context authentication is added.
        List<DocumentSummaryResponse> documents = documentService.getUserDocuments(userId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId
    ) {
        DocumentResponse response = documentService.uploadDocument(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/extract")
    public ResponseEntity<DocumentResponse> extractDocumentText(@PathVariable("id") UUID id) {
        DocumentResponse response = documentService.extractAndSaveDocumentText(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<DocumentPageResponse>> getDocumentPages(@PathVariable("id") UUID id) {
        List<DocumentPageResponse> pages = documentService.getDocumentPages(id);
        return ResponseEntity.ok(pages);
    }

    @PostMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> chunkDocument(@PathVariable("id") UUID id) {
        List<DocumentChunkResponse> chunks = documentService.chunkAndSaveDocument(id);
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(@PathVariable("id") UUID id) {
        List<DocumentChunkResponse> chunks = documentService.getDocumentChunks(id);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/embeddings")
    public ResponseEntity<List<DocumentChunkResponse>> generateEmbeddings(@PathVariable("id") UUID id) {
        List<DocumentChunkResponse> chunks = documentService.generateAndSaveEmbeddings(id);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/{id}/search")
    public ResponseEntity<List<SimilaritySearchResultResponse>> searchSimilarChunks(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SimilaritySearchRequest request
    ) {
        List<SimilaritySearchResultResponse> results = semanticSearchService.searchSimilarChunks(
                id,
                request.query(),
                request.getTopKOrDefault()
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<LegalAnswerResponse> askQuestion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody LegalAskRequest request
    ) {
        LegalAnswerResponse response = legalQaService.askQuestion(id, request);
        return ResponseEntity.ok(response);
    }
}

