package com.legalassist.repository;

import com.legalassist.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    List<DocumentChunk> findByDocumentIdAndPageNumber(UUID documentId, Integer pageNumber);
    void deleteByDocumentId(UUID documentId);

    @Modifying
    @Query(value = "UPDATE document_chunks SET embedding = CAST(:embeddingStr AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embeddingStr") String embeddingStr);

    @Query(value = """
        SELECT c.id AS id,
               c.document_id AS documentId,
               c.page_number AS pageNumber,
               c.chunk_index AS chunkIndex,
               c.content AS content,
               c.section AS section,
               c.clause AS clause,
               (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) AS similarityScore
        FROM document_chunks c
        WHERE c.document_id = :documentId
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("documentId") UUID documentId,
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );

    @Query(value = """
        SELECT c.id AS id,
               c.document_id AS documentId,
               c.page_number AS pageNumber,
               c.chunk_index AS chunkIndex,
               c.content AS content,
               c.section AS section,
               c.clause AS clause,
               (1.0 - (c.embedding <=> CAST(:queryVector AS vector))) AS similarityScore
        FROM document_chunks c
        WHERE c.document_id IN (:documentIds)
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunksForDocuments(
            @Param("documentIds") List<UUID> documentIds,
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );
}
