package com.legalassist.service;

import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponse getDocument(UUID documentId);
    DocumentResponse getDocument(UUID documentId, UUID userId);

    List<DocumentSummaryResponse> getUserDocuments(UUID userId);

    DocumentResponse uploadDocument(org.springframework.web.multipart.MultipartFile file, UUID userId);

    DocumentResponse extractAndSaveDocumentText(UUID documentId);
    DocumentResponse extractAndSaveDocumentText(UUID documentId, UUID userId);

    List<com.legalassist.dto.DocumentPageResponse> getDocumentPages(UUID documentId);
    List<com.legalassist.dto.DocumentPageResponse> getDocumentPages(UUID documentId, UUID userId);

    List<com.legalassist.dto.DocumentChunkResponse> chunkAndSaveDocument(UUID documentId);
    List<com.legalassist.dto.DocumentChunkResponse> chunkAndSaveDocument(UUID documentId, UUID userId);

    List<com.legalassist.dto.DocumentChunkResponse> getDocumentChunks(UUID documentId);
    List<com.legalassist.dto.DocumentChunkResponse> getDocumentChunks(UUID documentId, UUID userId);

    List<com.legalassist.dto.DocumentChunkResponse> generateAndSaveEmbeddings(UUID documentId);
    List<com.legalassist.dto.DocumentChunkResponse> generateAndSaveEmbeddings(UUID documentId, UUID userId);

    byte[] downloadDocumentFile(UUID documentId);
    byte[] downloadDocumentFile(UUID documentId, UUID userId);

    void deleteDocument(UUID documentId);
    void deleteDocument(UUID documentId, UUID userId);

    DocumentResponse replaceDocument(UUID documentId, org.springframework.web.multipart.MultipartFile file);
    DocumentResponse replaceDocument(UUID documentId, org.springframework.web.multipart.MultipartFile file, UUID userId);
}
