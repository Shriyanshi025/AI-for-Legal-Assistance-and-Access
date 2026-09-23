package com.legalassist.service.rag;

import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.exception.GenerationException;
import com.legalassist.service.generation.GeminiGenerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitationValidatorTest {

    private CitationValidator citationValidator;
    private UUID documentId;
    private RagSourceItem sourceItem1;
    private RagSourceItem sourceItem2;
    private RagContext context;

    @BeforeEach
    void setUp() {
        citationValidator = new CitationValidator();
        documentId = UUID.randomUUID();

        sourceItem1 = new RagSourceItem(
                "SRC-1", UUID.randomUUID(), documentId, 7, 12,
                "Either party may terminate this agreement with 30 days notice.", "Sec 7", "Cl 1", 0.85
        );
        sourceItem2 = new RagSourceItem(
                "SRC-2", UUID.randomUUID(), documentId, 8, 14,
                "Termination fee is 1000 USD.", "Sec 8", "Cl 2", 0.75
        );
        context = new RagContext(List.of(sourceItem1, sourceItem2), 100);
    }

    @Test
    @DisplayName("validateAndBuildResponse should map valid SRC-N citations to database provenance")
    void validateAndBuildResponseSuccess() {
        GeminiGenerationResponse rawResponse = new GeminiGenerationResponse(
                "The contract allows termination on 30 days notice.",
                true,
                List.of("SRC-1")
        );

        LegalAnswerResponse response = citationValidator.validateAndBuildResponse(rawResponse, context);

        assertThat(response.grounded()).isTrue();
        assertThat(response.answer()).isEqualTo("The contract allows termination on 30 days notice.");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).pageNumber()).isEqualTo(7);
        assertThat(response.citations().get(0).chunkIndex()).isEqualTo(12);
        assertThat(response.citations().get(0).excerpt()).contains("Either party may terminate");
    }

    @Test
    @DisplayName("validateAndBuildResponse should discard hallucinated source IDs and force grounded=false if no valid citations remain")
    void validateAndBuildResponseDiscardHallucinated() {
        GeminiGenerationResponse rawResponse = new GeminiGenerationResponse(
                "Some claim.",
                true,
                List.of("SRC-999", "page 5")
        );

        LegalAnswerResponse response = citationValidator.validateAndBuildResponse(rawResponse, context);

        assertThat(response.grounded()).isFalse();
        assertThat(response.citations()).isEmpty();
        assertThat(response.answer()).isEqualTo(CitationValidator.INSUFFICIENT_CONTEXT_ANSWER);
    }

    @Test
    @DisplayName("validateAndBuildResponse should force citations empty when grounded=false")
    void validateAndBuildResponseGroundedFalseEmptiesCitations() {
        GeminiGenerationResponse rawResponse = new GeminiGenerationResponse(
                "Insufficient details provided.",
                false,
                List.of("SRC-1")
        );

        LegalAnswerResponse response = citationValidator.validateAndBuildResponse(rawResponse, context);

        assertThat(response.grounded()).isFalse();
        assertThat(response.citations()).isEmpty();
        assertThat(response.answer()).isEqualTo(CitationValidator.INSUFFICIENT_CONTEXT_ANSWER);
    }

    @Test
    @DisplayName("validateAndBuildResponse should throw GenerationException when answer is null or blank")
    void validateAndBuildResponseShouldThrowOnEmptyAnswer() {
        GeminiGenerationResponse nullAnswer = new GeminiGenerationResponse(null, true, List.of("SRC-1"));
        assertThatThrownBy(() -> citationValidator.validateAndBuildResponse(nullAnswer, context))
                .isInstanceOf(GenerationException.class)
                .hasMessageContaining("empty or null answer");

        GeminiGenerationResponse blankAnswer = new GeminiGenerationResponse("   ", true, List.of("SRC-1"));
        assertThatThrownBy(() -> citationValidator.validateAndBuildResponse(blankAnswer, context))
                .isInstanceOf(GenerationException.class)
                .hasMessageContaining("empty or null answer");
    }
}
