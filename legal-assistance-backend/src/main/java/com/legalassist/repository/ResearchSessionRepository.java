package com.legalassist.repository;

import com.legalassist.entity.ResearchSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchSessionRepository extends JpaRepository<ResearchSession, UUID> {

    List<ResearchSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ResearchSession> findByIdAndUserId(UUID id, UUID userId);
}
