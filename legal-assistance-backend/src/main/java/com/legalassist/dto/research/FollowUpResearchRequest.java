package com.legalassist.dto.research;

import jakarta.validation.constraints.NotBlank;

public record FollowUpResearchRequest(
        @NotBlank(message = "Follow-up question cannot be blank")
        String question
) {
}
