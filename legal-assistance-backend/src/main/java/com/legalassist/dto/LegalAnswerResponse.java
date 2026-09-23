package com.legalassist.dto;

import java.util.List;

public record LegalAnswerResponse(
        String answer,
        boolean grounded,
        List<CitationResponse> citations
) {}
