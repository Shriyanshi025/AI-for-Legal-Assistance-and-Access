package com.legalassist.service.pdf;

public interface PdfTextExtractionService {

    /**
     * Extracts page-aware text from a PDF byte array.
     *
     * @param pdfBytes byte array of the PDF document
     * @return PdfExtractionResult containing page details and full text
     */
    PdfExtractionResult extractText(byte[] pdfBytes);
}
