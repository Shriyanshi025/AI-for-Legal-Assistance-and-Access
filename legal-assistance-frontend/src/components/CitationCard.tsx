import React from 'react';
import type { CitationResponse } from '../types/document';

interface CitationCardProps {
  citation: CitationResponse;
  index: number;
}

export const CitationCard: React.FC<CitationCardProps> = ({ citation, index }) => {
  return (
    <div className="citation-card">
      <div className="citation-tags">
        <span className="tag-page">Page {citation.pageNumber}</span>
        <span className="tag-chunk">Chunk #{citation.chunkIndex}</span>
        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 'auto' }}>
          Ref #{index + 1}
        </span>
      </div>
      <div className="citation-excerpt">
        "{citation.excerpt}"
      </div>
    </div>
  );
};
