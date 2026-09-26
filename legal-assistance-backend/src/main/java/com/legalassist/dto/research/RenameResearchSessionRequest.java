package com.legalassist.dto.research;

import jakarta.validation.constraints.NotBlank;

public record RenameResearchSessionRequest(
        @NotBlank(message = "Title cannot be blank")
        String title
) {
}
