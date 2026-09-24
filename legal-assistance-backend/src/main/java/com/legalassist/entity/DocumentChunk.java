package com.legalassist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "section")
    private String section;

    @Column(name = "clause")
    private String clause;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(read = "embedding::text", write = "?::vector")
    private String embedding;

    public DocumentChunk(UUID id, UUID documentId, Integer pageNumber, String section, String clause, String content, Integer chunkIndex) {
        this.id = id;
        this.documentId = documentId;
        this.pageNumber = pageNumber;
        this.section = section;
        this.clause = clause;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.embedding = null;
    }
}
