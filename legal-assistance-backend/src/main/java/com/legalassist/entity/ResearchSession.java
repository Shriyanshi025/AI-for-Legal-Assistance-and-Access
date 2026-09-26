package com.legalassist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "research_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResearchSession {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "research_question", columnDefinition = "TEXT", nullable = false)
    private String researchQuestion;

    @Column(name = "research_type")
    private String researchType;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "relevant_date")
    private String relevantDate;

    @Column(name = "document_ids_json", columnDefinition = "TEXT")
    private String documentIdsJson;

    @Column(name = "dossier_json", columnDefinition = "TEXT")
    private String dossierJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
