package com.legalassist.service.rag;

import java.util.UUID;

public record RagSourceItem(
        String sourceId,
        UUID chunkId,
        UUID documentId,
        Integer pageNumber,
        Integer chunkIndex,
        String content,
        String section,
        String clause,
        Double similarityScore
) {}
