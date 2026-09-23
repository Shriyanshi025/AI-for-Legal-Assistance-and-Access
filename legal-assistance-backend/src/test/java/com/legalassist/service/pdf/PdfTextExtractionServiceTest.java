package com.legalassist.service.pdf;

import com.legalassist.exception.PdfExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractionServiceTest {

    private PdfTextExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new PdfTextExtractionServiceImpl();
    }

    @Test
    @DisplayName("extractText should extract expected text from a valid single-page PDF")
    void extractTextFromSinglePagePdf() throws IOException {
        byte[] pdfBytes = createPdfWithPages("Clause 1: Confidentiality Agreement");

        PdfExtractionResult result = extractionService.extractText(pdfBytes);

        assertThat(result).isNotNull();
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasExtractableText()).isTrue();
        assertThat(result.fullText()).contains("Clause 1: Confidentiality Agreement");
        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(result.pages().get(0).text()).contains("Clause 1: Confidentiality Agreement");
    }

    @Test
    @DisplayName("extractText should extract text from a multi-page PDF preserving page boundaries")
    void extractTextFromMultiPagePdf() throws IOException {
        byte[] pdfBytes = createPdfWithPages(
                "Page 1: Overview of Terms",
                "Page 2: Liability and Indemnification",
                "Page 3: Governing Law and Jurisdiction"
        );

        PdfExtractionResult result = extractionService.extractText(pdfBytes);

        assertThat(result).isNotNull();
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasExtractableText()).isTrue();
        assertThat(result.pages()).hasSize(3);

        assertThat(result.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(result.pages().get(0).text()).contains("Overview of Terms");

        assertThat(result.pages().get(1).pageNumber()).isEqualTo(2);
        assertThat(result.pages().get(1).text()).contains("Liability and Indemnification");

        assertThat(result.pages().get(2).pageNumber()).isEqualTo(3);
        assertThat(result.pages().get(2).text()).contains("Governing Law");
    }

    @Test
    @DisplayName("extractText should handle PDF with no extractable text (image-only or blank PDF)")
    void extractTextFromBlankPdf() throws IOException {
        byte[] pdfBytes = createBlankPdf(2);

        PdfExtractionResult result = extractionService.extractText(pdfBytes);

        assertThat(result).isNotNull();
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasExtractableText()).isFalse();
        assertThat(result.fullText()).isBlank();
    }

    @Test
    @DisplayName("extractText should throw PdfExtractionException when input is null or empty")
    void extractTextShouldThrowOnEmptyInput() {
        assertThatThrownBy(() -> extractionService.extractText(null))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("empty or null");

        assertThatThrownBy(() -> extractionService.extractText(new byte[0]))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("empty or null");
    }

    @Test
    @DisplayName("extractText should throw PdfExtractionException when file is not a valid PDF")
    void extractTextShouldThrowOnNonPdfContent() {
        byte[] nonPdfBytes = "This is a plain text file, not a PDF".getBytes();

        assertThatThrownBy(() -> extractionService.extractText(nonPdfBytes))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("not a valid PDF");
    }

    @Test
    @DisplayName("extractText should throw PdfExtractionException when PDF is corrupt")
    void extractTextShouldThrowOnCorruptPdf() {
        byte[] corruptPdf = "%PDF-1.4 Corrupt payload bytes...".getBytes();

        assertThatThrownBy(() -> extractionService.extractText(corruptPdf))
                .isInstanceOf(PdfExtractionException.class);
    }

    private byte[] createPdfWithPages(String... pageTexts) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                    contents.beginText();
                    contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    contents.newLineAtOffset(50, 700);
                    contents.showText(text);
                    contents.endText();
                }
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private byte[] createBlankPdf(int pageCount) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }
}
