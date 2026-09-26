import React, { useState } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse, LegalAnswerResponse, CitationResponse } from '../types/document';
import { AnswerDisplay } from './AnswerDisplay';

interface LegalQaPanelProps {
  document: DocumentSummaryResponse | DocumentResponse | null;
  documentsList: DocumentSummaryResponse[];
  selectedDocIds: Set<string>;
  onSelectDocument: (doc: DocumentSummaryResponse) => void;
  onSelectCitation?: (citation: CitationResponse) => void;
}

export const LegalQaPanel: React.FC<LegalQaPanelProps> = ({
  document,
  documentsList,
  selectedDocIds,
  onSelectDocument,
  onSelectCitation,
}) => {
  const [question, setQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [answerResponse, setAnswerResponse] = useState<LegalAnswerResponse | null>(null);

  const selectedCount = selectedDocIds.size;
  const isReadyToAsk = selectedCount > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    if (selectedCount === 0) {
      setError('Please select at least one document from the list above before asking a question.');
      return;
    }

    if (question.length > 1000) {
      setError('Question must not exceed 1000 characters.');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      const docIdsArray = Array.from(selectedDocIds);
      const res = await documentApi.askMultiDocumentQuestion(docIdsArray, question.trim());
      setAnswerResponse(res);
    } catch (err: any) {
      setError(err.message || 'Failed to generate answer. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelectCitation = (citation: CitationResponse) => {
    if (onSelectCitation) {
      onSelectCitation(citation);
    }
  };

  const targetDocId = document?.id || (selectedDocIds.size > 0 ? Array.from(selectedDocIds)[0] : undefined);

  return (
    <div className="content-card qa-panel">
      <div>
        <h3 className="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          Legal Q&A (Document-Scoped RAG)
        </h3>
        <p className="section-subtitle">
          Ask questions across your selected legal documents and receive grounded answers with citations.
        </p>
      </div>

      {/* Selected RAG Scope Box / Validation Notice */}
      {selectedCount === 0 ? (
        <div className="doc-selector-box" style={{ padding: '12px 14px', flexDirection: 'column', alignItems: 'flex-start', gap: '4px', borderColor: 'rgba(239, 68, 68, 0.4)', background: 'var(--status-failed-bg)' }}>
          <div style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--status-failed-text)', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>⚠️</span> No Document Selected
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-main)' }}>
            Please select at least one document from the document list above before asking a question.
          </div>
        </div>
      ) : (
        <div className="doc-selector-box" style={{ padding: '12px 16px', justifyContent: 'space-between' }}>
          <div className="doc-selector-info" style={{ gap: '10px' }}>
            <span style={{ fontSize: '1rem' }}>🎯</span>
            <div>
              <div style={{ fontSize: '0.86rem', fontWeight: 600, color: 'var(--text-heading)' }}>
                {selectedCount === 1 ? '1 Document Selected for RAG Scope' : `${selectedCount} Documents Selected for RAG Scope`}
              </div>
              <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
                Vector retrieval will search exclusively within these selected documents.
              </div>
            </div>
          </div>
          {document && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Active View:</span>
              <select
                style={{
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: '8px',
                  color: 'var(--text-main)',
                  fontSize: '0.78rem',
                  padding: '4px 8px',
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
            </div>
          )}
        </div>
      )}

      {/* Question Area */}
      <form onSubmit={handleSubmit} className="question-form">
        <label className="form-label">Your Question</label>
        <textarea
          id="legal-qa-textarea"
          className="question-textarea"
          placeholder={selectedCount === 0 ? "Please select at least one document from the list above." : "e.g. Compare the termination notice periods across the selected contracts."}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          maxLength={1000}
          disabled={isLoading || !isReadyToAsk}
        />
        <div className="char-counter">{question.length}/1000</div>

        <button
          type="submit"
          className="ask-btn"
          disabled={isLoading || !question.trim() || !isReadyToAsk}
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
          <div style={{
            background: 'var(--status-failed-bg)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '12px',
            padding: '12px 16px',
            marginTop: '10px',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px'
          }}>
            <div style={{ color: 'var(--status-failed-text)', fontSize: '0.82rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px', lineHeight: '1.4' }}>
              <span>⚠️</span>
              <span>{error}</span>
            </div>
            {(error.toLowerCase().includes('temporarily unavailable') || error.toLowerCase().includes('high demand') || error.toLowerCase().includes('503') || error.toLowerCase().includes('service unavailable')) && (
              <button
                type="button"
                className="ask-btn"
                style={{
                  width: 'auto',
                  alignSelf: 'flex-start',
                  padding: '6px 16px',
                  fontSize: '0.78rem',
                  marginTop: '4px'
                }}
                onClick={handleSubmit}
                disabled={isLoading}
              >
                🔄 Try Again
              </button>
            )}
          </div>
        )}
      </form>

      {/* Answer & Citations Cards */}
      <AnswerDisplay
        response={answerResponse}
        onSelectCitation={handleSelectCitation}
        activeDocumentId={targetDocId}
      />
    </div>
  );
};
