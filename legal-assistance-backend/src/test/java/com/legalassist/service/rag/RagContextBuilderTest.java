package com.legalassist.service.rag;

import com.legalassist.dto.SimilaritySearchResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextBuilderTest {

    private RagContextBuilder contextBuilder;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        contextBuilder = new RagContextBuilder();
        documentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("buildContext should filter chunks below minSimilarity threshold")
    void buildContextShouldFilterLowSimilarity() {
        SimilaritySearchResultResponse highSim = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 1, "High similarity clause", "Sec 1", "Cl 1", 0.75
        );
        SimilaritySearchResultResponse lowSim = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 2, 2, "Low similarity clause", "Sec 2", "Cl 2", 0.20
        );

        RagContext context = contextBuilder.buildContext(List.of(highSim, lowSim), 0.35, 12000);

        assertThat(context.items()).hasSize(1);
        assertThat(context.items().get(0).sourceId()).isEqualTo("SRC-1");
        assertThat(context.items().get(0).content()).isEqualTo("High similarity clause");
    }

    @Test
    @DisplayName("buildContext should assign synthetic source IDs in order")
    void buildContextShouldAssignSourceIds() {
        SimilaritySearchResultResponse chunk1 = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 1, "First chunk text", "Sec 1", "Cl 1", 0.80
        );
        SimilaritySearchResultResponse chunk2 = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 2, "Second chunk text", "Sec 1", "Cl 2", 0.65
        );

        RagContext context = contextBuilder.buildContext(List.of(chunk1, chunk2), 0.35, 12000);

        assertThat(context.items()).hasSize(2);
        assertThat(context.items().get(0).sourceId()).isEqualTo("SRC-1");
        assertThat(context.items().get(1).sourceId()).isEqualTo("SRC-2");
    }

    @Test
    @DisplayName("buildContext should respect maxContextChars budget limit")
    void buildContextShouldEnforceCharLimit() {
        SimilaritySearchResultResponse chunk1 = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 1, "A".repeat(50), "Sec 1", "Cl 1", 0.80
        );
        SimilaritySearchResultResponse chunk2 = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 2, 2, "B".repeat(50), "Sec 2", "Cl 2", 0.70
        );

        RagContext context = contextBuilder.buildContext(List.of(chunk1, chunk2), 0.35, 60);

        assertThat(context.items()).hasSize(1);
        assertThat(context.items().get(0).sourceId()).isEqualTo("SRC-1");
        assertThat(context.totalCharacters()).isEqualTo(50);
    }

    @Test
    @DisplayName("buildContext should return empty context for empty or null chunk list")
    void buildContextShouldHandleEmpty() {
        RagContext contextNull = contextBuilder.buildContext(null, 0.35, 12000);
        assertThat(contextNull.isEmpty()).isTrue();

        RagContext contextEmpty = contextBuilder.buildContext(List.of(), 0.35, 12000);
        assertThat(contextEmpty.isEmpty()).isTrue();
    }
}
