import React, { useState, useEffect } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse } from '../types/document';
import { DocumentUpload } from '../components/DocumentUpload';
import { DocumentList } from '../components/DocumentList';
import { PipelineControl } from '../components/PipelineControl';
import { LegalQaPanel } from '../components/LegalQaPanel';

export const DocumentManager: React.FC = () => {
  const [documents, setDocuments] = useState<DocumentSummaryResponse[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentSummaryResponse | DocumentResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDocuments = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const docs = await documentApi.getUserDocuments();
      setDocuments(docs);
      if (docs.length > 0 && !selectedDoc) {
        setSelectedDoc(docs[0]);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to connect to backend server. Make sure Spring Boot is running on port 8080.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleUploadSuccess = (newDoc: DocumentResponse) => {
    const summary: DocumentSummaryResponse = {
      id: newDoc.id,
      filename: newDoc.filename,
      documentType: newDoc.documentType,
      fileSize: newDoc.fileSize,
      status: newDoc.status,
      createdAt: newDoc.createdAt,
    };
    setDocuments((prev) => [summary, ...prev]);
    setSelectedDoc(summary);
  };

  const handleStatusUpdated = (updatedDoc: DocumentResponse) => {
    const summary: DocumentSummaryResponse = {
      id: updatedDoc.id,
      filename: updatedDoc.filename,
      documentType: updatedDoc.documentType,
      fileSize: updatedDoc.fileSize,
      status: updatedDoc.status,
      createdAt: updatedDoc.createdAt,
    };
    setDocuments((prev) =>
      prev.map((d) => (d.id === updatedDoc.id ? summary : d))
    );
    setSelectedDoc(summary);
  };

  return (
    <div className="main-container">
      {/* Sidebar: Upload & Library */}
      <aside className="sidebar">
        <DocumentUpload onUploadSuccess={handleUploadSuccess} />

        <DocumentList
          documents={documents}
          selectedDocumentId={selectedDoc?.id || null}
          onSelectDocument={setSelectedDoc}
          isLoading={isLoading}
        />

        {selectedDoc && (
          <div className="glass-panel" style={{ marginTop: '1.5rem' }}>
            <h4 style={{ fontFamily: 'var(--font-heading)', fontSize: '0.95rem', fontWeight: 600 }}>
              Selected Document Status
            </h4>
            <PipelineControl
              document={selectedDoc}
              onStatusUpdated={handleStatusUpdated}
            />
          </div>
        )}
      </aside>

      {/* Main Panel: Interactive Q&A Workspace */}
      <main className="content-area">
        {error && (
          <div className="glass-panel" style={{ borderLeft: '4px solid var(--status-failed)', color: 'var(--status-failed)', marginBottom: '1.5rem' }}>
            <strong>Connection Warning:</strong> {error}
          </div>
        )}

        {selectedDoc ? (
          <LegalQaPanel document={selectedDoc} />
        ) : (
          <div className="glass-panel empty-state">
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📄</div>
            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '1.2rem', marginBottom: '0.5rem' }}>
              No Document Selected
            </h3>
            <p>Upload a legal PDF document or select an existing document from the library to begin.</p>
          </div>
        )}
      </main>
    </div>
  );
};
