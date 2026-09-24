import React, { useState } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse, LegalAnswerResponse, CitationResponse } from '../types/document';
import { DocumentStatus } from './DocumentStatus';
import { AnswerDisplay } from './AnswerDisplay';
import { CitationViewerModal } from './CitationViewerModal';

interface LegalQaPanelProps {
  document: DocumentSummaryResponse | DocumentResponse | null;
  documentsList: DocumentSummaryResponse[];
  onSelectDocument: (doc: DocumentSummaryResponse) => void;
}

export const LegalQaPanel: React.FC<LegalQaPanelProps> = ({
  document,
  documentsList,
  onSelectDocument,
}) => {
  const [question, setQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [answerResponse, setAnswerResponse] = useState<LegalAnswerResponse | null>(null);
  const [activeCitation, setActiveCitation] = useState<CitationResponse | null>(null);

  const isReady = document?.status === 'READY';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!document || !question.trim()) return;

    if (question.length > 1000) {
      setError('Question must not exceed 1000 characters.');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      const res = await documentApi.askQuestion(document.id, question.trim());
      setAnswerResponse(res);
    } catch (err: any) {
      setError(err.message || 'Failed to generate answer. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectCitation = (citation: CitationResponse) => {
    setActiveCitation(citation);
  };

  const targetDocId = activeCitation?.documentId || document?.id;

  return (
    <div className="content-card qa-panel">
      <div>
        <h3 className="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          Legal Q&A
        </h3>
        <p className="section-subtitle">
          Ask questions about your document and get accurate, grounded answers with citations.
        </p>
      </div>

      {/* Document Selector Dropdown / Empty State */}
      {!document ? (
        <div className="doc-selector-box" style={{ padding: '12px 14px', flexDirection: 'column', alignItems: 'flex-start', gap: '2px' }}>
          <div style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--text-heading)' }}>
            No Document Selected
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Upload a PDF document above to start legal Q&A.
          </div>
        </div>
      ) : (
        <div className="doc-selector-box">
          <div className="doc-selector-info">
            <div className="pdf-icon-badge" style={{ width: '20px', height: '20px', fontSize: '0.55rem' }}>pdf</div>
            <span>{document.filename}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <DocumentStatus status={document.status} />
            {documentsList.length > 1 && (
              <select
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: 'var(--text-subtle)',
                  fontSize: '0.8rem',
                  cursor: 'pointer',
                  outline: 'none'
                }}
                value={document.id}
                onChange={(e) => {
                  const found = documentsList.find((d) => d.id === e.target.value);
                  if (found) onSelectDocument(found);
                }}
              >
                {documentsList.map((d) => (
                  <option key={d.id} value={d.id}>{d.filename}</option>
                ))}
              </select>
            )}
          </div>
        </div>
      )}

      {/* Question Area */}
      <form onSubmit={handleSubmit} className="question-form">
        <label className="form-label">Your Question</label>
        <textarea
          className="question-textarea"
          placeholder={!document ? "Please upload and select a PDF document first." : "e.g. What are the key obligations in this contract?"}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          maxLength={1000}
          disabled={isLoading || !isReady}
        />
        <div className="char-counter">{question.length}/1000</div>

        <button
          type="submit"
          className="ask-btn"
          disabled={isLoading || !question.trim() || !isReady}
        >
          {isLoading ? (
            <>
              <span className="spinner"></span>
              <span>Generating Answer...</span>
            </>
          ) : (
            <>
              <span>✨</span> Ask Question
            </>
          )}
        </button>

        {error && (
          <div style={{ color: 'var(--status-failed-text)', fontSize: '0.78rem', marginTop: '4px', fontWeight: 500 }}>
            ⚠️ {error}
          </div>
        )}
      </form>

      {/* Answer & Citations Cards */}
      <AnswerDisplay
        response={answerResponse}
        onSelectCitation={handleSelectCitation}
      />

      {/* Citation Source Navigation Viewer Modal */}
      {activeCitation && targetDocId && (
        <CitationViewerModal
          documentId={targetDocId}
          documentFilename={document?.filename || 'Legal Document'}
          citation={activeCitation}
          onClose={() => setActiveCitation(null)}
        />
      )}
    </div>
  );
};
