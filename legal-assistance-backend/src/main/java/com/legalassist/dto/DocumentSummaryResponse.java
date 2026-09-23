package com.legalassist.dto;

import com.legalassist.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryResponse(
        UUID id,
        String filename,
        String documentType,
        Long fileSize,
        DocumentStatus status,
        Instant createdAt
) {
}
