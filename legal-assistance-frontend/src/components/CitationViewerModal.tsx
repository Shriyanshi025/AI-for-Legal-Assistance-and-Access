import React, { useState, useEffect } from 'react';
import { documentApi } from '../api/documentApi';
import type { CitationResponse, DocumentPageResponse } from '../types/document';

interface CitationViewerModalProps {
  documentId: string;
  documentFilename?: string;
  citation: CitationResponse;
  onClose: () => void;
}

export const CitationViewerModal: React.FC<CitationViewerModalProps> = ({
  documentId,
  documentFilename = 'Legal Document',
  citation,
  onClose,
}) => {
  const [activeTab, setActiveTab] = useState<'context' | 'pdf'>('context');
  const [currentPageNum, setCurrentPageNum] = useState<number>(citation.pageNumber || 1);
  const [pages, setPages] = useState<DocumentPageResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setError(null);

    documentApi.getDocumentPages(documentId)
      .then((data) => {
        if (isMounted) {
          setPages(data || []);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err.message || 'Unable to open the source document.');
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [documentId]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const pdfUrl = `${documentApi.getDocumentViewUrl(documentId)}#page=${currentPageNum}`;
  const totalPages = pages.length;
  const currentPageObj = pages.find((p) => p.pageNumber === currentPageNum);
  const pageContent = currentPageObj?.content || '';

  // Highlight citation excerpt in context text
  const renderHighlightedContent = () => {
    if (!pageContent) {
      return (
        <div style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
          No text extracted for page {currentPageNum}.
        </div>
      );
    }

    const excerpt = citation.excerpt ? citation.excerpt.replace(/\.\.\.$/, '').trim() : '';
    if (!excerpt || excerpt.length < 5) {
      return <div>{pageContent}</div>;
    }

    // Attempt substring matching (case insensitive)
    const lowerContent = pageContent.toLowerCase();
    const lowerExcerpt = excerpt.toLowerCase();
    const matchIdx = lowerContent.indexOf(lowerExcerpt);

    if (matchIdx !== -1) {
      const before = pageContent.substring(0, matchIdx);
      const matchText = pageContent.substring(matchIdx, matchIdx + excerpt.length);
      const after = pageContent.substring(matchIdx + excerpt.length);

      return (
        <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.6', fontSize: '0.88rem' }}>
          {before}
          <mark className="cited-excerpt-highlight">{matchText}</mark>
          {after}
        </div>
      );
    }

    // Fallback: render full page content
    return (
      <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.6', fontSize: '0.88rem' }}>
        {pageContent}
      </div>
    );
  };

  return (
    <div className="modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-label={`Source citation view for page ${currentPageNum}`}>
      <div
        className="modal-card citation-viewer-modal"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '900px', width: '92%', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}
      >
        {/* Modal Header */}
        <div className="modal-header" style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '1.2rem' }}>📜</span>
            <div>
              <div style={{ fontWeight: 600, fontSize: '0.98rem', color: 'var(--text-heading)' }}>
                {documentFilename}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Citation Target: Page {citation.pageNumber} • Chunk #{citation.chunkIndex}
              </div>
            </div>
          </div>
          <button
            className="btn-icon"
            onClick={onClose}
            aria-label="Close source view"
            style={{ background: 'transparent', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: 'var(--text-muted)' }}
          >
            ✕
          </button>
        </div>

        {/* Navigation & Tab Bar */}
        <div style={{ padding: '12px 20px', background: 'var(--bg-subcard)', borderBottom: '1px solid var(--border-color)', display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
          {/* View Toggle */}
          <div style={{ display: 'flex', gap: '6px' }}>
            <button
              onClick={() => setActiveTab('context')}
              style={{
                padding: '6px 14px',
                borderRadius: 'var(--radius-pill)',
                border: '1px solid',
                borderColor: activeTab === 'context' ? 'var(--primary-blue)' : 'var(--border-color)',
                background: activeTab === 'context' ? 'var(--light-blue-bg)' : 'var(--bg-card)',
                color: activeTab === 'context' ? 'var(--primary-blue-hover)' : 'var(--text-main)',
                fontSize: '0.78rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🔍 Citation Context & Highlight
            </button>
            <button
              onClick={() => setActiveTab('pdf')}
              style={{
                padding: '6px 14px',
                borderRadius: 'var(--radius-pill)',
                border: '1px solid',
                borderColor: activeTab === 'pdf' ? 'var(--primary-blue)' : 'var(--border-color)',
                background: activeTab === 'pdf' ? 'var(--light-blue-bg)' : 'var(--bg-card)',
                color: activeTab === 'pdf' ? 'var(--primary-blue-hover)' : 'var(--text-main)',
                fontSize: '0.78rem',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              📑 Native PDF Page
            </button>
          </div>

          {/* Page Pagination */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              disabled={currentPageNum <= 1}
              onClick={() => setCurrentPageNum((prev) => Math.max(1, prev - 1))}
              aria-label="Previous Page"
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                background: 'var(--bg-card)',
                fontSize: '0.75rem',
                cursor: currentPageNum <= 1 ? 'not-allowed' : 'pointer',
                opacity: currentPageNum <= 1 ? 0.5 : 1,
              }}
            >
              ◀ Prev
            </button>
            <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)' }}>
              Page {currentPageNum} {totalPages > 0 ? `of ${totalPages}` : ''}
            </span>
            <button
              disabled={totalPages > 0 && currentPageNum >= totalPages}
              onClick={() => setCurrentPageNum((prev) => (totalPages > 0 ? Math.min(totalPages, prev + 1) : prev + 1))}
              aria-label="Next Page"
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                background: 'var(--bg-card)',
                fontSize: '0.75rem',
                cursor: totalPages > 0 && currentPageNum >= totalPages ? 'not-allowed' : 'pointer',
                opacity: totalPages > 0 && currentPageNum >= totalPages ? 0.5 : 1,
              }}
            >
              Next ▶
            </button>
          </div>
        </div>

        {/* Content View Container */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1, minHeight: '350px' }}>
          {isLoading ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', gap: '10px', padding: '40px 0' }}>
              <span className="spinner" style={{ width: '28px', height: '28px' }}></span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Opening source page...</span>
            </div>
          ) : error ? (
            <div style={{ padding: '20px', borderRadius: '12px', background: 'var(--status-failed-bg)', color: 'var(--status-failed-text)', fontSize: '0.85rem' }}>
              ⚠️ {error}
            </div>
          ) : activeTab === 'context' ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Citation Excerpt Card */}
              <div style={{ padding: '14px 16px', borderRadius: '12px', background: 'var(--light-blue-pill)', borderLeft: '4px solid var(--primary-blue)' }}>
                <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.5px', fontWeight: 700, color: 'var(--primary-blue)', marginBottom: '4px' }}>
                  Cited Excerpt (Page {citation.pageNumber})
                </div>
                <div style={{ fontSize: '0.9rem', fontStyle: 'italic', color: 'var(--text-heading)', fontWeight: 500 }}>
                  "{citation.excerpt}"
                </div>
              </div>

              {/* Full Page Context Card */}
              <div style={{ padding: '16px', borderRadius: '12px', border: '1px solid var(--border-color)', background: 'var(--bg-card)' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-subtle)', marginBottom: '10px', borderBottom: '1px solid var(--border-light)', paddingBottom: '6px' }}>
                  Surrounding Document Page Text (Page {currentPageNum})
                </div>
                {renderHighlightedContent()}
              </div>
            </div>
          ) : (
            <div style={{ width: '100%', height: '520px' }}>
              <iframe
                src={pdfUrl}
                title={`PDF Page ${currentPageNum}`}
                width="100%"
                height="100%"
                style={{ border: '1px solid var(--border-color)', borderRadius: '12px' }}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
