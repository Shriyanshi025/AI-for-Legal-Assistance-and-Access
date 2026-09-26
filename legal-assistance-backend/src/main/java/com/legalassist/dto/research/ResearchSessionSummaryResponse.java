package com.legalassist.dto.research;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResearchSessionSummaryResponse(
        UUID id,
        String researchQuestion,
        String researchType,
        int documentCount,
        int findingsCount,
        int gapsCount,
        int conflictsCount,
        LocalDateTime createdAt
) {
}
