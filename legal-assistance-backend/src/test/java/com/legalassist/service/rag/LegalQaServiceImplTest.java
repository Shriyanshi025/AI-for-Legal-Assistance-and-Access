package com.legalassist.service.rag;

import com.legalassist.config.GenerationProperties;
import com.legalassist.dto.CitationResponse;
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

    @Test
    @DisplayName("askMultiDocumentQuestion should retrieve chunks from single selected document [A]")
    void askMultiDocumentQuestionSingleDoc() {
        UUID docA = UUID.randomUUID();
        Document documentA = new Document(docA, UUID.randomUUID(), "docA.pdf", "application/pdf", "pathA", 100L, DocumentStatus.READY, Instant.now(), Instant.now());
        LegalAskRequest request = new LegalAskRequest("What is section 1?", List.of(docA));

        SimilaritySearchResultResponse chunkA = new SimilaritySearchResultResponse(
                UUID.randomUUID(), docA, 1, 1, "Section 1 content", "Sec 1", "Cl 1", 0.88
        );
        RagSourceItem item = new RagSourceItem("SRC-1", chunkA.chunkId(), docA, 1, 1, chunkA.content(), "Sec 1", "Cl 1", 0.88);
        RagContext context = new RagContext(List.of(item), 20);
        GeminiGenerationResponse rawGen = new GeminiGenerationResponse("Section 1 is clause A.", true, List.of("SRC-1"));

        when(documentRepository.findAllById(List.of(docA))).thenReturn(List.of(documentA));
        when(semanticSearchService.searchSimilarChunksForDocuments(eq(List.of(docA)), eq("What is section 1?"), eq(5)))
                .thenReturn(List.of(chunkA));
        when(ragContextBuilder.buildContext(any(), anyDouble(), anyInt())).thenReturn(context);
        when(legalPromptBuilder.buildSystemInstruction()).thenReturn("SysInst");
        when(legalPromptBuilder.buildUserPrompt(eq(context), eq("What is section 1?"))).thenReturn("UserPrompt");
        when(generationService.generateAnswer("SysInst", "UserPrompt")).thenReturn(rawGen);
        when(citationValidator.validateAndBuildResponse(rawGen, context))
                .thenReturn(new LegalAnswerResponse("Section 1 is clause A.", true, List.of(
                        new CitationResponse(docA, 1, 1, "Section 1 content")
                )));

        LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(List.of(docA), request);

        assertThat(response.answer()).isEqualTo("Section 1 is clause A.");
        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).documentId()).isEqualTo(docA);
        verify(semanticSearchService).searchSimilarChunksForDocuments(eq(List.of(docA)), any(), anyInt());
    }

    @Test
    @DisplayName("askMultiDocumentQuestion should retrieve and synthesize chunks from multiple selected documents [A, B]")
    void askMultiDocumentQuestionMultipleDocs() {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        Document documentA = new Document(docA, UUID.randomUUID(), "docA.pdf", "application/pdf", "pathA", 100L, DocumentStatus.READY, Instant.now(), Instant.now());
        Document documentB = new Document(docB, UUID.randomUUID(), "docB.pdf", "application/pdf", "pathB", 200L, DocumentStatus.READY, Instant.now(), Instant.now());
        LegalAskRequest request = new LegalAskRequest("Compare termination terms", List.of(docA, docB));

        SimilaritySearchResultResponse chunkA = new SimilaritySearchResultResponse(
                UUID.randomUUID(), docA, 2, 1, "Doc A 30 days notice", "Sec 2", "Cl 1", 0.90
        );
        SimilaritySearchResultResponse chunkB = new SimilaritySearchResultResponse(
                UUID.randomUUID(), docB, 5, 3, "Doc B 60 days notice", "Sec 4", "Cl 2", 0.85
        );

        RagSourceItem itemA = new RagSourceItem("SRC-1", chunkA.chunkId(), docA, 2, 1, chunkA.content(), "Sec 2", "Cl 1", 0.90);
        RagSourceItem itemB = new RagSourceItem("SRC-2", chunkB.chunkId(), docB, 5, 3, chunkB.content(), "Sec 4", "Cl 2", 0.85);
        RagContext context = new RagContext(List.of(itemA, itemB), 40);
        GeminiGenerationResponse rawGen = new GeminiGenerationResponse("Doc A requires 30 days while Doc B requires 60 days.", true, List.of("SRC-1", "SRC-2"));

        when(documentRepository.findAllById(List.of(docA, docB))).thenReturn(List.of(documentA, documentB));
        when(semanticSearchService.searchSimilarChunksForDocuments(eq(List.of(docA, docB)), eq("Compare termination terms"), eq(5)))
                .thenReturn(List.of(chunkA, chunkB));
        when(ragContextBuilder.buildContext(any(), anyDouble(), anyInt())).thenReturn(context);
        when(legalPromptBuilder.buildSystemInstruction()).thenReturn("SysInst");
        when(legalPromptBuilder.buildUserPrompt(eq(context), eq("Compare termination terms"))).thenReturn("UserPrompt");
        when(generationService.generateAnswer("SysInst", "UserPrompt")).thenReturn(rawGen);

        CitationResponse citA = new CitationResponse(docA, 2, 1, "Doc A 30 days notice");
        CitationResponse citB = new CitationResponse(docB, 5, 3, "Doc B 60 days notice");
        when(citationValidator.validateAndBuildResponse(rawGen, context))
                .thenReturn(new LegalAnswerResponse("Doc A requires 30 days while Doc B requires 60 days.", true, List.of(citA, citB)));

        LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(List.of(docA, docB), request);

        assertThat(response.answer()).contains("Doc A").contains("Doc B");
        assertThat(response.citations()).hasSize(2);
        assertThat(response.citations().get(0).documentId()).isEqualTo(docA);
        assertThat(response.citations().get(1).documentId()).isEqualTo(docB);
    }

    @Test
    @DisplayName("askMultiDocumentQuestion should exclude unselected documents B and C when only A is selected")
    void askMultiDocumentQuestionExcludesUnselectedDocs() {
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        UUID docC = UUID.randomUUID();

        Document documentA = new Document(docA, UUID.randomUUID(), "docA.pdf", "application/pdf", "pathA", 100L, DocumentStatus.READY, Instant.now(), Instant.now());
        LegalAskRequest request = new LegalAskRequest("Query only A", List.of(docA));

        when(documentRepository.findAllById(List.of(docA))).thenReturn(List.of(documentA));
        // Search is strictly called with docA, docB and docC are never passed to search
        when(semanticSearchService.searchSimilarChunksForDocuments(eq(List.of(docA)), any(), anyInt()))
                .thenReturn(List.of());

        LegalAnswerResponse response = legalQaService.askMultiDocumentQuestion(List.of(docA), request);

        assertThat(response.grounded()).isFalse();
        verify(semanticSearchService).searchSimilarChunksForDocuments(eq(List.of(docA)), eq("Query only A"), eq(5));
        verify(semanticSearchService, never()).searchSimilarChunksForDocuments(eq(List.of(docB)), any(), anyInt());
        verify(semanticSearchService, never()).searchSimilarChunksForDocuments(eq(List.of(docC)), any(), anyInt());
    }

    @Test
    @DisplayName("askMultiDocumentQuestion should throw IllegalArgumentException when documentIds list is empty")
    void askMultiDocumentQuestionThrowsWhenEmptyDocIds() {
        LegalAskRequest request = new LegalAskRequest("Question", List.of());

        assertThatThrownBy(() -> legalQaService.askMultiDocumentQuestion(List.of(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one document ID must be selected");
    }

    @Test
    @DisplayName("askMultiDocumentQuestion should throw IllegalArgumentException when selected document does not exist")
    void askMultiDocumentQuestionThrowsWhenDocNotFound() {
        UUID nonExistent = UUID.randomUUID();
        LegalAskRequest request = new LegalAskRequest("Question", List.of(nonExistent));

        when(documentRepository.findAllById(List.of(nonExistent))).thenReturn(List.of());

        assertThatThrownBy(() -> legalQaService.askMultiDocumentQuestion(List.of(nonExistent), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("One or more selected documents do not exist");
    }

    @Test
    @DisplayName("askMultiDocumentQuestion should throw AccessDeniedException when one of the documents belongs to another user [H, I]")
    void askMultiDocumentQuestionThrowsAccessDeniedForUnauthorizedDocument() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        Instant now = Instant.now();
        Document documentA = new Document(docA, userA, "docA.pdf", "application/pdf", "pathA", 100L, DocumentStatus.READY, now, now);
        Document documentB = new Document(docB, userB, "docB.pdf", "application/pdf", "pathB", 200L, DocumentStatus.READY, now, now);

        LegalAskRequest request = new LegalAskRequest("Synthesize information", List.of(docA, docB));

        when(documentRepository.findAllById(List.of(docA, docB))).thenReturn(List.of(documentA, documentB));

        assertThatThrownBy(() -> legalQaService.askMultiDocumentQuestion(List.of(docA, docB), request, userA))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        // CRITICAL SECURITY RULE: Gemini generation service is NEVER invoked for unauthorized requests
        verify(generationService, never()).generateAnswer(any(), any());
        verify(semanticSearchService, never()).searchSimilarChunksForDocuments(any(), any(), anyInt());
    }
}
