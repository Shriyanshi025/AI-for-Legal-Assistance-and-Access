import React from 'react';
import type { DocumentSummaryResponse } from '../types/document';
import { DocumentStatus } from './DocumentStatus';

interface DocumentListProps {
  documents: DocumentSummaryResponse[];
  selectedDocumentId: string | null;
  onSelectDocument: (doc: DocumentSummaryResponse) => void;
  isLoading: boolean;
}

export const DocumentList: React.FC<DocumentListProps> = ({
  documents,
  selectedDocumentId,
  onSelectDocument,
  isLoading
}) => {
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const formatDate = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="glass-panel" style={{ marginTop: '1.5rem' }}>
      <h3 className="panel-title">
        <span>📚</span> Document Library ({documents.length})
      </h3>

      {isLoading ? (
        <div className="empty-state">Loading documents...</div>
      ) : documents.length === 0 ? (
        <div className="empty-state">No documents uploaded yet. Upload a PDF to begin.</div>
      ) : (
        <div className="document-list">
          {documents.map((doc) => {
            const isSelected = doc.id === selectedDocumentId;
            return (
              <div
                key={doc.id}
                className={`document-item ${isSelected ? 'selected' : ''}`}
                onClick={() => onSelectDocument(doc)}
              >
                <div className="doc-name">{doc.filename}</div>
                <div className="doc-meta">
                  <span>{formatFileSize(doc.fileSize)} • {formatDate(doc.createdAt)}</span>
                  <DocumentStatus status={doc.status} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
