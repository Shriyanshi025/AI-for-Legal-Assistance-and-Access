package com.legalassist.service.rag;

import com.legalassist.config.GenerationProperties;
import com.legalassist.dto.LegalAskRequest;
import com.legalassist.dto.LegalAnswerResponse;
import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.service.generation.GeminiGenerationResponse;
import com.legalassist.service.generation.GenerationService;
import com.legalassist.service.search.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalQaServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private RagContextBuilder ragContextBuilder;

    @Mock
    private LegalPromptBuilder legalPromptBuilder;

    @Mock
    private GenerationService generationService;

    @Mock
    private CitationValidator citationValidator;

    private GenerationProperties generationProperties;
    private LegalQaServiceImpl legalQaService;
    private UUID documentId;
    private Document readyDocument;

    @BeforeEach
    void setUp() {
        generationProperties = new GenerationProperties("google-gemini", "gemini-3.5-flash", 5, 0.35, 12000, 0.0, "key");
        legalQaService = new LegalQaServiceImpl(
                documentRepository,
                semanticSearchService,
                ragContextBuilder,
                legalPromptBuilder,
                generationService,
                citationValidator,
                generationProperties
        );

        documentId = UUID.randomUUID();
        Instant now = Instant.now();
        readyDocument = new Document(documentId, UUID.randomUUID(), "contract.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, now, now);
    }

    @Test
    @DisplayName("askQuestion should complete full Q&A pipeline when relevant chunks exist")
    void askQuestionSuccess() {
        LegalAskRequest request = new LegalAskRequest("What is the termination notice?");
        SimilaritySearchResultResponse chunk = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 1, "30 days written notice", "Sec 1", "Cl 1", 0.85
        );
        RagSourceItem item = new RagSourceItem("SRC-1", chunk.chunkId(), documentId, 1, 1, chunk.content(), "Sec 1", "Cl 1", 0.85);
        RagContext context = new RagContext(List.of(item), 25);
        GeminiGenerationResponse rawGen = new GeminiGenerationResponse("Termination requires 30 days notice.", true, List.of("SRC-1"));

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(readyDocument));
        when(semanticSearchService.searchSimilarChunks(eq(documentId), eq("What is the termination notice?"), eq(5)))
                .thenReturn(List.of(chunk));
        when(ragContextBuilder.buildContext(any(), anyDouble(), anyInt())).thenReturn(context);
        when(legalPromptBuilder.buildSystemInstruction()).thenReturn("SysInst");
        when(legalPromptBuilder.buildUserPrompt(eq(context), eq("What is the termination notice?"))).thenReturn("UserPrompt");
        when(generationService.generateAnswer("SysInst", "UserPrompt")).thenReturn(rawGen);
        when(citationValidator.validateAndBuildResponse(rawGen, context))
                .thenReturn(new LegalAnswerResponse("Termination requires 30 days notice.", true, List.of()));

        LegalAnswerResponse response = legalQaService.askQuestion(documentId, request);

        assertThat(response.answer()).isEqualTo("Termination requires 30 days notice.");
        assertThat(response.grounded()).isTrue();
        verify(generationService).generateAnswer("SysInst", "UserPrompt");
    }

    @Test
    @DisplayName("askQuestion should short-circuit and NOT call Gemini when zero chunks pass minSimilarity gate")
    void askQuestionShortCircuitsWhenZeroChunksPassGate() {
        LegalAskRequest request = new LegalAskRequest("Unrelated question");
        SimilaritySearchResultResponse lowSimChunk = new SimilaritySearchResultResponse(
                UUID.randomUUID(), documentId, 1, 1, "Irrelevant content", "Sec 1", "Cl 1", 0.10
        );

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(readyDocument));
        when(semanticSearchService.searchSimilarChunks(eq(documentId), eq("Unrelated question"), eq(5)))
                .thenReturn(List.of(lowSimChunk));
        when(ragContextBuilder.buildContext(any(), anyDouble(), anyInt())).thenReturn(new RagContext(List.of(), 0));

        LegalAnswerResponse response = legalQaService.askQuestion(documentId, request);

        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).isEqualTo(CitationValidator.INSUFFICIENT_CONTEXT_ANSWER);
        assertThat(response.citations()).isEmpty();

        // GUARANTEE: Gemini API generation service is NEVER invoked
        verify(generationService, never()).generateAnswer(any(), any());
    }

    @Test
    @DisplayName("askQuestion should short-circuit and NOT call Gemini when search returns zero chunks")
    void askQuestionShortCircuitsWhenZeroChunksReturned() {
        LegalAskRequest request = new LegalAskRequest("Question");

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(readyDocument));
        when(semanticSearchService.searchSimilarChunks(eq(documentId), eq("Question"), eq(5)))
                .thenReturn(List.of());

        LegalAnswerResponse response = legalQaService.askQuestion(documentId, request);

        assertThat(response.grounded()).isFalse();
        assertThat(response.citations()).isEmpty();
        verify(generationService, never()).generateAnswer(any(), any());
    }

    @Test
    @DisplayName("askQuestion should throw DocumentNotFoundException when document does not exist")
    void askQuestionShouldThrowWhenNotFound() {
        UUID randomId = UUID.randomUUID();
        when(documentRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> legalQaService.askQuestion(randomId, new LegalAskRequest("Q")))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("askQuestion should throw IllegalStateException when document status is not READY")
    void askQuestionShouldThrowWhenNotReady() {
        Instant now = Instant.now();
        Document unreadyDoc = new Document(documentId, UUID.randomUUID(), "c.pdf", "pdf", "p", 10L, DocumentStatus.PROCESSING, now, now);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(unreadyDoc));

        assertThatThrownBy(() -> legalQaService.askQuestion(documentId, new LegalAskRequest("Q")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROCESSING");
    }
}
