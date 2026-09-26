package com.legalassist.dto.research;

import java.util.List;

public record ResearchConflictDto(
        String id,
        String topic,
        String sourceAExcerpt,
        String sourceBExcerpt,
        String analysis,
        String resolutionStatus, // UNRESOLVED, RESOLVED_BY_SOURCE, INSUFFICIENT_INFORMATION
        List<String> citations
) {
}
