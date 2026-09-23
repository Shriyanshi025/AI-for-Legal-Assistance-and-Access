package com.legalassist.service;

import com.legalassist.dto.DocumentResponse;
import com.legalassist.dto.DocumentSummaryResponse;
import com.legalassist.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toDocumentResponse(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getDocumentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public DocumentSummaryResponse toDocumentSummaryResponse(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentSummaryResponse(
                document.getId(),
                document.getFilename(),
                document.getDocumentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    public com.legalassist.dto.DocumentPageResponse toDocumentPageResponse(com.legalassist.entity.DocumentPage page) {
        if (page == null) {
            return null;
        }
        return new com.legalassist.dto.DocumentPageResponse(
                page.getId(),
                page.getDocumentId(),
                page.getPageNumber(),
                page.getContent(),
                page.getCreatedAt()
        );
    }

    public com.legalassist.dto.DocumentChunkResponse toDocumentChunkResponse(com.legalassist.entity.DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }
        return new com.legalassist.dto.DocumentChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getPageNumber(),
                chunk.getSection(),
                chunk.getClause(),
                chunk.getContent(),
                chunk.getChunkIndex()
        );
    }
}
