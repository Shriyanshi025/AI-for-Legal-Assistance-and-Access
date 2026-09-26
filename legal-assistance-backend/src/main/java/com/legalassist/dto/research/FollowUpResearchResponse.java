package com.legalassist.dto.research;

import com.legalassist.dto.CitationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FollowUpResearchResponse(
        UUID sessionId,
        String question,
        String answer,
        List<CitationResponse> citations,
        LocalDateTime createdAt
) {
}
