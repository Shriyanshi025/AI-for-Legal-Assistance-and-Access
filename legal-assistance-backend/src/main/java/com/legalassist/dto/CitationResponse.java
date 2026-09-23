package com.legalassist.dto;

public record CitationResponse(
        Integer pageNumber,
        Integer chunkIndex,
        String excerpt
) {}
