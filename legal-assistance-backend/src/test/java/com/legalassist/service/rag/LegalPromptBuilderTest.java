package com.legalassist.service.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LegalPromptBuilderTest {

    private LegalPromptBuilder promptBuilder;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        promptBuilder = new LegalPromptBuilder();
        documentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("buildSystemInstruction should contain grounding mandates and reference rules")
    void buildSystemInstructionShouldContainMandates() {
        String systemInstruction = promptBuilder.buildSystemInstruction();

        assertThat(systemInstruction)
                .contains("expert legal document assistant")
                .contains("GROUNDING MANDATES")
                .contains("citations")
                .contains("<<<DOCUMENT CONTEXT>>>");
    }

    @Test
    @DisplayName("buildUserPrompt should format context blocks with SRC-N labels and user question")
    void buildUserPromptShouldFormatContextAndQuestion() {
        RagSourceItem item = new RagSourceItem(
                "SRC-1", UUID.randomUUID(), documentId, 5, 10, "Either party may terminate on 30 days notice.", "Sec 5", "Cl 1", 0.85
        );
        RagContext context = new RagContext(List.of(item), 45);

        String userPrompt = promptBuilder.buildUserPrompt(context, "How can the contract be terminated?");

        assertThat(userPrompt)
                .contains("=== DOCUMENT CONTEXT ===")
                .contains("[SRC-1] (Page 5, Chunk 10):")
                .contains("Either party may terminate on 30 days notice.")
                .contains("<<<END DOCUMENT CONTEXT>>>")
                .contains("=== USER QUESTION ===")
                .contains("How can the contract be terminated?");
    }
}
