package com.legalassist.dto.research;

import com.legalassist.dto.CitationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LegalResearchResponse(
        UUID id,
        String researchQuestion,
        String researchType,
        String jurisdiction,
        String relevantDate,
        List<UUID> selectedDocumentIds,
        List<ResearchIssueDto> issues,
        List<ResearchFindingDto> findings,
        List<EvidenceMatrixItemDto> evidenceMatrix,
        List<ResearchConflictDto> conflicts,
        List<EvidenceGapDto> evidenceGaps,
        List<String> followUpQuestions,
        List<CitationResponse> sources,
        LocalDateTime createdAt
) {
}
