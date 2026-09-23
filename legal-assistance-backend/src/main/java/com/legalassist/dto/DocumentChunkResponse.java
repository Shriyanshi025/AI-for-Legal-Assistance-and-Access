package com.legalassist.dto;

import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        UUID documentId,
        Integer pageNumber,
        String section,
        String clause,
        String content,
        Integer chunkIndex
) {
}
