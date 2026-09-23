import React, { useState } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse } from '../types/document';

interface PipelineControlProps {
  document: DocumentSummaryResponse | DocumentResponse;
  onStatusUpdated: (updatedDoc: DocumentResponse) => void;
}

export const PipelineControl: React.FC<PipelineControlProps> = ({ document, onStatusUpdated }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [stepText, setStepText] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const isReady = document.status === 'READY';

  const handlePrepareDocument = async () => {
    setError(null);
    setIsProcessing(true);

    try {
      const updatedDoc = await documentApi.prepareDocument(document.id, (step) => {
        switch (step) {
          case 'EXTRACTING':
            setStepText('Extracting PDF pages & text...');
            break;
          case 'CHUNKING':
            setStepText('Chunking legal clauses & sections...');
            break;
          case 'EMBEDDING':
            setStepText('Generating 768-d Gemini embeddings...');
            break;
          case 'COMPLETED':
            setStepText('Document Ready!');
            break;
        }
      });
      onStatusUpdated(updatedDoc);
    } catch (err: any) {
      setError(err.message || 'Document processing failed.');
    } finally {
      setIsProcessing(false);
      setStepText('');
    }
  };

  if (isReady) {
    return (
      <div style={{ padding: '0.75rem', background: 'rgba(16, 185, 129, 0.08)', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(16, 185, 129, 0.2)', fontSize: '0.85rem', color: 'var(--accent-emerald)', marginTop: '1rem' }}>
        ✓ Document is prepared and ready for legal Q&A.
      </div>
    );
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <button
        className="btn-primary"
        onClick={handlePrepareDocument}
        disabled={isProcessing}
      >
        {isProcessing ? (
          <>
            <span className="spinner"></span>
            <span>{stepText || 'Processing...'}</span>
          </>
        ) : (
          <>
            <span>⚡</span> Prepare Document for Q&A
          </>
        )}
      </button>

      {error && (
        <div style={{ color: 'var(--status-failed)', fontSize: '0.85rem', marginTop: '0.5rem' }}>
          ⚠️ {error}
        </div>
      )}
    </div>
  );
};
