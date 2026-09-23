package com.legalassist.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEntityTest {

    @Test
    @DisplayName("Should correctly construct Document entity with valid fields")
    void shouldConstructDocumentWithValidFields() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        Document doc = new Document(
                docId,
                userId,
                "lease_agreement.pdf",
                "application/pdf",
                "/storage/docs/lease_agreement.pdf",
                1024576L,
                DocumentStatus.UPLOADED,
                now,
                now
        );

        assertThat(doc.getId()).isEqualTo(docId);
        assertThat(doc.getUserId()).isEqualTo(userId);
        assertThat(doc.getFilename()).isEqualTo("lease_agreement.pdf");
        assertThat(doc.getDocumentType()).isEqualTo("application/pdf");
        assertThat(doc.getStoragePath()).isEqualTo("/storage/docs/lease_agreement.pdf");
        assertThat(doc.getFileSize()).isEqualTo(1024576L);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(doc.getCreatedAt()).isEqualTo(now);
        assertThat(doc.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should contain expected DocumentStatus enum constants")
    void shouldContainExpectedDocumentStatusValues() {
        assertThat(DocumentStatus.values()).containsExactly(
                DocumentStatus.UPLOADED,
                DocumentStatus.PROCESSING,
                DocumentStatus.READY,
                DocumentStatus.FAILED
        );
        assertThat(DocumentStatus.valueOf("UPLOADED")).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(DocumentStatus.valueOf("PROCESSING")).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(DocumentStatus.valueOf("READY")).isEqualTo(DocumentStatus.READY);
        assertThat(DocumentStatus.valueOf("FAILED")).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("Should correctly construct DocumentPage entity representing a document page")
    void shouldConstructDocumentPageWithValidFields() {
        UUID pageId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentPage page = new DocumentPage(
                pageId,
                docId,
                1,
                "This is the content of page 1.",
                now
        );

        assertThat(page.getId()).isEqualTo(pageId);
        assertThat(page.getDocumentId()).isEqualTo(docId);
        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getContent()).isEqualTo("This is the content of page 1.");
        assertThat(page.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should correctly construct DocumentChunk entity with section and clause metadata")
    void shouldConstructDocumentChunkWithValidFields() {
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        DocumentChunk chunk = new DocumentChunk(
                chunkId,
                docId,
                2,
                "Section 4: Termination",
                "Clause 4.1",
                "Either party may terminate this agreement with 30 days written notice.",
                0
        );

        assertThat(chunk.getId()).isEqualTo(chunkId);
        assertThat(chunk.getDocumentId()).isEqualTo(docId);
        assertThat(chunk.getPageNumber()).isEqualTo(2);
        assertThat(chunk.getSection()).isEqualTo("Section 4: Termination");
        assertThat(chunk.getClause()).isEqualTo("Clause 4.1");
        assertThat(chunk.getContent()).isEqualTo("Either party may terminate this agreement with 30 days written notice.");
        assertThat(chunk.getChunkIndex()).isEqualTo(0);
    }
}
