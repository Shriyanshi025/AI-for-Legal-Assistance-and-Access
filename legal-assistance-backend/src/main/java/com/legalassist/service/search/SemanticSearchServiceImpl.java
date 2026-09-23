package com.legalassist.service.search;

import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.repository.ChunkSimilarityProjection;
import com.legalassist.repository.DocumentChunkRepository;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.service.embedding.EmbeddingService;
import com.legalassist.service.embedding.EmbeddingTaskType;
import com.legalassist.service.embedding.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    public SemanticSearchServiceImpl(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<SimilaritySearchResultResponse> searchSimilarChunks(UUID documentId, String query, int topK) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or blank");
        }

        if (topK <= 0 || topK > 100) {
            throw new IllegalArgumentException("topK must be between 1 and 100");
        }

        List<Float> queryVector = embeddingService.generateEmbedding(query, EmbeddingTaskType.RETRIEVAL_QUERY);
        String formattedVector = VectorUtils.formatPgVector(queryVector);

        List<ChunkSimilarityProjection> projections = documentChunkRepository.findSimilarChunks(
                documentId,
                formattedVector,
                topK
        );

        log.info("Found {} similar chunks for document {} with query topK={}", projections.size(), documentId, topK);

        return projections.stream()
                .map(p -> new SimilaritySearchResultResponse(
                        p.getId(),
                        p.getDocumentId(),
                        p.getPageNumber(),
                        p.getChunkIndex(),
                        p.getContent(),
                        p.getSection(),
                        p.getClause(),
                        p.getSimilarityScore()
                ))
                .toList();
    }
}
