package com.legalassist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record LegalAskRequest(
        @NotBlank(message = "Question must not be blank")
        @Size(max = 1000, message = "Question must not exceed 1000 characters")
        String question,
        List<UUID> documentIds
) {
    public LegalAskRequest(String question) {
        this(question, null);
    }
}
