package com.legalassist.service;

import com.legalassist.config.ChunkingProperties;
import com.legalassist.dto.DocumentChunkResponse;
import com.legalassist.dto.DocumentPageResponse;
import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentChunk;
import com.legalassist.entity.DocumentPage;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.exception.ChunkingException;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.exception.PdfExtractionException;
import com.legalassist.repository.DocumentChunkRepository;
import com.legalassist.repository.DocumentPageRepository;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.service.chunking.RawChunk;
import com.legalassist.service.chunking.TextChunkingService;
import com.legalassist.service.pdf.ExtractedPage;
import com.legalassist.service.pdf.PdfExtractionResult;
import com.legalassist.service.pdf.PdfTextExtractionService;
import com.legalassist.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMapper documentMapper;
    private final StorageService storageService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final TextChunkingService textChunkingService;
    private final ChunkingProperties chunkingProperties;
    private final com.legalassist.service.embedding.EmbeddingService embeddingService;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            DocumentPageRepository documentPageRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentMapper documentMapper,
            StorageService storageService,
            PdfTextExtractionService pdfTextExtractionService,
            TextChunkingService textChunkingService,
            ChunkingProperties chunkingProperties,
            com.legalassist.service.embedding.EmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.documentPageRepository = documentPageRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentMapper = documentMapper;
        this.storageService = storageService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.textChunkingService = textChunkingService;
        this.chunkingProperties = chunkingProperties;
        this.embeddingService = embeddingService;
    }

    @Override
    public DocumentResponse getDocument(UUID documentId) {
        return getDocument(documentId, null);
    }

    @Override
    public DocumentResponse getDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    public List<DocumentSummaryResponse> getUserDocuments(UUID userId) {
        List<Document> documents = documentRepository.findByUserId(userId);
        return documents.stream()
                .map(documentMapper::toDocumentSummaryResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, UUID userId) {
        validatePdfFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file content", e);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_document.pdf";
        } else {
            // Strip paths to prevent directory traversal
            int lastSep = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
            if (lastSep >= 0) {
                originalFilename = originalFilename.substring(lastSep + 1);
            }
        }

        UUID documentId = UUID.randomUUID();
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String safeStorageKey = String.format("%s/%s-%s",
                userId != null ? userId.toString() : "anonymous",
                documentId,
                safeFilename
        );

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/pdf";
        }

        String storagePath = storageService.uploadFile(safeStorageKey, bytes, contentType);

        Instant now = Instant.now();
        Document document = new Document(
                documentId,
                userId,
                originalFilename,
                contentType,
                storagePath,
                file.getSize(),
                DocumentStatus.UPLOADED,
                now,
                now
        );

        Document savedDocument;
        try {
            savedDocument = documentRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to persist document metadata after storage upload. Cleaning up storage at path: {}", storagePath, e);
            try {
                storageService.deleteFile(storagePath);
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup orphaned storage file at path: {}", storagePath, cleanupEx);
            }
            throw e;
        }

        return documentMapper.toDocumentResponse(savedDocument);
    }

    @Override
    @Transactional
    public DocumentResponse extractAndSaveDocumentText(UUID documentId) {
        return extractAndSaveDocumentText(documentId, null);
    }

    @Override
    @Transactional
    public DocumentResponse extractAndSaveDocumentText(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);

        boolean isPdf = (document.getFilename() != null && document.getFilename().toLowerCase().endsWith(".pdf"))
                || (document.getDocumentType() != null && document.getDocumentType().toLowerCase().contains("pdf"));

        if (!isPdf) {
            throw new PdfExtractionException("Document with id " + documentId + " is not a PDF file");
        }

        document.setStatus(DocumentStatus.PROCESSING);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        byte[] pdfBytes;
        try {
            pdfBytes = storageService.downloadFile(document.getStoragePath());
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            throw e;
        }

        PdfExtractionResult extractionResult;
        try {
            extractionResult = pdfTextExtractionService.extractText(pdfBytes);
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            throw e;
        }

        Instant now = Instant.now();
        List<DocumentPage> pageEntities = new ArrayList<>();
        for (ExtractedPage page : extractionResult.pages()) {
            pageEntities.add(new DocumentPage(
                    UUID.randomUUID(),
                    documentId,
                    page.pageNumber(),
                    page.text(),
                    now
            ));
        }

        if (!pageEntities.isEmpty()) {
            documentPageRepository.saveAll(pageEntities);
        }

        if (extractionResult.hasExtractableText()) {
            document.setStatus(DocumentStatus.READY);
        } else {
            // Scanned / image-only PDF with no machine readable text
            document.setStatus(DocumentStatus.FAILED);
        }

        document.setUpdatedAt(now);
        Document savedDoc = documentRepository.save(document);
        return documentMapper.toDocumentResponse(savedDoc);
    }

    @Override
    public List<DocumentPageResponse> getDocumentPages(UUID documentId) {
        return getDocumentPages(documentId, null);
    }

    @Override
    public List<DocumentPageResponse> getDocumentPages(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);
        List<DocumentPage> pages = documentPageRepository.findByDocumentIdOrderByPageNumberAsc(documentId);
        return pages.stream()
                .map(documentMapper::toDocumentPageResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<DocumentChunkResponse> chunkAndSaveDocument(UUID documentId) {
        return chunkAndSaveDocument(documentId, null);
    }

    @Override
    @Transactional
    public List<DocumentChunkResponse> chunkAndSaveDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);

        if (document.getStatus() != DocumentStatus.READY) {
            throw new IllegalStateException("Document is not ready for chunking. Current status: " + document.getStatus());
        }

        List<DocumentPage> pages = documentPageRepository.findByDocumentIdOrderByPageNumberAsc(documentId);
        if (pages.isEmpty()) {
            throw new ChunkingException("Document has no extracted pages to chunk");
        }

        List<RawChunk> rawChunks = textChunkingService.createChunks(
                pages,
                chunkingProperties.maxChunkSize(),
                chunkingProperties.overlapSize()
        );

        if (rawChunks.isEmpty()) {
            log.info("No non-empty text content found across {} pages for document {}", pages.size(), documentId);
            return Collections.emptyList();
        }

        // Remove old chunks for idempotency / reprocessing
        documentChunkRepository.deleteByDocumentId(documentId);

        List<DocumentChunk> chunkEntities = new ArrayList<>(rawChunks.size());
        for (RawChunk raw : rawChunks) {
            chunkEntities.add(new DocumentChunk(
                    UUID.randomUUID(),
                    documentId,
                    raw.pageNumber(),
                    raw.section(),
                    raw.clause(),
                    raw.content(),
                    raw.chunkIndex()
            ));
        }

        List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunkEntities);
        log.info("Successfully persisted {} text chunks for document {}", savedChunks.size(), documentId);

        return savedChunks.stream()
                .map(documentMapper::toDocumentChunkResponse)
                .toList();
    }

    @Override
    public List<DocumentChunkResponse> getDocumentChunks(UUID documentId) {
        return getDocumentChunks(documentId, null);
    }

    @Override
    public List<DocumentChunkResponse> getDocumentChunks(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        return chunks.stream()
                .map(documentMapper::toDocumentChunkResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<DocumentChunkResponse> generateAndSaveEmbeddings(UUID documentId) {
        return generateAndSaveEmbeddings(documentId, null);
    }

    @Override
    @Transactional
    public List<DocumentChunkResponse> generateAndSaveEmbeddings(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);

        if (document.getStatus() != DocumentStatus.READY) {
            throw new IllegalStateException("Document is not ready for embedding generation. Current status: " + document.getStatus());
        }

        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            throw new ChunkingException("Document has no text chunks available for embedding generation");
        }

        log.info("Generating embeddings for {} chunks of document {}", chunks.size(), documentId);
        for (DocumentChunk chunk : chunks) {
            List<Float> vector = embeddingService.generateEmbedding(
                    chunk.getContent(),
                    com.legalassist.service.embedding.EmbeddingTaskType.RETRIEVAL_DOCUMENT
            );
            String formattedVector = com.legalassist.service.embedding.VectorUtils.formatPgVector(vector);
            chunk.setEmbedding(formattedVector);
            try {
                documentChunkRepository.updateEmbedding(chunk.getId(), formattedVector);
            } catch (Exception e) {
                log.debug("Native pgvector update fallback to JPA entity state for chunk {}: {}", chunk.getId(), e.getMessage());
            }
        }

        documentChunkRepository.saveAll(chunks);
        log.info("Successfully persisted embeddings for {} chunks of document {}", chunks.size(), documentId);

        return chunks.stream()
                .map(documentMapper::toDocumentChunkResponse)
                .toList();
    }

    @Override
    public byte[] downloadDocumentFile(UUID documentId) {
        return downloadDocumentFile(documentId, null);
    }

    @Override
    public byte[] downloadDocumentFile(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);
        return storageService.downloadFile(document.getStoragePath());
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        deleteDocument(documentId, null);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);

        documentChunkRepository.deleteByDocumentId(documentId);
        documentPageRepository.deleteByDocumentId(documentId);

        try {
            storageService.deleteFile(document.getStoragePath());
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to delete storage file at path {} during document deletion: {}", document.getStoragePath(), e.getMessage());
        }

        documentRepository.delete(document);
        log.info("Successfully deleted document {} and all associated pages, chunks, embeddings, and storage files", documentId);
    }

    @Override
    @Transactional
    public DocumentResponse replaceDocument(UUID documentId, MultipartFile file) {
        return replaceDocument(documentId, file, null);
    }

    @Override
    @Transactional
    public DocumentResponse replaceDocument(UUID documentId, MultipartFile file, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        validateDocumentOwnership(document, userId);
        validatePdfFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read replacement file content", e);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_document.pdf";
        } else {
            int lastSep = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
            if (lastSep >= 0) {
                originalFilename = originalFilename.substring(lastSep + 1);
            }
        }

        // 1. Delete old chunks and pages from DB
        documentChunkRepository.deleteByDocumentId(documentId);
        documentPageRepository.deleteByDocumentId(documentId);

        // 2. Clean up old file from storage
        try {
            storageService.deleteFile(document.getStoragePath());
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to delete old storage file at path {}: {}", document.getStoragePath(), e.getMessage());
        }

        // 3. Upload new PDF
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String safeStorageKey = String.format("%s/%s-%s",
                document.getUserId() != null ? document.getUserId().toString() : "anonymous",
                documentId,
                safeFilename
        );
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/pdf";
        }

        String newStoragePath = storageService.uploadFile(safeStorageKey, bytes, contentType);

        // 4. Update document metadata
        Instant now = Instant.now();
        document.setFilename(originalFilename);
        document.setDocumentType(contentType);
        document.setStoragePath(newStoragePath);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setUpdatedAt(now);
        Document updatedDocument = documentRepository.save(document);

        log.info("Successfully uploaded replacement file for document {}. Re-processing RAG pipeline...", documentId);

        // 5. Re-process text extraction, chunking, and embeddings for replacement document
        try {
            extractAndSaveDocumentText(documentId, userId);
            chunkAndSaveDocument(documentId, userId);
            generateAndSaveEmbeddings(documentId, userId);
        } catch (Exception e) {
            log.error("Failed to automatically re-process replacement document {}: {}", documentId, e.getMessage(), e);
        }

        Document reprocessedDoc = documentRepository.findById(documentId).orElse(updatedDocument);
        return documentMapper.toDocumentResponse(reprocessedDoc);
    }

    private void validateDocumentOwnership(Document document, UUID userId) {
        if (userId != null && document != null && document.getUserId() != null && !document.getUserId().equals(userId)) {
            log.warn("User ID {} attempted unauthorized access to document {}", userId, document.getId());
            throw new com.legalassist.exception.AccessDeniedException("Access denied: Document does not belong to user");
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file must be provided and non-empty");
        }

        long maxSizeBytes = 20 * 1024 * 1024L; // 20 MB max size
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Uploaded file exceeds maximum allowed limit of 20 MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            if (!originalFilename.toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("Only PDF files (.pdf) are allowed");
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file content", e);
        }

        if (bytes.length < 4 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            throw new IllegalArgumentException("Invalid file format. The file is not a valid PDF document (missing %PDF header signature)");
        }
    }
}
