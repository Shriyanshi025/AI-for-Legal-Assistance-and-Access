package com.legalassist.service.chunking;

import com.legalassist.entity.DocumentPage;

import java.util.List;

public interface TextChunkingService {

    /**
     * Splits extracted document pages into deterministic text chunks.
     *
     * @param pages        list of document pages ordered by page number
     * @param maxChunkSize maximum character size per chunk
     * @param overlapSize  character overlap size between consecutive chunks
     * @return list of generated raw chunks
     */
    List<RawChunk> createChunks(List<DocumentPage> pages, int maxChunkSize, int overlapSize);
}
