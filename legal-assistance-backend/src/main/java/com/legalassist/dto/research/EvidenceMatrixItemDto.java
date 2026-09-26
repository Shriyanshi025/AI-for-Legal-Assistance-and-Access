package com.legalassist.dto.research;

public record EvidenceMatrixItemDto(
        String findingTitle,
        String sourceDocument,
        Integer pageNumber,
        String evidenceType,
        String supportStatus
) {
}
