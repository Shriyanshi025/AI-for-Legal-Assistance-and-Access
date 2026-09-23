package com.legalassist.service.chunking;

import com.legalassist.entity.DocumentPage;
import com.legalassist.exception.ChunkingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkingServiceTest {

    private TextChunkingService textChunkingService;

    @BeforeEach
    void setUp() {
        textChunkingService = new TextChunkingServiceImpl();
    }

    @Test
    @DisplayName("createChunks should return single chunk when page content is smaller than maxChunkSize")
    void shortContentProducesSingleChunk() {
        UUID docId = UUID.randomUUID();
        DocumentPage page = new DocumentPage(UUID.randomUUID(), docId, 1, "Short legal clause text.", Instant.now());

        List<RawChunk> chunks = textChunkingService.createChunks(List.of(page), 100, 20);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).content()).isEqualTo("Short legal clause text.");
    }

    @Test
    @DisplayName("createChunks should produce multiple chunks and preserve overlap for long text")
    void longContentProducesMultipleChunksWithOverlap() {
        UUID docId = UUID.randomUUID();
        String p1 = "First paragraph containing legal definition of indemnification and liabilities under clause 1.";
        String p2 = "Second paragraph outlining termination requirements, notice period, and jurisdiction terms.";
        String p3 = "Third paragraph providing governing law provisions and dispute resolution mechanisms.";
        String fullText = p1 + "\n\n" + p2 + "\n\n" + p3;

        DocumentPage page = new DocumentPage(UUID.randomUUID(), docId, 1, fullText, Instant.now());

        List<RawChunk> chunks = textChunkingService.createChunks(List.of(page), 120, 25);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(0);
        assertThat(chunks.get(1).chunkIndex()).isEqualTo(1);

        // Verify deterministic output on re-running
        List<RawChunk> reChunks = textChunkingService.createChunks(List.of(page), 120, 25);
        assertThat(reChunks).isEqualTo(chunks);
    }

    @Test
    @DisplayName("createChunks should prefer paragraph and line boundaries over hard truncation")
    void chunkingPrefersNaturalBoundaries() {
        UUID docId = UUID.randomUUID();
        String paragraph1 = "Section 1: Definitions and Scope of Agreement.";
        String paragraph2 = "Section 2: Intellectual Property Assignment and Licensing Conditions.";
        String fullText = paragraph1 + "\n\n" + paragraph2;

        DocumentPage page = new DocumentPage(UUID.randomUUID(), docId, 1, fullText, Instant.now());

        List<RawChunk> chunks = textChunkingService.createChunks(List.of(page), 80, 0);

        assertThat(chunks.get(0).content()).isEqualTo("Section 1: Definitions and Scope of Agreement.");
        assertThat(chunks.get(1).content()).isEqualTo("Section 2: Intellectual Property Assignment and Licensing Conditions.");
    }

    @Test
    @DisplayName("createChunks should preserve page provenance for multi-page documents")
    void chunkingPreservesPageProvenance() {
        UUID docId = UUID.randomUUID();
        DocumentPage page1 = new DocumentPage(UUID.randomUUID(), docId, 1, "Page 1 Content", Instant.now());
        DocumentPage page2 = new DocumentPage(UUID.randomUUID(), docId, 2, "Page 2 Content", Instant.now());

        List<RawChunk> chunks = textChunkingService.createChunks(List.of(page1, page2), 100, 10);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(chunks.get(0).content()).isEqualTo("Page 1 Content");
        assertThat(chunks.get(1).pageNumber()).isEqualTo(2);
        assertThat(chunks.get(1).content()).isEqualTo("Page 2 Content");
    }

    @Test
    @DisplayName("createChunks should produce no chunks for empty or whitespace-only pages")
    void emptyPagesProduceNoChunks() {
        UUID docId = UUID.randomUUID();
        DocumentPage emptyPage = new DocumentPage(UUID.randomUUID(), docId, 1, "   \n\n  ", Instant.now());

        List<RawChunk> chunks = textChunkingService.createChunks(List.of(emptyPage), 100, 10);

        assertThat(chunks).isEmpty();
        assertThat(textChunkingService.createChunks(Collections.emptyList(), 100, 10)).isEmpty();
    }

    @Test
    @DisplayName("createChunks should throw ChunkingException when overlap is equal to or greater than maxChunkSize")
    void invalidOverlapThrowsChunkingException() {
        UUID docId = UUID.randomUUID();
        DocumentPage page = new DocumentPage(UUID.randomUUID(), docId, 1, "Content", Instant.now());

        assertThatThrownBy(() -> textChunkingService.createChunks(List.of(page), 100, 100))
                .isInstanceOf(ChunkingException.class)
                .hasMessageContaining("strictly smaller");

        assertThatThrownBy(() -> textChunkingService.createChunks(List.of(page), 100, 150))
                .isInstanceOf(ChunkingException.class)
                .hasMessageContaining("strictly smaller");
    }
}
