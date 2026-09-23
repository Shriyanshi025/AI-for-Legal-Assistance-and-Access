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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LegalQaServiceImpl implements LegalQaService {

    private static final Logger log = LoggerFactory.getLogger(LegalQaServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final SemanticSearchService semanticSearchService;
    private final RagContextBuilder ragContextBuilder;
    private final LegalPromptBuilder legalPromptBuilder;
    private final GenerationService generationService;
    private final CitationValidator citationValidator;
    private final GenerationProperties generationProperties;

    public LegalQaServiceImpl(
            DocumentRepository documentRepository,
            SemanticSearchService semanticSearchService,
            RagContextBuilder ragContextBuilder,
            LegalPromptBuilder legalPromptBuilder,
            GenerationService generationService,
            CitationValidator citationValidator,
            GenerationProperties generationProperties
    ) {
        this.documentRepository = documentRepository;
        this.semanticSearchService = semanticSearchService;
        this.ragContextBuilder = ragContextBuilder;
        this.legalPromptBuilder = legalPromptBuilder;
        this.generationService = generationService;
        this.citationValidator = citationValidator;
        this.generationProperties = generationProperties;
    }

    @Override
    public LegalAnswerResponse askQuestion(UUID documentId, LegalAskRequest request) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("Question cannot be null or blank");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new IllegalStateException("Document is not ready for Q&A. Current status: " + document.getStatus());
        }

        String question = request.question().trim();
        int topK = generationProperties.getTopK();
        double minSimilarity = generationProperties.getMinSimilarity();
        int maxContextChars = generationProperties.getMaxContextChars();

        List<SimilaritySearchResultResponse> chunks = semanticSearchService.searchSimilarChunks(documentId, question, topK);

        if (chunks == null || chunks.isEmpty()) {
            log.info("Zero vector search results for document {} question: '{}'. Returning insufficient context.", documentId, question);
            return new LegalAnswerResponse(CitationValidator.INSUFFICIENT_CONTEXT_ANSWER, false, List.of());
        }

        RagContext ragContext = ragContextBuilder.buildContext(chunks, minSimilarity, maxContextChars);

        if (ragContext.isEmpty()) {
            log.info("Zero chunks passed min-similarity gate ({}) for document {} question: '{}'. Gemini will NOT be invoked.",
                    minSimilarity, documentId, question);
            return new LegalAnswerResponse(CitationValidator.INSUFFICIENT_CONTEXT_ANSWER, false, List.of());
        }

        String systemInstruction = legalPromptBuilder.buildSystemInstruction();
        String userPrompt = legalPromptBuilder.buildUserPrompt(ragContext, question);

        log.info("Invoking Gemini generation for document {} with {} context chunks", documentId, ragContext.items().size());
        GeminiGenerationResponse rawResponse = generationService.generateAnswer(systemInstruction, userPrompt);

        return citationValidator.validateAndBuildResponse(rawResponse, ragContext);
    }
}
