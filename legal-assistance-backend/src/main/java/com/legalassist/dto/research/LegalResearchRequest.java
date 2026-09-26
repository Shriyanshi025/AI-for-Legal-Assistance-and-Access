package com.legalassist.dto.research;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record LegalResearchRequest(
        @NotBlank(message = "Research question cannot be blank")
        String researchQuestion,

        @NotEmpty(message = "At least one document ID must be selected")
        List<UUID> documentIds,

        String researchType,
        String jurisdiction,
        String relevantDate
) {
}
