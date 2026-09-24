package com.legalassist.dto;

import java.util.UUID;

public record CitationResponse(
        UUID documentId,
        Integer pageNumber,
        Integer chunkIndex,
        String excerpt
) {
    public CitationResponse(Integer pageNumber, Integer chunkIndex, String excerpt) {
        this(null, pageNumber, chunkIndex, excerpt);
    }
}
