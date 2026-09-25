import React from 'react';
import { documentApi } from '../api/documentApi';
import type { CitationResponse } from '../types/document';

interface CitationCardProps {
  citation: CitationResponse;
  index: number;
  onSelectCitation?: (citation: CitationResponse) => void;
  activeDocumentId?: string;
}

export const CitationCard: React.FC<CitationCardProps> = ({
  citation,
  index,
  onSelectCitation,
  activeDocumentId,
}) => {
  const targetDocId = citation.documentId || activeDocumentId;
  const pdfViewUrl = targetDocId
    ? `${documentApi.getDocumentViewUrl(targetDocId)}#page=${citation.pageNumber}`
    : '#';

  const handleClick = () => {
    if (onSelectCitation) {
      onSelectCitation(citation);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  return (
    <div
      className="citation-item interactive-citation-card"
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`Citation reference ${index + 1} on page ${citation.pageNumber}`}
    >
      <div className="citation-item-header">
        <span className="badge-page">Page {citation.pageNumber}</span>
        <span className="badge-chunk">Chunk #{citation.chunkIndex}</span>
        <span className="badge-ref">Ref #{index + 1}</span>

        {/* Visually grouped citation actions */}
        <div className="citation-actions-group" onClick={(e) => e.stopPropagation()}>
          <button
            type="button"
            className="citation-btn citation-view-source-btn"
            onClick={handleClick}
            title={`View in-app source context for Page ${citation.pageNumber}`}
            aria-label={`View source context for Page ${citation.pageNumber}`}
          >
            👁️ View Source
          </button>

          {targetDocId && (
            <a
              href={pdfViewUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="citation-btn citation-open-newtab-btn"
              title={`Open PDF document in a new browser tab on Page ${citation.pageNumber}`}
              aria-label={`Open PDF in new tab on Page ${citation.pageNumber}`}
            >
              ↗ Open in New Tab
            </a>
          )}
        </div>
      </div>
      <div className="citation-item-text">
        "{citation.excerpt}"
      </div>
    </div>
  );
};
