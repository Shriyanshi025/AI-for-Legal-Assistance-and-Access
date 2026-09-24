package com.legalassist.service;

import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponse getDocument(UUID documentId);

    List<DocumentSummaryResponse> getUserDocuments(UUID userId);

    DocumentResponse uploadDocument(org.springframework.web.multipart.MultipartFile file, UUID userId);

    DocumentResponse extractAndSaveDocumentText(UUID documentId);

    List<com.legalassist.dto.DocumentPageResponse> getDocumentPages(UUID documentId);

    List<com.legalassist.dto.DocumentChunkResponse> chunkAndSaveDocument(UUID documentId);

    List<com.legalassist.dto.DocumentChunkResponse> getDocumentChunks(UUID documentId);

    List<com.legalassist.dto.DocumentChunkResponse> generateAndSaveEmbeddings(UUID documentId);

    byte[] downloadDocumentFile(UUID documentId);

    void deleteDocument(UUID documentId);

    DocumentResponse replaceDocument(UUID documentId, org.springframework.web.multipart.MultipartFile file);
}
