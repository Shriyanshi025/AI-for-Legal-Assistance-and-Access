package com.legalassist.service.pdf;

import com.legalassist.exception.PdfExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfTextExtractionServiceImpl implements PdfTextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractionServiceImpl.class);
    private static final byte[] PDF_HEADER = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    @Override
    public PdfExtractionResult extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new PdfExtractionException("PDF content is empty or null");
        }

        if (!hasPdfHeader(pdfBytes)) {
            throw new PdfExtractionException("Provided file is not a valid PDF document");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new PdfExtractionException("Encrypted or password-protected PDF documents are not supported");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new PdfExtractionException("PDF document contains no pages");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedPage> pages = new ArrayList<>(pageCount);

            for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                String pageText = stripper.getText(document).trim();
                pages.add(new ExtractedPage(pageNo, pageText));
            }

            String fullText = pages.stream()
                    .map(ExtractedPage::text)
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.joining("\n\n"));

            boolean hasExtractableText = !fullText.isBlank();
            if (!hasExtractableText) {
                log.info("PDF document contains {} pages but no machine-readable text was found (scanned or image-only PDF)", pageCount);
            } else {
                log.info("Successfully extracted text from {} pages of PDF document", pageCount);
            }

            return new PdfExtractionResult(pageCount, pages, fullText, hasExtractableText);
        } catch (PdfExtractionException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to parse PDF document content", e);
            throw new PdfExtractionException("Failed to extract text from PDF document: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during PDF text extraction", e);
            throw new PdfExtractionException("An unexpected error occurred during PDF text extraction", e);
        }
    }

    private boolean hasPdfHeader(byte[] bytes) {
        if (bytes.length < PDF_HEADER.length) {
            return false;
        }
        for (int i = 0; i < PDF_HEADER.length; i++) {
            if (bytes[i] != PDF_HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
