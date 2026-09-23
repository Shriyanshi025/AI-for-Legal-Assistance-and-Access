package com.legalassist.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentPageResponse(
        UUID id,
        UUID documentId,
        Integer pageNumber,
        String content,
        Instant createdAt
) {
}
