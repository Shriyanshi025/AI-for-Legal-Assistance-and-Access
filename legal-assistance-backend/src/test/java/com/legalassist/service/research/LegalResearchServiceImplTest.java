package com.legalassist.service.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalassist.config.GenerationProperties;
import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.dto.research.LegalResearchRequest;
import com.legalassist.dto.research.LegalResearchResponse;
import com.legalassist.dto.research.ResearchSessionSummaryResponse;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.entity.ResearchSession;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.repository.ResearchSessionRepository;
import com.legalassist.service.generation.GenerationService;
import com.legalassist.service.rag.RagContext;
import com.legalassist.service.rag.RagContextBuilder;
import com.legalassist.service.rag.RagSourceItem;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalResearchServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ResearchSessionRepository researchSessionRepository;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private RagContextBuilder ragContextBuilder;

    @Mock
    private ResearchPromptBuilder researchPromptBuilder;

    @Mock
    private GenerationService generationService;

    private GenerationProperties generationProperties;
    private ObjectMapper objectMapper;
    private LegalResearchServiceImpl legalResearchService;

    private UUID userId;
    private UUID docId;
    private Document readyDocument;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        docId = UUID.randomUUID();
        generationProperties = new GenerationProperties("google-gemini", "gemini-3.5-flash", 5, 0.35, 12000, 0.0, "key");
        objectMapper = new ObjectMapper().findAndRegisterModules();

        legalResearchService = new LegalResearchServiceImpl(
                documentRepository,
                researchSessionRepository,
                semanticSearchService,
                ragContextBuilder,
                researchPromptBuilder,
                generationService,
                generationProperties,
                objectMapper
        );

        readyDocument = new Document(docId, userId, "agreement.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("executeResearch should return structured dossier and persist research session")
    void executeResearchSuccess() {
        LegalResearchRequest request = new LegalResearchRequest("Is notice required for termination?", List.of(docId), "Comprehensive Research", "New York", "2025");
        SimilaritySearchResultResponse chunk = new SimilaritySearchResultResponse(UUID.randomUUID(), docId, 1, 1, "Written notice of 30 days required.", "Sec 1", "Cl 1", 0.88);
        RagSourceItem item = new RagSourceItem("SRC-1", chunk.chunkId(), docId, 1, 1, chunk.content(), "Sec 1", "Cl 1", 0.88);
        RagContext ragContext = new RagContext(List.of(item), 30);

        String mockJson = """
                {
                  "issues": [{"id": "ISSUE-1", "title": "Termination Notice", "description": "Notice requirement analysis", "relatedCitations": ["SRC-1"]}],
                  "findings": [{"id": "FINDING-1", "title": "Written Notice Required", "statement": "Termination requires 30 days written notice", "supportStatus": "SUPPORTED", "evidenceType": "DIRECT", "citations": ["SRC-1"]}],
                  "evidenceMatrix": [{"findingTitle": "Written Notice Required", "sourceDocument": "agreement.pdf", "pageNumber": 1, "evidenceType": "DIRECT", "supportStatus": "SUPPORTED"}],
                  "conflicts": [],
                  "evidenceGaps": [{"description": "Missing governing jurisdiction", "whyItMatters": "Unspecified governing law", "relatedIssue": "ISSUE-1"}],
                  "followUpQuestions": ["Is there an amendment to Clause 1?"]
                }
                """;

        when(documentRepository.findAllById(List.of(docId))).thenReturn(List.of(readyDocument));
        when(semanticSearchService.searchSimilarChunksForDocuments(eq(List.of(docId)), eq("Is notice required for termination?"), anyInt())).thenReturn(List.of(chunk));
        when(ragContextBuilder.buildContext(any(), anyDouble(), anyInt())).thenReturn(ragContext);
        when(researchPromptBuilder.buildSystemInstruction()).thenReturn("SysInst");
        when(researchPromptBuilder.buildUserPrompt(any(), any(), any(), any(), any())).thenReturn("UserPrompt");
        when(generationService.generateRawContent("SysInst", "UserPrompt")).thenReturn(mockJson);

        LegalResearchResponse response = legalResearchService.executeResearch(userId, request);

        assertThat(response.researchQuestion()).isEqualTo("Is notice required for termination?");
        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().get(0).supportStatus()).isEqualTo("SUPPORTED");
        assertThat(response.sources()).hasSize(1);
        verify(researchSessionRepository).save(any(ResearchSession.class));
    }

    @Test
    @DisplayName("executeResearch should return evidence-bounded empty dossier when zero context chunks pass filter")
    void executeResearchEmptyContext() {
        LegalResearchRequest request = new LegalResearchRequest("Unrelated question", List.of(docId), "Comprehensive", null, null);

        when(documentRepository.findAllById(List.of(docId))).thenReturn(List.of(readyDocument));
        when(semanticSearchService.searchSimilarChunksForDocuments(eq(List.of(docId)), any(), anyInt())).thenReturn(List.of());

        LegalResearchResponse response = legalResearchService.executeResearch(userId, request);

        assertThat(response.findings()).hasSize(1);
        assertThat(response.findings().get(0).supportStatus()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(response.evidenceGaps()).isNotEmpty();
        verify(generationService, never()).generateRawContent(any(), any());
        verify(researchSessionRepository).save(any());
    }

    @Test
    @DisplayName("getUserResearchSessions should return user's research session summaries")
    void getUserResearchSessionsSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();
        ResearchSession session = new ResearchSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setResearchQuestion("Notice period");
        session.setResearchType("Comprehensive");
        session.setDocumentIdsJson(objectMapper.writeValueAsString(List.of(docId)));
        session.setDossierJson("""
                {"findings": [{}, {}], "evidenceGaps": [{}], "conflicts": []}
                """);
        session.setCreatedAt(Instant.now());

        when(researchSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(session));

        List<ResearchSessionSummaryResponse> summaries = legalResearchService.getUserResearchSessions(userId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id()).isEqualTo(sessionId);
        assertThat(summaries.get(0).findingsCount()).isEqualTo(2);
        assertThat(summaries.get(0).gapsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("executeResearch should throw IllegalArgumentException when document belongs to another user")
    void executeResearchUnauthorizedDocument() {
        UUID otherUser = UUID.randomUUID();
        Document otherDoc = new Document(docId, otherUser, "other.pdf", "application/pdf", "path", 100L, DocumentStatus.READY, Instant.now(), Instant.now());
        LegalResearchRequest request = new LegalResearchRequest("Notice period?", List.of(docId), "Comprehensive", null, null);

        when(documentRepository.findAllById(List.of(docId))).thenReturn(List.of(otherDoc));

        assertThatThrownBy(() -> legalResearchService.executeResearch(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getResearchSession should throw AccessDeniedException when session belongs to another user")
    void getResearchSessionAccessDenied() {
        UUID sessionId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        ResearchSession session = new ResearchSession();
        session.setId(sessionId);
        session.setUserId(otherUser);

        when(researchSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> legalResearchService.getResearchSession(sessionId, userId))
                .isInstanceOf(com.legalassist.exception.AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
