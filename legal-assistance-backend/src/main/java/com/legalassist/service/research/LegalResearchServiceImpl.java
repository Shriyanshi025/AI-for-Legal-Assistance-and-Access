package com.legalassist.service.research;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.legalassist.config.GenerationProperties;
import com.legalassist.dto.CitationResponse;
import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.dto.research.*;
import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentStatus;
import com.legalassist.entity.ResearchSession;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.repository.ResearchSessionRepository;
import com.legalassist.service.generation.GenerationService;
import com.legalassist.service.rag.RagContext;
import com.legalassist.service.rag.RagContextBuilder;
import com.legalassist.service.search.SemanticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class LegalResearchServiceImpl implements LegalResearchService {

    private static final Logger log = LoggerFactory.getLogger(LegalResearchServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final ResearchSessionRepository researchSessionRepository;
    private final SemanticSearchService semanticSearchService;
    private final RagContextBuilder ragContextBuilder;
    private final ResearchPromptBuilder researchPromptBuilder;
    private final GenerationService generationService;
    private final GenerationProperties generationProperties;
    private final ObjectMapper objectMapper;

    public LegalResearchServiceImpl(
            DocumentRepository documentRepository,
            ResearchSessionRepository researchSessionRepository,
            SemanticSearchService semanticSearchService,
            RagContextBuilder ragContextBuilder,
            ResearchPromptBuilder researchPromptBuilder,
            GenerationService generationService,
            GenerationProperties generationProperties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.researchSessionRepository = researchSessionRepository;
        this.semanticSearchService = semanticSearchService;
        this.ragContextBuilder = ragContextBuilder;
        this.researchPromptBuilder = researchPromptBuilder;
        this.generationService = generationService;
        this.generationProperties = generationProperties;
        this.objectMapper = (objectMapper != null ? objectMapper.copy() : new ObjectMapper())
                .registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public LegalResearchResponse executeResearch(UUID userId, LegalResearchRequest request) {
        if (request == null || request.researchQuestion() == null || request.researchQuestion().isBlank()) {
            throw new IllegalArgumentException("Research question cannot be null or blank");
        }
        List<UUID> docIds = request.documentIds();
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("At least one document ID must be selected for research");
        }

        log.info("Research request received for user ID {}", userId);

        List<Document> documents = documentRepository.findAllById(docIds);
        if (documents.size() != docIds.size()) {
            throw new IllegalArgumentException("One or more selected documents do not exist");
        }

        for (Document doc : documents) {
            if (userId != null && doc.getUserId() != null && !doc.getUserId().equals(userId)) {
                log.warn("User ID {} attempted to access unauthorized document {}", userId, doc.getId());
                throw new IllegalArgumentException("Access denied: Document '" + doc.getFilename() + "' does not belong to user");
            }
            if (doc.getStatus() != DocumentStatus.READY) {
                throw new IllegalStateException("Document '" + doc.getFilename() + "' is not ready for research. Status: " + doc.getStatus());
            }
        }

        log.info("Validated {} selected documents belonging to user ID {}", docIds.size(), userId);

        String question = request.researchQuestion().trim();
        int topK = generationProperties.getTopK() * 2; // Retrieve larger evidence set for research
        double minSimilarity = generationProperties.getMinSimilarity();
        int maxContextChars = generationProperties.getMaxContextChars() * 2;

        log.info("Retrieval started for {} documents with topK={}", docIds.size(), topK);
        List<SimilaritySearchResultResponse> chunks = semanticSearchService.searchSimilarChunksForDocuments(docIds, question, topK);
        log.info("Retrieved {} vector search chunks for research question", chunks != null ? chunks.size() : 0);

        RagContext ragContext = (chunks != null && !chunks.isEmpty())
                ? ragContextBuilder.buildContext(chunks, minSimilarity, maxContextChars)
                : new RagContext(List.of(), 0);

        List<CitationResponse> sources = buildCitations(ragContext);

        if (ragContext.isEmpty()) {
            log.info("Zero context chunks passed similarity gate for research query on {} documents. Returning evidence-bounded empty dossier.", docIds.size());
            return saveAndBuildEmptyResponse(userId, request, sources);
        }

        String systemInstruction = researchPromptBuilder.buildSystemInstruction();
        String userPrompt = researchPromptBuilder.buildUserPrompt(
                ragContext,
                question,
                request.researchType(),
                request.jurisdiction(),
                request.relevantDate()
        );

        log.info("Gemini invocation started using model '{}' with {} context items", generationProperties.getGenerationModel(), ragContext.items().size());
        String rawJson = generationService.generateRawContent(systemInstruction, userPrompt);
        log.info("Gemini raw JSON generation succeeded (length: {} chars)", rawJson != null ? rawJson.length() : 0);

        LegalResearchResponse response = parseAndBuildResponse(userId, request, rawJson, sources);
        saveResearchSession(userId, response, request.documentIds());
        log.info("Research session {} successfully persisted for user ID {}", response.id(), userId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchSessionSummaryResponse> getUserResearchSessions(UUID userId) {
        List<ResearchSession> sessions = researchSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream().map(s -> {
            int docCount = 0;
            int findingsCount = 0;
            int gapsCount = 0;
            int conflictsCount = 0;
            try {
                if (s.getDocumentIdsJson() != null) {
                    List<?> ids = objectMapper.readValue(s.getDocumentIdsJson(), List.class);
                    docCount = ids.size();
                }
                if (s.getDossierJson() != null) {
                    Map<?, ?> map = objectMapper.readValue(s.getDossierJson(), Map.class);
                    if (map.get("findings") instanceof List<?> list) findingsCount = list.size();
                    if (map.get("evidenceGaps") instanceof List<?> list) gapsCount = list.size();
                    if (map.get("conflicts") instanceof List<?> list) conflictsCount = list.size();
                }
            } catch (Exception e) {
                log.warn("Failed to parse stored session counts for session {}: {}", s.getId(), e.getMessage());
            }

            return new ResearchSessionSummaryResponse(
                    s.getId(),
                    s.getResearchQuestion(),
                    s.getResearchType(),
                    docCount,
                    findingsCount,
                    gapsCount,
                    conflictsCount,
                    LocalDateTime.ofInstant(s.getCreatedAt(), ZoneId.systemDefault())
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LegalResearchResponse getResearchSession(UUID sessionId, UUID userId) {
        ResearchSession session = researchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.legalassist.exception.ResearchSessionNotFoundException("Research session not found with ID: " + sessionId));

        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new com.legalassist.exception.AccessDeniedException("Access denied: Research session does not belong to user");
        }

        try {
            return objectMapper.readValue(session.getDossierJson(), LegalResearchResponse.class);
        } catch (Exception e) {
            log.error("Failed to deserialize saved research session {}", sessionId, e);
            throw new IllegalStateException("Failed to load saved research session dossier: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public FollowUpResearchResponse executeFollowUp(UUID sessionId, UUID userId, FollowUpResearchRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("Follow-up question cannot be blank");
        }

        ResearchSession session = researchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.legalassist.exception.ResearchSessionNotFoundException("Research session not found with ID: " + sessionId));

        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new com.legalassist.exception.AccessDeniedException("Access denied: Research session does not belong to user");
        }

        List<UUID> docIds = new ArrayList<>();
        if (session.getDocumentIdsJson() != null) {
            try {
                List<?> ids = objectMapper.readValue(session.getDocumentIdsJson(), List.class);
                for (Object idObj : ids) {
                    docIds.add(UUID.fromString(idObj.toString()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse documentIdsJson for session {}: {}", sessionId, e.getMessage());
            }
        }

        String question = request.question().trim();
        List<CitationResponse> sources = List.of();
        String answerText = "";

        if (!docIds.isEmpty()) {
            int topK = generationProperties.getTopK();
            List<SimilaritySearchResultResponse> chunks = semanticSearchService.searchSimilarChunksForDocuments(docIds, question, topK);
            RagContext ragContext = (chunks != null && !chunks.isEmpty())
                    ? ragContextBuilder.buildContext(chunks, generationProperties.getMinSimilarity(), generationProperties.getMaxContextChars())
                    : new RagContext(List.of(), 0);

            sources = buildCitations(ragContext);

            if (!ragContext.isEmpty()) {
                String systemInstruction = "You are an expert legal research assistant. Answer the follow-up inquiry with well-structured, professional legal formatting using clear paragraphs, bullet points for key terms or clauses, **bold text** for critical findings, and *italicized text* for citations or statutory references. Organize your response logically into an Overview/Direct Answer, Grounded Legal Analysis, and Key Takeaways. Ground your answer strictly in the provided document evidence.";
                String userPrompt = "BACKGROUND RESEARCH QUESTION: " + session.getResearchQuestion() + "\n\nFOLLOW-UP QUESTION: " + question + "\n\nRETRIEVED EVIDENCE CONTEXT:\n" + researchPromptBuilder.buildUserPrompt(ragContext, question, "FollowUp", session.getJurisdiction(), session.getRelevantDate());

                String rawJson = generationService.generateRawContent(systemInstruction, userPrompt);
                try {
                    String cleanJson = rawJson.trim();
                    if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                    if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                    if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    cleanJson = cleanJson.trim();
                    Map<?, ?> map = objectMapper.readValue(cleanJson, Map.class);
                    if (map.get("answer") != null) answerText = map.get("answer").toString();
                    else if (map.get("statement") != null) answerText = map.get("statement").toString();
                    else answerText = cleanJson;
                } catch (Exception e) {
                    answerText = rawJson;
                }
            } else {
                answerText = "I could not find sufficient direct evidence in the selected document scope to answer this follow-up question.";
            }
        } else {
            answerText = "No document context is linked to this research session.";
        }

        return new FollowUpResearchResponse(
                sessionId,
                question,
                answerText,
                sources,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public ResearchSessionSummaryResponse renameResearchSession(UUID sessionId, UUID userId, RenameResearchSessionRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Session title cannot be blank");
        }

        ResearchSession session = researchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.legalassist.exception.ResearchSessionNotFoundException("Research session not found with ID: " + sessionId));

        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new com.legalassist.exception.AccessDeniedException("Access denied: Research session does not belong to user");
        }

        String newTitle = request.title().trim();
        session.setResearchQuestion(newTitle);
        session.setUpdatedAt(Instant.now());

        if (session.getDossierJson() != null) {
            try {
                LegalResearchResponse existingDossier = objectMapper.readValue(session.getDossierJson(), LegalResearchResponse.class);
                LegalResearchResponse updatedDossier = new LegalResearchResponse(
                        existingDossier.id(),
                        newTitle,
                        existingDossier.researchType(),
                        existingDossier.jurisdiction(),
                        existingDossier.relevantDate(),
                        existingDossier.selectedDocumentIds(),
                        existingDossier.issues(),
                        existingDossier.findings(),
                        existingDossier.evidenceMatrix(),
                        existingDossier.conflicts(),
                        existingDossier.evidenceGaps(),
                        existingDossier.followUpQuestions(),
                        existingDossier.sources(),
                        existingDossier.createdAt()
                );
                session.setDossierJson(objectMapper.writeValueAsString(updatedDossier));
            } catch (Exception e) {
                log.warn("Could not update dossierJson title for session {}: {}", sessionId, e.getMessage());
            }
        }

        researchSessionRepository.save(session);
        log.info("Renamed research session {} to '{}' for user ID {}", sessionId, newTitle, userId);

        int docCount = 0;
        int findingsCount = 0;
        int gapsCount = 0;
        int conflictsCount = 0;
        try {
            if (session.getDocumentIdsJson() != null) {
                List<?> ids = objectMapper.readValue(session.getDocumentIdsJson(), List.class);
                docCount = ids.size();
            }
            if (session.getDossierJson() != null) {
                Map<?, ?> map = objectMapper.readValue(session.getDossierJson(), Map.class);
                if (map.get("findings") instanceof List<?> list) findingsCount = list.size();
                if (map.get("evidenceGaps") instanceof List<?> list) gapsCount = list.size();
                if (map.get("conflicts") instanceof List<?> list) conflictsCount = list.size();
            }
        } catch (Exception ignored) {}

        return new ResearchSessionSummaryResponse(
                session.getId(),
                session.getResearchQuestion(),
                session.getResearchType(),
                docCount,
                findingsCount,
                gapsCount,
                conflictsCount,
                LocalDateTime.ofInstant(session.getCreatedAt(), ZoneId.systemDefault())
        );
    }

    @Override
    @Transactional
    public void deleteResearchSession(UUID sessionId, UUID userId) {
        ResearchSession session = researchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new com.legalassist.exception.ResearchSessionNotFoundException("Research session not found with ID: " + sessionId));

        if (userId != null && session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new com.legalassist.exception.AccessDeniedException("Access denied: Research session does not belong to user");
        }

        researchSessionRepository.delete(session);
        log.info("Deleted research session record {} for user ID {}", sessionId, userId);
    }

    private void saveResearchSession(UUID userId, LegalResearchResponse response, List<UUID> docIds) {
        try {
            ResearchSession session = new ResearchSession();
            session.setId(response.id());
            session.setUserId(userId);
            session.setResearchQuestion(response.researchQuestion());
            session.setResearchType(response.researchType());
            session.setJurisdiction(response.jurisdiction());
            session.setRelevantDate(response.relevantDate());
            session.setDocumentIdsJson(objectMapper.writeValueAsString(docIds));
            session.setDossierJson(objectMapper.writeValueAsString(response));
            session.setCreatedAt(Instant.now());
            session.setUpdatedAt(Instant.now());

            researchSessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to persist ResearchSession to database (user: {}, id: {}): {}", userId, response != null ? response.id() : "null", e.getMessage(), e);
            throw new IllegalStateException("Failed to persist research session: " + e.getMessage(), e);
        }
    }

    private LegalResearchResponse parseAndBuildResponse(
            UUID userId,
            LegalResearchRequest request,
            String rawJson,
            List<CitationResponse> sources
    ) {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            String cleanJson = rawJson.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            Map<String, Object> map = objectMapper.readValue(cleanJson, new TypeReference<>() {});

            List<ResearchIssueDto> issues = parseList(map.get("issues"), ResearchIssueDto.class);
            List<ResearchFindingDto> findings = parseList(map.get("findings"), ResearchFindingDto.class);
            List<EvidenceMatrixItemDto> matrix = parseList(map.get("evidenceMatrix"), EvidenceMatrixItemDto.class);
            List<ResearchConflictDto> conflicts = parseList(map.get("conflicts"), ResearchConflictDto.class);
            List<EvidenceGapDto> gaps = parseList(map.get("evidenceGaps"), EvidenceGapDto.class);
            List<String> followUps = parseStringList(map.get("followUpQuestions"));

            return new LegalResearchResponse(
                    sessionId,
                    request.researchQuestion(),
                    request.researchType() != null ? request.researchType() : "Comprehensive Research",
                    request.jurisdiction(),
                    request.relevantDate(),
                    request.documentIds(),
                    issues,
                    findings,
                    matrix,
                    conflicts,
                    gaps,
                    followUps,
                    sources,
                    now
            );
        } catch (Exception e) {
            log.error("Failed to parse Gemini structured JSON response: {}", rawJson, e);
            throw new IllegalStateException("Legal research could not be generated because the AI response could not be processed.", e);
        }
    }

    private LegalResearchResponse saveAndBuildEmptyResponse(
            UUID userId,
            LegalResearchRequest request,
            List<CitationResponse> sources
    ) {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ResearchFindingDto emptyFinding = new ResearchFindingDto(
                "FINDING-1",
                "Insufficient Direct Evidence",
                "I could not establish a conclusive answer from the selected sources. No retrieved chunks passed minimum similarity threshold.",
                "INSUFFICIENT_EVIDENCE",
                "MISSING",
                List.of()
        );

        EvidenceGapDto gap = new EvidenceGapDto(
                "Insufficient evidence available in the uploaded document scope.",
                "The primary research question cannot be verified directly from the selected files.",
                "Primary Question Coverage"
        );

        List<String> followUp = List.of(
                "Verify if additional contract schedules or amendments need to be uploaded.",
                "Check if the governing jurisdiction is specified in an unselected document."
        );

        LegalResearchResponse response = new LegalResearchResponse(
                sessionId,
                request.researchQuestion(),
                request.researchType() != null ? request.researchType() : "Comprehensive Research",
                request.jurisdiction(),
                request.relevantDate(),
                request.documentIds(),
                List.of(new ResearchIssueDto("ISSUE-1", request.researchQuestion(), "Primary inquiry topic", List.of())),
                List.of(emptyFinding),
                List.of(),
                List.of(),
                List.of(gap),
                followUp,
                sources,
                now
        );

        saveResearchSession(userId, response, request.documentIds());
        return response;
    }

    private List<CitationResponse> buildCitations(RagContext ragContext) {
        if (ragContext == null || ragContext.items() == null) return List.of();

        List<CitationResponse> list = new ArrayList<>();
        for (var item : ragContext.items()) {
            list.add(new CitationResponse(
                    item.documentId(),
                    item.pageNumber(),
                    item.chunkIndex(),
                    item.content()
            ));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseList(Object obj, Class<T> clazz) {
        if (!(obj instanceof List<?> list)) return List.of();
        List<T> result = new ArrayList<>();
        for (Object item : list) {
            try {
                if (clazz.isInstance(item)) {
                    result.add(clazz.cast(item));
                } else {
                    T parsed = objectMapper.convertValue(item, clazz);
                    result.add(parsed);
                }
            } catch (Exception e) {
                log.warn("Failed to convert item to {}: {}", clazz.getSimpleName(), e.getMessage());
            }
        }
        return result;
    }

    private List<String> parseStringList(Object obj) {
        if (!(obj instanceof List<?> list)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) result.add(item.toString());
        }
        return result;
    }
}
