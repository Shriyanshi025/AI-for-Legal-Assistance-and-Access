package com.legalassist.repository;

import java.util.UUID;

public interface ChunkSimilarityProjection {
    UUID getId();
    UUID getDocumentId();
    Integer getPageNumber();
    Integer getChunkIndex();
    String getContent();
    String getSection();
    String getClause();
    Double getSimilarityScore();
}
