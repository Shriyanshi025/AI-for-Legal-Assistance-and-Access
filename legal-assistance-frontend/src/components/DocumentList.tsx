import React, { useState } from 'react';
import type { DocumentSummaryResponse } from '../types/document';
import { DocumentStatus } from './DocumentStatus';

interface DocumentListProps {
  documents: DocumentSummaryResponse[];
  selectedDocumentId: string | null;
  selectedDocIds: Set<string>;
  onSelectDocument: (doc: DocumentSummaryResponse) => void;
  onToggleSelectDocument: (id: string) => void;
  onToggleSelectAll: (selectAll: boolean) => void;
  onViewDocument?: (doc: DocumentSummaryResponse) => void;
  onReplaceDocument?: (doc: DocumentSummaryResponse, file: File) => void;
  onDeleteDocument?: (doc: DocumentSummaryResponse) => void;
  isLoading: boolean;
  isProcessingAction?: boolean;
}

export const DocumentList: React.FC<DocumentListProps> = ({
  documents,
  selectedDocumentId,
  selectedDocIds,
  onSelectDocument,
  onToggleSelectDocument,
  onToggleSelectAll,
  onViewDocument,
  onReplaceDocument,
  onDeleteDocument,
  isLoading,
  isProcessingAction = false
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingDoc, setDeletingDoc] = useState<DocumentSummaryResponse | null>(null);
  const [replacingDocId, setReplacingDocId] = useState<string | null>(null);
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(0)) + ' ' + sizes[i];
  };

  const formatDate = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  const filteredDocuments = documents.filter((doc) =>
    doc.filename.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const allSelected = filteredDocuments.length > 0 && filteredDocuments.every((d) => selectedDocIds.has(d.id));
  const selectedCount = selectedDocIds.size;

  const handleTriggerReplace = (doc: DocumentSummaryResponse, e: React.MouseEvent) => {
    e.stopPropagation();
    setReplacingDocId(doc.id);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
      fileInputRef.current.click();
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && replacingDocId) {
      const targetDoc = documents.find((d) => d.id === replacingDocId);
      if (targetDoc && onReplaceDocument) {
        onReplaceDocument(targetDoc, file);
      }
    }
    setReplacingDocId(null);
  };

  const handleConfirmDelete = () => {
    if (deletingDoc && onDeleteDocument) {
      onDeleteDocument(deletingDoc);
    }
    setDeletingDoc(null);
  };

  return (
    <div className="content-card" style={{ position: 'relative' }}>
      {/* Hidden file input for Replace action */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept=".pdf,application/pdf"
        style={{ display: 'none' }}
      />

      {/* Delete Confirmation Modal */}
      {deletingDoc && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.4)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 999
        }}>
          <div className="content-card" style={{ maxWidth: '420px', width: '90%', padding: '24px' }}>
            <h3 style={{ fontSize: '1.1rem', color: 'var(--text-heading)', marginBottom: '8px' }}>
              Delete Document?
            </h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Are you sure you want to delete <strong>"{deletingDoc.filename}"</strong>? This will remove the document and its indexed vector data. This action cannot be undone.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                className="prepare-btn"
                onClick={() => setDeletingDoc(null)}
                disabled={isProcessingAction}
                style={{ background: 'var(--bg-subcard)', color: 'var(--text-main)', border: '1px solid var(--border-color)' }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="prepare-btn"
                onClick={handleConfirmDelete}
                disabled={isProcessingAction}
                style={{ background: '#EF4444', color: '#FFFFFF', borderColor: '#DC2626' }}
              >
                {isProcessingAction ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="section-header-row">
        <div>
          <h3 className="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            Your Documents
          </h3>
          <p className="section-subtitle">Select documents to scope your legal RAG questions, manage files, and track status.</p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* Master Selection Controls */}
          {documents.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                type="button"
                onClick={() => onToggleSelectAll(!allSelected)}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-pill)',
                  border: '1px solid var(--border-color)',
                  background: allSelected ? 'var(--light-blue-bg)' : 'var(--bg-card)',
                  color: allSelected ? 'var(--primary-blue-hover)' : 'var(--text-main)',
                  fontSize: '0.78rem',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                {allSelected ? '✓ Deselect All' : '☑ Select All'}
              </button>
              <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--primary-blue)', background: 'var(--light-blue-pill)', padding: '4px 10px', borderRadius: '12px' }}>
                {selectedCount} Selected for RAG
              </span>
            </div>
          )}

          <div className="search-input-box">
            <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              placeholder="Search documents..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="doc-table-container">
        {isLoading ? (
          <div className="empty-state-box">Loading document library...</div>
        ) : filteredDocuments.length === 0 ? (
          <div className="empty-state-box">
            <div className="empty-state-title">No documents found</div>
            <div className="empty-state-desc">Upload a PDF document to begin analysis.</div>
          </div>
        ) : (
          <table className="doc-table">
            <thead>
              <tr>
                <th style={{ width: '40px', textAlign: 'center' }}>
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={(e) => onToggleSelectAll(e.target.checked)}
                    aria-label="Select or deselect all documents"
                    style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                  />
                </th>
                <th>Filename</th>
                <th>Type</th>
                <th>Size</th>
                <th>Status</th>
                <th>Created At</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDocuments.map((doc) => {
                const isSelectedForRag = selectedDocIds.has(doc.id);
                const isActiveDoc = doc.id === selectedDocumentId;
                return (
                  <tr
                    key={doc.id}
                    className={isActiveDoc ? 'selected-row' : isSelectedForRag ? 'rag-selected-row' : ''}
                    onClick={() => onSelectDocument(doc)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td style={{ textAlign: 'center' }} onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={isSelectedForRag}
                        onChange={() => onToggleSelectDocument(doc.id)}
                        aria-label={`Select ${doc.filename} for RAG`}
                        style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                      />
                    </td>
                    <td>
                      <div className="doc-name-cell">
                        <div className="pdf-icon-badge">pdf</div>
                        <span title={doc.filename}>{doc.filename}</span>
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>PDF</td>
                    <td style={{ color: 'var(--text-muted)' }}>{formatFileSize(doc.fileSize)}</td>
                    <td>
                      <DocumentStatus status={doc.status} />
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>{formatDate(doc.createdAt)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                        {/* VIEW ACTION */}
                        <button
                          type="button"
                          className="view-btn"
                          title="View Document PDF"
                          aria-label="View Document PDF"
                          onClick={(e) => {
                            e.stopPropagation();
                            if (onViewDocument) onViewDocument(doc);
                          }}
                        >
                          👁
                        </button>

                        {/* REPLACE ACTION */}
                        <button
                          type="button"
                          className="view-btn"
                          title="Replace Document PDF"
                          aria-label="Replace Document PDF"
                          disabled={isProcessingAction}
                          onClick={(e) => handleTriggerReplace(doc, e)}
                          style={{ background: 'rgba(59, 130, 246, 0.1)', color: '#3B82F6', borderColor: 'rgba(59, 130, 246, 0.3)' }}
                        >
                          🔄
                        </button>

                        {/* DELETE ACTION */}
                        <button
                          type="button"
                          className="view-btn"
                          title="Delete Document"
                          aria-label="Delete Document"
                          disabled={isProcessingAction}
                          onClick={(e) => {
                            e.stopPropagation();
                            setDeletingDoc(doc);
                          }}
                          style={{ background: 'rgba(239, 68, 68, 0.1)', color: '#EF4444', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                        >
                          🗑
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
