package com.legalassist.service.pdf;

import java.util.List;

public record PdfExtractionResult(
        int totalPages,
        List<ExtractedPage> pages,
        String fullText,
        boolean hasExtractableText
) {
}
