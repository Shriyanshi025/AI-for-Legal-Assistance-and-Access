package com.legalassist.service.chunking;

public record RawChunk(
        int chunkIndex,
        int pageNumber,
        String content,
        String section,
        String clause
) {
}
