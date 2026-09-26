package com.legalassist.service.search;

import com.legalassist.dto.SimilaritySearchResultResponse;
import com.legalassist.exception.DocumentNotFoundException;
import com.legalassist.repository.ChunkSimilarityProjection;
import com.legalassist.repository.DocumentChunkRepository;
import com.legalassist.repository.DocumentRepository;
import com.legalassist.service.embedding.EmbeddingService;
import com.legalassist.service.embedding.EmbeddingTaskType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private SemanticSearchServiceImpl semanticSearchService;

    @Test
    @DisplayName("searchSimilarChunks should execute search using RETRIEVAL_QUERY taskType and return mapped DTOs")
    void searchSimilarChunksSuccess() {
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(embeddingService.generateEmbedding("termination clause", EmbeddingTaskType.RETRIEVAL_QUERY))
                .thenReturn(List.of(0.6f, 0.8f));

        ChunkSimilarityProjection projection = new ChunkSimilarityProjection() {
            @Override
            public UUID getId() { return chunkId; }
            @Override
            public UUID getDocumentId() { return docId; }
            @Override
            public Integer getPageNumber() { return 3; }
            @Override
            public Integer getChunkIndex() { return 2; }
            @Override
            public String getContent() { return "Section 5: Termination conditions"; }
            @Override
            public String getSection() { return "5"; }
            @Override
            public String getClause() { return "A"; }
            @Override
            public Double getSimilarityScore() { return 0.92; }
        };

        when(documentChunkRepository.findSimilarChunks(eq(docId), anyString(), eq(3)))
                .thenReturn(List.of(projection));

        List<SimilaritySearchResultResponse> results = semanticSearchService.searchSimilarChunks(docId, "termination clause", 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkId()).isEqualTo(chunkId);
        assertThat(results.get(0).documentId()).isEqualTo(docId);
        assertThat(results.get(0).pageNumber()).isEqualTo(3);
        assertThat(results.get(0).similarityScore()).isEqualTo(0.92);
        assertThat(results.get(0).content()).isEqualTo("Section 5: Termination conditions");

        verify(embeddingService).generateEmbedding("termination clause", EmbeddingTaskType.RETRIEVAL_QUERY);
        verify(documentChunkRepository).findSimilarChunks(eq(docId), anyString(), eq(3));
    }

    @Test
    @DisplayName("searchSimilarChunks should throw DocumentNotFoundException when document does not exist")
    void searchSimilarChunksShouldThrowWhenDocumentNotFound() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(false);

        assertThatThrownBy(() -> semanticSearchService.searchSimilarChunks(docId, "query", 5))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("searchSimilarChunks should throw IllegalArgumentException when query is blank or topK is invalid")
    void searchSimilarChunksShouldValidateInputs() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.existsById(docId)).thenReturn(true);

        assertThatThrownBy(() -> semanticSearchService.searchSimilarChunks(docId, "   ", 5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> semanticSearchService.searchSimilarChunks(docId, "query", 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> semanticSearchService.searchSimilarChunks(docId, "query", 150))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
