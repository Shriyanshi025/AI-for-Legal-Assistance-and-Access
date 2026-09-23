package com.legalassist.dto;

import java.util.UUID;

public record SimilaritySearchResultResponse(
        UUID chunkId,
        UUID documentId,
        Integer pageNumber,
        Integer chunkIndex,
        String content,
        String section,
        String clause,
        Double similarityScore
) {
}
