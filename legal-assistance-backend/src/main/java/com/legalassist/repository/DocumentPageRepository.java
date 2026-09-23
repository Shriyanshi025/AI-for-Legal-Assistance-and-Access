package com.legalassist.repository;

import com.legalassist.entity.DocumentPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentPageRepository extends JpaRepository<DocumentPage, UUID> {
    List<DocumentPage> findByDocumentIdOrderByPageNumberAsc(UUID documentId);
}
