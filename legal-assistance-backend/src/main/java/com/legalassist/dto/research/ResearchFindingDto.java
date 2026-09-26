package com.legalassist.dto.research;

import java.util.List;

public record ResearchFindingDto(
        String id,
        String title,
        String statement,
        String supportStatus, // SUPPORTED, PARTIALLY_SUPPORTED, CONFLICTING, INSUFFICIENT_EVIDENCE
        String evidenceType,  // DIRECT, INDIRECT, INFERRED, MISSING
        List<String> citations
) {
}
