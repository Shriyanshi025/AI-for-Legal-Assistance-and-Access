package com.legalassist.repository;

import com.legalassist.entity.Document;
import com.legalassist.entity.DocumentChunk;
import com.legalassist.entity.DocumentPage;
import com.legalassist.entity.DocumentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryInterfaceTest {

    @Test
    @DisplayName("DocumentRepository interface should extend JpaRepository and define derived query methods")
    void verifyDocumentRepositorySignatures() throws NoSuchMethodException {
        assertThat(JpaRepository.class).isAssignableFrom(DocumentRepository.class);

        Method findByUserId = DocumentRepository.class.getMethod("findByUserId", UUID.class);
        assertThat(findByUserId.getReturnType()).isEqualTo(List.class);

        Method findByStatus = DocumentRepository.class.getMethod("findByStatus", DocumentStatus.class);
        assertThat(findByStatus.getReturnType()).isEqualTo(List.class);
    }

    @Test
    @DisplayName("DocumentPageRepository interface should extend JpaRepository and define derived query methods")
    void verifyDocumentPageRepositorySignatures() throws NoSuchMethodException {
        assertThat(JpaRepository.class).isAssignableFrom(DocumentPageRepository.class);

        Method findByDocId = DocumentPageRepository.class.getMethod("findByDocumentIdOrderByPageNumberAsc", UUID.class);
        assertThat(findByDocId.getReturnType()).isEqualTo(List.class);
    }

    @Test
    @DisplayName("DocumentChunkRepository interface should extend JpaRepository and define derived query methods")
    void verifyDocumentChunkRepositorySignatures() throws NoSuchMethodException {
        assertThat(JpaRepository.class).isAssignableFrom(DocumentChunkRepository.class);

        Method findByChunkIndex = DocumentChunkRepository.class.getMethod("findByDocumentIdOrderByChunkIndexAsc", UUID.class);
        assertThat(findByChunkIndex.getReturnType()).isEqualTo(List.class);

        Method findByDocAndPage = DocumentChunkRepository.class.getMethod("findByDocumentIdAndPageNumber", UUID.class, Integer.class);
        assertThat(findByDocAndPage.getReturnType()).isEqualTo(List.class);
    }
}
