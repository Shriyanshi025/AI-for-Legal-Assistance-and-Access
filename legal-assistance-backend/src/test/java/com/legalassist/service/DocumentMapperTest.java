package com.legalassist.service;

import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMapperTest {

    private DocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        documentMapper = new DocumentMapper();
    }

    @Test
    @DisplayName("toDocumentResponse should map Document entity to DocumentResponse without exposing sensitive storage or user fields")
    void shouldMapToDocumentResponse() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-20T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-20T10:05:00Z");

        Document document = new Document(
                docId,
                userId,
                "contract.pdf",
                "application/pdf",
                "/internal/storage/contract.pdf",
                204800L,
                DocumentStatus.READY,
                createdAt,
                updatedAt
        );

        DocumentResponse response = documentMapper.toDocumentResponse(document);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(docId);
        assertThat(response.filename()).isEqualTo("contract.pdf");
        assertThat(response.documentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(204800L);
        assertThat(response.status()).isEqualTo(DocumentStatus.READY);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("toDocumentSummaryResponse should map Document entity to concise DocumentSummaryResponse")
    void shouldMapToDocumentSummaryResponse() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-20T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-20T10:05:00Z");

        Document document = new Document(
                docId,
                userId,
                "nda.pdf",
                "application/pdf",
                "/internal/storage/nda.pdf",
                51200L,
                DocumentStatus.UPLOADED,
                createdAt,
                updatedAt
        );

        DocumentSummaryResponse summary = documentMapper.toDocumentSummaryResponse(document);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(docId);
        assertThat(summary.filename()).isEqualTo("nda.pdf");
        assertThat(summary.documentType()).isEqualTo("application/pdf");
        assertThat(summary.fileSize()).isEqualTo(51200L);
        assertThat(summary.status()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(summary.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Mapper methods should cleanly return null when passed null Document")
    void shouldReturnNullForNullInput() {
        assertThat(documentMapper.toDocumentResponse(null)).isNull();
        assertThat(documentMapper.toDocumentSummaryResponse(null)).isNull();
    }
}
