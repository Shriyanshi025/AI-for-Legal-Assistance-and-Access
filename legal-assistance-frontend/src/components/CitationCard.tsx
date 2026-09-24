import React from 'react';
import type { CitationResponse } from '../types/document';

interface CitationCardProps {
  citation: CitationResponse;
  index: number;
  onSelectCitation?: (citation: CitationResponse) => void;
}

export const CitationCard: React.FC<CitationCardProps> = ({ citation, index, onSelectCitation }) => {
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
      aria-label={`Open source on page ${citation.pageNumber}`}
      style={{ cursor: 'pointer', transition: 'all 0.2s ease' }}
    >
      <div className="citation-item-header">
        <span className="badge-page">Page {citation.pageNumber}</span>
        <span className="badge-chunk">Chunk #{citation.chunkIndex}</span>
        <span style={{ color: 'var(--text-subtle)', marginLeft: 'auto', fontSize: '0.68rem', fontWeight: 400 }}>
          Ref #{index + 1}
        </span>
        <button
          type="button"
          className="citation-view-source-btn"
          onClick={(e) => {
            e.stopPropagation();
            handleClick();
          }}
          title={`View source context on Page ${citation.pageNumber}`}
          aria-label={`View source context on Page ${citation.pageNumber}`}
          style={{
            marginLeft: '8px',
            padding: '2px 8px',
            borderRadius: '12px',
            border: '1px solid var(--border-color)',
            background: 'var(--bg-card)',
            color: 'var(--primary-blue)',
            fontSize: '0.7rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          👁️ View Source ↗
        </button>
      </div>
      <div className="citation-item-text">
        "{citation.excerpt}"
      </div>
    </div>
  );
};
