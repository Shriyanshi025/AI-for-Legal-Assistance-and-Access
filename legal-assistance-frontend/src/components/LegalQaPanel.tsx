import React, { useState } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse, LegalAnswerResponse } from '../types/document';
import { AnswerDisplay } from './AnswerDisplay';

interface LegalQaPanelProps {
  document: DocumentSummaryResponse | DocumentResponse;
}

export const LegalQaPanel: React.FC<LegalQaPanelProps> = ({ document }) => {
  const [question, setQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [answerResponse, setAnswerResponse] = useState<LegalAnswerResponse | null>(null);

  const isReady = document.status === 'READY';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

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

  return (
    <div className="glass-panel">
      <div className="selected-doc-header">
        <div>
          <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '1.2rem', fontWeight: 600 }}>
            {document.filename}
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            ID: {document.id}
          </p>
        </div>
      </div>

      {!isReady ? (
        <div className="empty-state" style={{ padding: '2rem 1rem' }}>
          ⚠️ This document is not ready for Q&A yet. Click <strong>"Prepare Document for Q&A"</strong> in the sidebar to process text and embeddings.
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="qa-input-box" style={{ marginTop: '1.25rem' }}>
          <label style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
            Ask a Legal Question
          </label>

          <textarea
            className="qa-textarea"
            placeholder="e.g., What is the notice period required for contract termination?"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            maxLength={1000}
            disabled={isLoading}
          />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {question.length} / 1000 characters
            </span>

            <button
              type="submit"
              className="btn-primary"
              style={{ width: 'auto', padding: '0.6rem 1.5rem', marginTop: 0 }}
              disabled={isLoading || !question.trim()}
            >
              {isLoading ? (
                <>
                  <span className="spinner"></span>
                  <span>Analyzing & Generating Answer...</span>
                </>
              ) : (
                <>
                  <span>🔍</span> Ask Legal Assistant
                </>
              )}
            </button>
          </div>

          {error && (
            <div style={{ color: 'var(--status-failed)', fontSize: '0.85rem', marginTop: '0.5rem' }}>
              ⚠️ {error}
            </div>
          )}
        </form>
      )}

      {answerResponse && (
        <div style={{ marginTop: '1.5rem' }}>
          <AnswerDisplay response={answerResponse} />
        </div>
      )}
    </div>
  );
};
