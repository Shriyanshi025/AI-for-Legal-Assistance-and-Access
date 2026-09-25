package com.legalassist.service.search;

import com.legalassist.dto.SimilaritySearchResultResponse;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchService {

    List<SimilaritySearchResultResponse> searchSimilarChunks(UUID documentId, String query, int topK);

    List<SimilaritySearchResultResponse> searchSimilarChunksForDocuments(List<UUID> documentIds, String query, int topK);
}
