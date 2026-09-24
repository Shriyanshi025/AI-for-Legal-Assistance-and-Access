package com.legalassist.controller;

import com.legalassist.dto.DocumentPageResponse;
import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.exception.GlobalExceptionHandler;
import com.legalassist.exception.PdfExtractionException;
import com.legalassist.exception.StorageException;
import com.legalassist.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private com.legalassist.service.search.SemanticSearchService semanticSearchService;

    @MockitoBean
    private com.legalassist.service.rag.LegalQaService legalQaService;


    @Test
    @DisplayName("GET /api/documents/{id} should return 200 and DocumentResponse when document exists")
    void getDocumentByIdShouldReturnDocumentResponse() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-20T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-20T10:05:00Z");

        DocumentResponse response = new DocumentResponse(
                docId,
                "employment_contract.pdf",
                "application/pdf",
                102400L,
                DocumentStatus.READY,
                createdAt,
                updatedAt
        );

        when(documentService.getDocument(docId)).thenReturn(response);

        mockMvc.perform(get("/api/documents/{id}", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("employment_contract.pdf"))
                .andExpect(jsonPath("$.documentType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSize").value(102400))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-20T10:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-20T10:05:00Z"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.storagePath").doesNotExist());

        verify(documentService).getDocument(docId);
    }

    @Test
    @DisplayName("GET /api/documents/{id} should return 404 ApiErrorResponse when document does not exist")
    void getDocumentByIdShouldReturn404WhenNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(documentService.getDocument(missingId)).thenThrow(new DocumentNotFoundException(missingId));

        mockMvc.perform(get("/api/documents/{id}", missingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Document not found with id: " + missingId))
                .andExpect(jsonPath("$.path").value("/api/documents/" + missingId));

        verify(documentService).getDocument(missingId);
    }

    @Test
    @DisplayName("GET /api/documents?userId={userId} should return 200 and list of DocumentSummaryResponse")
    void getUserDocumentsShouldReturnDocumentSummaryList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-20T10:00:00Z");

        DocumentSummaryResponse doc1 = new DocumentSummaryResponse(
                doc1Id,
                "lease_agreement.pdf",
                "application/pdf",
                204800L,
                DocumentStatus.READY,
                createdAt
        );
        DocumentSummaryResponse doc2 = new DocumentSummaryResponse(
                doc2Id,
                "nda.pdf",
                "application/pdf",
                51200L,
                DocumentStatus.PROCESSING,
                createdAt
        );

        when(documentService.getUserDocuments(userId)).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/documents")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(doc1Id.toString()))
                .andExpect(jsonPath("$[0].filename").value("lease_agreement.pdf"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[1].id").value(doc2Id.toString()))
                .andExpect(jsonPath("$[1].filename").value("nda.pdf"))
                .andExpect(jsonPath("$[1].status").value("PROCESSING"));

        verify(documentService).getUserDocuments(userId);
    }

    @Test
    @DisplayName("GET /api/documents?userId={userId} should return 200 and empty list when user has no documents")
    void getUserDocumentsShouldReturnEmptyListWhenNoDocumentsFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(documentService.getUserDocuments(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/documents")
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(documentService).getUserDocuments(userId);
    }

    @Test
    @DisplayName("GET /api/documents/{id} with invalid UUID syntax should return non-200 status")
    void getDocumentByInvalidUuidShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/documents/not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/documents?userId={userId} with invalid UUID syntax should return non-200 status")
    void getUserDocumentsWithInvalidUuidShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .param("userId", "not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/documents should return 201 Created and DocumentResponse when upload succeeds")
    void uploadDocumentShouldReturn201CreatedOnSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                "application/pdf",
                "pdf content bytes".getBytes()
        );

        DocumentResponse response = new DocumentResponse(
                docId,
                "contract.pdf",
                "application/pdf",
                (long) "pdf content bytes".getBytes().length,
                DocumentStatus.UPLOADED,
                now,
                now
        );

        when(documentService.uploadDocument(any(), eq(userId))).thenReturn(response);

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("userId", userId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("contract.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    @DisplayName("POST /api/documents should return 400 Bad Request when file upload fails validation")
    void uploadDocumentShouldReturn400WhenFileIsEmpty() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        when(documentService.uploadDocument(any(), eq(userId)))
                .thenThrow(new IllegalArgumentException("Document file must be provided and non-empty"));

        mockMvc.perform(multipart("/api/documents")
                        .file(emptyFile)
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Document file must be provided and non-empty"));
    }

    @Test
    @DisplayName("POST /api/documents should return 500 Internal Server Error when storage upload fails")
    void uploadDocumentShouldReturn500WhenStorageFails() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());

        when(documentService.uploadDocument(any(), eq(userId)))
                .thenThrow(new StorageException("Storage error"));

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("userId", userId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Storage service error occurred"));
    }

    @Test
    @DisplayName("POST /api/documents/{id}/extract should return 200 OK and updated DocumentResponse on success")
    void extractDocumentTextShouldReturn200OK() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentResponse response = new DocumentResponse(
                docId,
                "contract.pdf",
                "application/pdf",
                2048L,
                DocumentStatus.READY,
                now,
                now
        );

        when(documentService.extractAndSaveDocumentText(docId)).thenReturn(response);

        mockMvc.perform(post("/api/documents/{id}/extract", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.status").value("READY"));

        verify(documentService).extractAndSaveDocumentText(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/extract should return 400 Bad Request on PdfExtractionException")
    void extractDocumentTextShouldReturn400OnExtractionFailure() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.extractAndSaveDocumentText(docId))
                .thenThrow(new PdfExtractionException("Document is not a valid PDF file"));

        mockMvc.perform(post("/api/documents/{id}/extract", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Document is not a valid PDF file"));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/pages should return 200 OK and list of DocumentPageResponse")
    void getDocumentPagesShouldReturnPagesList() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentPageResponse page1 = new DocumentPageResponse(UUID.randomUUID(), docId, 1, "Page 1 text", now);
        DocumentPageResponse page2 = new DocumentPageResponse(UUID.randomUUID(), docId, 2, "Page 2 text", now);

        when(documentService.getDocumentPages(docId)).thenReturn(List.of(page1, page2));

        mockMvc.perform(get("/api/documents/{id}/pages", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pageNumber").value(1))
                .andExpect(jsonPath("$[0].content").value("Page 1 text"))
                .andExpect(jsonPath("$[1].pageNumber").value(2))
                .andExpect(jsonPath("$[1].content").value("Page 2 text"));

        verify(documentService).getDocumentPages(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/chunks should return 200 OK and list of DocumentChunkResponse on success")
    void chunkDocumentShouldReturn200OK() throws Exception {
        UUID docId = UUID.randomUUID();

        com.legalassist.dto.DocumentChunkResponse chunk1 = new com.legalassist.dto.DocumentChunkResponse(
                UUID.randomUUID(), docId, 1, null, null, "Chunk 1 content", 0
        );

        when(documentService.chunkAndSaveDocument(docId)).thenReturn(List.of(chunk1));

        mockMvc.perform(post("/api/documents/{id}/chunks", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Chunk 1 content"));

        verify(documentService).chunkAndSaveDocument(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/chunks should return 400 Bad Request on ChunkingException")
    void chunkDocumentShouldReturn400OnChunkingException() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.chunkAndSaveDocument(docId))
                .thenThrow(new com.legalassist.exception.ChunkingException("Document contains no text content to chunk"));

        mockMvc.perform(post("/api/documents/{id}/chunks", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Document contains no text content to chunk"));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/chunks should return 200 OK and list of DocumentChunkResponse")
    void getDocumentChunksShouldReturnChunksList() throws Exception {
        UUID docId = UUID.randomUUID();

        com.legalassist.dto.DocumentChunkResponse chunk1 = new com.legalassist.dto.DocumentChunkResponse(
                UUID.randomUUID(), docId, 1, null, null, "Chunk 1 content", 0
        );
        com.legalassist.dto.DocumentChunkResponse chunk2 = new com.legalassist.dto.DocumentChunkResponse(
                UUID.randomUUID(), docId, 1, null, null, "Chunk 2 content", 1
        );

        when(documentService.getDocumentChunks(docId)).thenReturn(List.of(chunk1, chunk2));

        mockMvc.perform(get("/api/documents/{id}/chunks", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[0].content").value("Chunk 1 content"))
                .andExpect(jsonPath("$[1].chunkIndex").value(1))
                .andExpect(jsonPath("$[1].content").value("Chunk 2 content"));

        verify(documentService).getDocumentChunks(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/embeddings should return 200 OK and list of DocumentChunkResponse")
    void generateEmbeddingsShouldReturn200OK() throws Exception {
        UUID docId = UUID.randomUUID();

        com.legalassist.dto.DocumentChunkResponse chunk1 = new com.legalassist.dto.DocumentChunkResponse(
                UUID.randomUUID(), docId, 1, null, null, "Chunk content", 0
        );

        when(documentService.generateAndSaveEmbeddings(docId)).thenReturn(List.of(chunk1));

        mockMvc.perform(post("/api/documents/{id}/embeddings", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Chunk content"));

        verify(documentService).generateAndSaveEmbeddings(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/search should return 200 OK and list of SimilaritySearchResultResponse")
    void searchSimilarChunksShouldReturn200OK() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        com.legalassist.dto.SimilaritySearchResultResponse resultItem = new com.legalassist.dto.SimilaritySearchResultResponse(
                chunkId, docId, 2, 0, "Termination clause text", "Section 4", "Clause A", 0.94
        );

        when(semanticSearchService.searchSimilarChunks(eq(docId), eq("termination clause"), eq(3)))
                .thenReturn(List.of(resultItem));

        String jsonBody = """
                {
                    "query": "termination clause",
                    "topK": 3
                }
                """;

        mockMvc.perform(post("/api/documents/{id}/search", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chunkId").value(chunkId.toString()))
                .andExpect(jsonPath("$[0].similarityScore").value(0.94))
                .andExpect(jsonPath("$[0].content").value("Termination clause text"))
                .andExpect(jsonPath("$[0].embedding").doesNotExist());

        verify(semanticSearchService).searchSimilarChunks(docId, "termination clause", 3);
    }

    @Test
    @DisplayName("GET /api/documents/{id}/view should return 200 OK and PDF bytes with inline header")
    void viewDocumentFileShouldReturnPdfBytes() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        DocumentResponse docResp = new DocumentResponse(
                docId, "test.pdf", "application/pdf", 100L, DocumentStatus.READY, now, now
        );

        when(documentService.getDocument(docId)).thenReturn(docResp);
        when(documentService.downloadDocumentFile(docId)).thenReturn("%PDF-1.4 test bytes".getBytes());

        mockMvc.perform(get("/api/documents/{id}/view", docId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"test.pdf\""
                ));

        verify(documentService).downloadDocumentFile(docId);
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} should return 24 No Content on successful deletion")
    void deleteDocumentShouldReturn204NoContent() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/documents/{id}", docId))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(docId);
    }

    @Test
    @DisplayName("POST /api/documents/{id}/replace should return 200 OK and updated DocumentResponse")
    void replaceDocumentShouldReturn200OK() throws Exception {
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();

        MockMultipartFile newFile = new MockMultipartFile(
                "file", "new_contract.pdf", "application/pdf", "new pdf bytes".getBytes()
        );

        DocumentResponse updatedResp = new DocumentResponse(
                docId, "new_contract.pdf", "application/pdf", 200L, DocumentStatus.READY, now, now
        );

        when(documentService.replaceDocument(eq(docId), any())).thenReturn(updatedResp);

        mockMvc.perform(multipart("/api/documents/{id}/replace", docId).file(newFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.filename").value("new_contract.pdf"));

        verify(documentService).replaceDocument(eq(docId), any());
    }
}
