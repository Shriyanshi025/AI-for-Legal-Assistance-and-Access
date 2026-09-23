package com.legalassist.service.generation;

import java.util.List;

public record GeminiGenerationResponse(
        String answer,
        boolean grounded,
        List<String> citations
) {}
