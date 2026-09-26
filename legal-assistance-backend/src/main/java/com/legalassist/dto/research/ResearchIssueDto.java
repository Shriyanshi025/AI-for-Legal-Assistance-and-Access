package com.legalassist.dto.research;

import java.util.List;

public record ResearchIssueDto(
        String id,
        String title,
        String description,
        List<String> relatedCitations
) {
}
