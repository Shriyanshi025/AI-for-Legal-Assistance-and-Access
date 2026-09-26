import React, { useState } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse } from '../types/document';

interface PipelineControlProps {
  document: DocumentSummaryResponse | DocumentResponse | null;
  onStatusUpdated: (updatedDoc: DocumentResponse) => void;
  onPrepareDocument?: () => void;
}

export const PipelineControl: React.FC<PipelineControlProps> = ({ document, onStatusUpdated, onPrepareDocument }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [stepText, setStepText] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const isReady = document?.status === 'READY';

  const handlePrepareDocument = async () => {
    if (!document) return;
    setError(null);
    setIsProcessing(true);

    if (onPrepareDocument) {
      onPrepareDocument();
    }

    try {
      const updatedDoc = await documentApi.prepareDocument(document.id, (step) => {
        switch (step) {
          case 'EXTRACTING':
            setStepText('1. Extracting text...');
            break;
          case 'CHUNKING':
            setStepText('2. Creating chunks...');
            break;
          case 'EMBEDDING':
            setStepText('3. Generating embeddings...');
            break;
          case 'COMPLETED':
            setStepText('Completed!');
            break;
        }
      });
      onStatusUpdated(updatedDoc);
    } catch (err: any) {
      setError(err.message || 'Processing failed.');
    } finally {
      setIsProcessing(false);
      setStepText('');
    }
  };

  return (
    <div className="content-card">
      <h3 className="section-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
        </svg>
        Processing Pipeline
      </h3>
      <p className="section-subtitle">Automatically extract text, create chunks and generate embeddings for better search and Q&A.</p>

      <div className="pipeline-stepper">
        <div className="pipeline-step-card">
          <div className="step-check-icon">✓</div>
          <div className="step-info">
            <div className="step-title" title="1. Extract Text">1. Extract Text</div>
            <div className="step-desc" title="Parse PDF and extract content">Parse PDF and extract content</div>
          </div>
        </div>

        <span className="pipeline-arrow">→</span>

        <div className="pipeline-step-card">
          <div className="step-check-icon">✓</div>
          <div className="step-info">
            <div className="step-title" title="2. Create Chunks">2. Create Chunks</div>
            <div className="step-desc" title="Split into semantic chunks">Split into semantic chunks</div>
          </div>
        </div>

        <span className="pipeline-arrow">→</span>

        <div className="pipeline-step-card">
          <div className="step-check-icon">✓</div>
          <div className="step-info">
            <div className="step-title" title="3. Generate Embeddings">3. Generate Embeddings</div>
            <div className="step-desc" title="Create 768-d vectors (Gemini)">Create 768-d vectors (Gemini)</div>
          </div>
        </div>

        <button
          className="prepare-btn"
          onClick={handlePrepareDocument}
          disabled={!document || isProcessing || isReady}
        >
          {isProcessing ? (
            <>
              <span className="spinner"></span>
              <span>{stepText || 'Processing...'}</span>
            </>
          ) : isReady ? (
            <>
              <span>✓</span> Document Ready
            </>
          ) : (
            <>
              <span>▶</span> Prepare Document for Q&A
            </>
          )}
        </button>
      </div>

      {error && (
        <div style={{ color: 'var(--status-failed-text)', fontSize: '0.8rem', marginTop: '8px', fontWeight: 500 }}>
          ⚠️ {error}
        </div>
      )}
    </div>
  );
};
