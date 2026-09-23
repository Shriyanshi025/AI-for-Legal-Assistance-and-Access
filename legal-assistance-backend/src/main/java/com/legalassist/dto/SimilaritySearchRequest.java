package com.legalassist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SimilaritySearchRequest(
        @NotBlank(message = "Search query must not be blank")
        String query,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 100, message = "topK cannot exceed 100")
        Integer topK
) {
    public int getTopKOrDefault() {
        return (topK != null && topK > 0) ? topK : 5;
    }
}
