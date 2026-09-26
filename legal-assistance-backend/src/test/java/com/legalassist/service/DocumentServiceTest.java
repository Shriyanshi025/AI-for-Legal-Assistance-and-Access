package com.legalassist.service;

import com.legalassist.dto.DocumentPageResponse;
import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentPage;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.exception.PdfExtractionException;
import com.legalassist.exception.StorageException;
import com.legalassist.repository.DocumentPageRepository;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.service.pdf.ExtractedPage;
import com.legalassist.service.pdf.PdfExtractionResult;
import com.legalassist.service.pdf.PdfTextExtractionService;
import com.legalassist.service.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentPageRepository documentPageRepository;

    @Mock
    private com.legalassist.repository.DocumentChunkRepository documentChunkRepository;

    @Mock
    private com.legalassist.service.chunking.TextChunkingService textChunkingService;

    @Mock
    private com.legalassist.config.ChunkingProperties chunkingProperties;

    @Mock
    private com.legalassist.service.embedding.EmbeddingService embeddingService;

    @Spy
    private DocumentMapper documentMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private PdfTextExtractionService pdfTextExtractionService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    @DisplayName("getDocument should return DocumentResponse when document exists")
    void getDocumentShouldReturnDocumentResponseWhenFound() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(
                docId,
                userId,
                "employment_agreement.pdf",
                "application/pdf",
                "/storage/docs/employment_agreement.pdf",
                102400L,
                DocumentStatus.READY,
                now,
                now
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        DocumentResponse response = documentService.getDocument(docId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(docId);
        assertThat(response.filename()).isEqualTo("employment_agreement.pdf");
        assertThat(response.status()).isEqualTo(DocumentStatus.READY);
        verify(documentRepository).findById(docId);
    }

    @Test
    @DisplayName("getDocument should throw DocumentNotFoundException when document does not exist")
    void getDocumentShouldThrowExceptionWhenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(documentRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(missingId))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(missingId.toString());

        verify(documentRepository).findById(missingId);
    }

    @Test
    @DisplayName("getUserDocuments should return mapped list of DocumentSummaryResponse")
    void getUserDocumentsShouldReturnSummaryList() {
        UUID userId = UUID.randomUUID();
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc1 = new Document(doc1Id, userId, "doc1.pdf", "application/pdf", "/storage/doc1.pdf", 1000L, DocumentStatus.READY, now, now);
        Document doc2 = new Document(doc2Id, userId, "doc2.pdf", "application/pdf", "/storage/doc2.pdf", 2000L, DocumentStatus.PROCESSING, now, now);

        when(documentRepository.findByUserId(userId)).thenReturn(List.of(doc1, doc2));

        List<DocumentSummaryResponse> result = documentService.getUserDocuments(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(doc1Id);
        assertThat(result.get(0).filename()).isEqualTo("doc1.pdf");
        assertThat(result.get(1).id()).isEqualTo(doc2Id);
        assertThat(result.get(1).filename()).isEqualTo("doc2.pdf");
        verify(documentRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("getUserDocuments should return empty list when user has no documents")
    void getUserDocumentsShouldReturnEmptyListWhenNoDocumentsFound() {
        UUID userId = UUID.randomUUID();
        when(documentRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<DocumentSummaryResponse> result = documentService.getUserDocuments(userId);

        assertThat(result).isEmpty();
        verify(documentRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("uploadDocument should upload file to storage and persist document metadata")
    void uploadDocumentShouldStoreFileAndPersistMetadata() {
        UUID userId = UUID.randomUUID();
        byte[] pdfBytes = "%PDF-1.4 sample pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_contract.pdf",
                "application/pdf",
                pdfBytes
        );

        when(storageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.uploadDocument(file, userId);

        assertThat(response).isNotNull();
        assertThat(response.filename()).isEqualTo("test_contract.pdf");
        assertThat(response.documentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(pdfBytes.length);
        assertThat(response.status()).isEqualTo(DocumentStatus.UPLOADED);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document capturedDoc = docCaptor.getValue();
        assertThat(capturedDoc.getUserId()).isEqualTo(userId);
        assertThat(capturedDoc.getStoragePath()).contains(userId.toString());
        assertThat(capturedDoc.getStoragePath()).doesNotContain("..");
    }

    @Test
    @DisplayName("uploadDocument should throw IllegalArgumentException when file is empty or missing")
    void uploadDocumentShouldThrowWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> documentService.uploadDocument(emptyFile, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provided and non-empty");

        assertThatThrownBy(() -> documentService.uploadDocument(null, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provided and non-empty");

        verify(storageService, never()).uploadFile(anyString(), any(), anyString());
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadDocument should not persist database entity when storage upload fails")
    void uploadDocumentShouldNotPersistEntityWhenStorageFails() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "%PDF-1.4 data".getBytes());

        when(storageService.uploadFile(anyString(), any(byte[].class), anyString()))
                .thenThrow(new StorageException("Storage error"));

        assertThatThrownBy(() -> documentService.uploadDocument(file, userId))
                .isInstanceOf(StorageException.class);

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadDocument should cleanup storage file if database save fails after upload")
    void uploadDocumentShouldCleanupStorageIfDbSaveFails() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "%PDF-1.4 data".getBytes());

        when(storageService.uploadFile(anyString(), any(byte[].class), anyString()))
                .thenReturn("user/123-contract.pdf");

        when(documentRepository.save(any(Document.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> documentService.uploadDocument(file, userId))
                .isInstanceOf(RuntimeException.class);

        verify(storageService).deleteFile("user/123-contract.pdf");
    }

    @Test
    @DisplayName("extractAndSaveDocumentText should extract text from storage PDF and persist DocumentPages")
    void extractAndSaveDocumentTextSuccess() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = new Document(
                docId, userId, "lease.pdf", "application/pdf",
                "storage/path/lease.pdf", 1024L, DocumentStatus.UPLOADED, now, now
        );

        byte[] pdfBytes = "%PDF-1.4 sample content".getBytes();
        PdfExtractionResult extractionResult = new PdfExtractionResult(
                2,
                List.of(
                        new ExtractedPage(1, "Text on page 1"),
                        new ExtractedPage(2, "Text on page 2")
                ),
                "Text on page 1\n\nText on page 2",
                true
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(storageService.downloadFile("storage/path/lease.pdf")).thenReturn(pdfBytes);
        when(pdfTextExtractionService.extractText(pdfBytes)).thenReturn(extractionResult);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.extractAndSaveDocumentText(docId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(DocumentStatus.PROCESSING);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentPage>> pagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentPageRepository).saveAll(pagesCaptor.capture());
        List<DocumentPage> savedPages = pagesCaptor.getValue();
        assertThat(savedPages).hasSize(2);
        assertThat(savedPages.get(0).getPageNumber()).isEqualTo(1);
        assertThat(savedPages.get(0).getContent()).isEqualTo("Text on page 1");
    }

    @Test
    @DisplayName("extractAndSaveDocumentText should throw PdfExtractionException when document is not a PDF")
    void extractAndSaveDocumentTextShouldThrowNonPdf() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                docId, UUID.randomUUID(), "image.png", "image/png",
                "storage/path/image.png", 500L, DocumentStatus.UPLOADED, Instant.now(), Instant.now()
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.extractAndSaveDocumentText(docId))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("not a PDF file");

        verify(storageService, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("getDocumentPages should return mapped DocumentPageResponse list")
    void getDocumentPagesShouldReturnMappedPages() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = new Document(
                docId, userId, "lease.pdf", "application/pdf",
                "storage/path/lease.pdf", 1024L, DocumentStatus.READY, now, now
        );

        DocumentPage page1 = new DocumentPage(UUID.randomUUID(), docId, 1, "Page 1 Content", now);
        DocumentPage page2 = new DocumentPage(UUID.randomUUID(), docId, 2, "Page 2 Content", now);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentPageRepository.findByDocumentIdOrderByPageNumberAsc(docId)).thenReturn(List.of(page1, page2));

        List<DocumentPageResponse> result = documentService.getDocumentPages(docId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).pageNumber()).isEqualTo(1);
        assertThat(result.get(0).content()).isEqualTo("Page 1 Content");
        assertThat(result.get(1).pageNumber()).isEqualTo(2);
        assertThat(result.get(1).content()).isEqualTo("Page 2 Content");
    }

    @Test
    @DisplayName("chunkAndSaveDocument should generate chunks, delete existing chunks, and save new chunks")
    void chunkAndSaveDocumentSuccess() {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.PROCESSING, now, now
        );

        DocumentPage page1 = new DocumentPage(UUID.randomUUID(), docId, 1, "Page 1 content text", now);
        com.legalassist.service.chunking.RawChunk rawChunk1 = new com.legalassist.service.chunking.RawChunk(0, 1, "Page 1 content text", null, null);

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentPageRepository.findByDocumentIdOrderByPageNumberAsc(docId)).thenReturn(List.of(page1));
        when(chunkingProperties.maxChunkSize()).thenReturn(1500);
        when(chunkingProperties.overlapSize()).thenReturn(200);
        when(textChunkingService.createChunks(List.of(page1), 1500, 200)).thenReturn(List.of(rawChunk1));
        when(documentChunkRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<com.legalassist.dto.DocumentChunkResponse> response = documentService.chunkAndSaveDocument(docId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).documentId()).isEqualTo(docId);
        verify(documentChunkRepository).deleteByDocumentId(docId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.legalassist.entity.DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunksCaptor.capture());
        List<com.legalassist.entity.DocumentChunk> savedChunks = chunksCaptor.getValue();
        assertThat(savedChunks).hasSize(1);
        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(savedChunks.get(0).getPageNumber()).isEqualTo(1);
        assertThat(savedChunks.get(0).getContent()).isEqualTo("Page 1 content text");
    }

    @Test
    @DisplayName("chunkAndSaveDocument should throw IllegalStateException when document status is UPLOADED")
    void chunkAndSaveDocumentShouldThrowWhenNotReady() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.UPLOADED, Instant.now(), Instant.now()
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.chunkAndSaveDocument(docId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready");

        verify(documentChunkRepository, never()).deleteByDocumentId(any());
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("chunkAndSaveDocument should return empty list when document pages contain no text")
    void chunkAndSaveDocumentShouldReturnEmptyListWhenNoText() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.PROCESSING, Instant.now(), Instant.now()
        );

        DocumentPage emptyPage = new DocumentPage(UUID.randomUUID(), docId, 1, "   ", Instant.now());

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentPageRepository.findByDocumentIdOrderByPageNumberAsc(docId)).thenReturn(List.of(emptyPage));
        when(chunkingProperties.maxChunkSize()).thenReturn(1500);
        when(chunkingProperties.overlapSize()).thenReturn(200);
        when(textChunkingService.createChunks(List.of(emptyPage), 1500, 200)).thenReturn(Collections.emptyList());

        List<com.legalassist.dto.DocumentChunkResponse> result = documentService.chunkAndSaveDocument(docId);

        assertThat(result).isEmpty();
        verify(documentChunkRepository, never()).deleteByDocumentId(any());
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getDocumentChunks should return mapped list of DocumentChunkResponse")
    void getDocumentChunksShouldReturnMappedChunks() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = new Document(
                docId, userId, "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.READY, now, now
        );

        com.legalassist.entity.DocumentChunk chunk1 = new com.legalassist.entity.DocumentChunk(
                UUID.randomUUID(), docId, 1, null, null, "Chunk 1 content", 0
        );
        com.legalassist.entity.DocumentChunk chunk2 = new com.legalassist.entity.DocumentChunk(
                UUID.randomUUID(), docId, 1, null, null, "Chunk 2 content", 1
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId)).thenReturn(List.of(chunk1, chunk2));

        List<com.legalassist.dto.DocumentChunkResponse> result = documentService.getDocumentChunks(docId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).chunkIndex()).isEqualTo(0);
        assertThat(result.get(0).content()).isEqualTo("Chunk 1 content");
        assertThat(result.get(1).chunkIndex()).isEqualTo(1);
        assertThat(result.get(1).content()).isEqualTo("Chunk 2 content");
    }

    @Test
    @DisplayName("generateAndSaveEmbeddings should generate RETRIEVAL_DOCUMENT embeddings, save chunks, and mark document READY")
    void generateAndSaveEmbeddingsSuccess() {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.PROCESSING, now, now
        );

        com.legalassist.entity.DocumentChunk chunk1 = new com.legalassist.entity.DocumentChunk(
                UUID.randomUUID(), docId, 1, null, null, "Clause content 1", 0
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId)).thenReturn(List.of(chunk1));
        when(embeddingService.generateEmbedding(eq("Clause content 1"), eq(com.legalassist.service.embedding.EmbeddingTaskType.RETRIEVAL_DOCUMENT)))
                .thenReturn(List.of(0.6f, 0.8f));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        List<com.legalassist.dto.DocumentChunkResponse> response = documentService.generateAndSaveEmbeddings(docId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).documentId()).isEqualTo(docId);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
        verify(embeddingService).generateEmbedding("Clause content 1", com.legalassist.service.embedding.EmbeddingTaskType.RETRIEVAL_DOCUMENT);
        verify(documentChunkRepository).saveAll(any());
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("generateAndSaveEmbeddings should throw IllegalStateException when document status is FAILED")
    void generateAndSaveEmbeddingsShouldThrowWhenInvalidStatus() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.FAILED, Instant.now(), Instant.now()
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.generateAndSaveEmbeddings(docId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ready");

        verify(embeddingService, never()).generateEmbedding(anyString(), any());
    }

    @Test
    @DisplayName("generateAndSaveEmbeddings should set status to FAILED when embedding service throws exception")
    void generateAndSaveEmbeddingsFailureSetsStatusFailed() {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        Document document = new Document(
                docId, UUID.randomUUID(), "contract.pdf", "application/pdf",
                "storage/path/contract.pdf", 2048L, DocumentStatus.PROCESSING, now, now
        );

        com.legalassist.entity.DocumentChunk chunk1 = new com.legalassist.entity.DocumentChunk(
                UUID.randomUUID(), docId, 1, null, null, "Clause content 1", 0
        );

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(docId)).thenReturn(List.of(chunk1));
        when(embeddingService.generateEmbedding(anyString(), any()))
                .thenThrow(new com.legalassist.exception.EmbeddingException("AI service failed"));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> documentService.generateAndSaveEmbeddings(docId))
                .isInstanceOf(com.legalassist.exception.EmbeddingException.class);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("getDocument should throw AccessDeniedException when requesting user does not own document [A]")
    void getDocumentShouldThrowAccessDeniedForOtherUser() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(docId, ownerId, "secret.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.getDocument(docId, attackerId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("deleteDocument should throw AccessDeniedException when requesting user does not own document [B]")
    void deleteDocumentShouldThrowAccessDeniedForOtherUser() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(docId, ownerId, "secret.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.deleteDocument(docId, attackerId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class);
    }

    @Test
    @DisplayName("replaceDocument should throw AccessDeniedException when requesting user does not own document [C]")
    void replaceDocumentShouldThrowAccessDeniedForOtherUser() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(docId, ownerId, "secret.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        MockMultipartFile pdfFile = new MockMultipartFile("file", "new.pdf", "application/pdf", "%PDF-1.4 sample".getBytes());

        assertThatThrownBy(() -> documentService.replaceDocument(docId, pdfFile, attackerId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class);
    }

    @Test
    @DisplayName("downloadDocumentFile should throw AccessDeniedException when requesting user does not own document [D]")
    void downloadDocumentFileShouldThrowAccessDeniedForOtherUser() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(docId, ownerId, "secret.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.downloadDocumentFile(docId, attackerId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class);
    }

    @Test
    @DisplayName("getDocumentChunks should throw AccessDeniedException when requesting user does not own document [E]")
    void getDocumentChunksShouldThrowAccessDeniedForOtherUser() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(docId, ownerId, "secret.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.getDocumentChunks(docId, attackerId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class);
    }

    @Test
    @DisplayName("uploadDocument should reject non-PDF file extension [J]")
    void uploadDocumentShouldRejectNonPdfExtension() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile txtFile = new MockMultipartFile("file", "malicious.exe", "application/pdf", "%PDF-1.4 data".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(txtFile, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF files (.pdf) are allowed");
    }

    @Test
    @DisplayName("uploadDocument should reject invalid PDF magic header bytes [K]")
    void uploadDocumentShouldRejectInvalidMagicHeader() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile fakePdf = new MockMultipartFile("file", "fake.pdf", "application/pdf", "NOT_A_PDF_CONTENT".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(fakePdf, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing %PDF header signature");
    }

    @Test
    @DisplayName("uploadDocument should reject file exceeding 20 MB [L]")
    void uploadDocumentShouldRejectOversizedFile() {
        UUID userId = UUID.randomUUID();
        byte[] largeBytes = new byte[21 * 1024 * 1024]; // 21 MB
        System.arraycopy("%PDF-1.4".getBytes(), 0, largeBytes, 0, 8);
        MockMultipartFile largeFile = new MockMultipartFile("file", "huge.pdf", "application/pdf", largeBytes);

        assertThatThrownBy(() -> documentService.uploadDocument(largeFile, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed limit of 20 MB");
    }

    @Test
    @DisplayName("uploadDocument should succeed for valid PDF with %PDF header [M]")
    void uploadDocumentShouldSucceedForValidPdfHeader() {
        UUID userId = UUID.randomUUID();
        byte[] validPdfContent = "%PDF-1.4 valid content".getBytes();
        MockMultipartFile validPdf = new MockMultipartFile("file", "valid.pdf", "application/pdf", validPdfContent);

        when(storageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf"))).thenReturn("path/valid.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.uploadDocument(validPdf, userId);

        assertThat(response).isNotNull();
        assertThat(response.filename()).isEqualTo("valid.pdf");
    }
}
