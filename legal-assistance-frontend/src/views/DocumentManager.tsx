import React, { useState, useEffect } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentSummaryResponse, DocumentResponse, CitationResponse } from '../types/document';
import { Navbar } from '../components/Navbar';
import { DocumentUpload } from '../components/DocumentUpload';
import { DocumentList } from '../components/DocumentList';
import { PipelineControl } from '../components/PipelineControl';
import { LegalQaPanel } from '../components/LegalQaPanel';
import { CitationViewerModal } from '../components/CitationViewerModal';

export const DocumentManager: React.FC = () => {
  const [documents, setDocuments] = useState<DocumentSummaryResponse[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentSummaryResponse | DocumentResponse | null>(null);
  const [selectedDocIds, setSelectedDocIds] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [activeCitation, setActiveCitation] = useState<CitationResponse | null>(null);

  const fetchDocuments = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const docs = await documentApi.getUserDocuments();
      const safeDocs = Array.isArray(docs) ? docs.filter(Boolean) : [];
      setDocuments(safeDocs);

      if (safeDocs.length > 0) {
        setSelectedDoc((prev) => {
          if (!prev) return safeDocs[0];
          const exists = safeDocs.find((d) => d && d.id === prev.id);
          return exists || safeDocs[0];
        });

        setSelectedDocIds((prev) => {
          if (prev.size === 0) {
            return new Set(safeDocs.map((d) => d.id));
          }
          const validIds = new Set(Array.from(prev).filter((id) => safeDocs.some((d) => d.id === id)));
          return validIds.size > 0 ? validIds : new Set(safeDocs.map((d) => d.id));
        });
      } else {
        setSelectedDoc(null);
        setSelectedDocIds(new Set());
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

  const handleToggleSelectDocument = (id: string) => {
    setSelectedDocIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleToggleSelectAll = (selectAll: boolean) => {
    if (selectAll) {
      setSelectedDocIds(new Set(documents.map((d) => d.id)));
    } else {
      setSelectedDocIds(new Set());
    }
  };

  const handleUploadSuccess = (newDoc: DocumentResponse) => {
    if (!newDoc || !newDoc.id) return;
    const summary: DocumentSummaryResponse = {
      id: newDoc.id,
      filename: newDoc.filename,
      documentType: newDoc.documentType,
      fileSize: newDoc.fileSize,
      status: newDoc.status,
      createdAt: newDoc.createdAt,
    };
    setDocuments((prev) => [summary, ...(prev || [])]);
    setSelectedDoc(summary);
    setSelectedDocIds((prev) => new Set(prev).add(newDoc.id));
  };

  const handleStatusUpdated = (updatedDoc: DocumentResponse) => {
    if (!updatedDoc || !updatedDoc.id) return;
    const summary: DocumentSummaryResponse = {
      id: updatedDoc.id,
      filename: updatedDoc.filename,
      documentType: updatedDoc.documentType,
      fileSize: updatedDoc.fileSize,
      status: updatedDoc.status,
      createdAt: updatedDoc.createdAt,
    };
    setDocuments((prev) =>
      (prev || []).map((d) => (d && d.id === updatedDoc.id ? summary : d))
    );
    setSelectedDoc(summary);
    setSelectedDocIds((prev) => new Set(prev).add(updatedDoc.id));
  };

  const [isProcessingAction, setIsProcessingAction] = useState<boolean>(false);

  const handleViewDocument = (doc: DocumentSummaryResponse) => {
    if (!doc || !doc.id) return;
    const viewUrl = documentApi.getDocumentViewUrl(doc.id);
    window.open(viewUrl, '_blank', 'noopener,noreferrer');
  };

  const handleReplaceDocument = async (doc: DocumentSummaryResponse, file: File) => {
    if (!doc || !doc.id || !file) return;
    setIsProcessingAction(true);
    setError(null);
    try {
      const updatedDoc = await documentApi.replaceDocument(doc.id, file);
      handleStatusUpdated(updatedDoc);
    } catch (err: any) {
      setError(err.message || 'Failed to replace document.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const handleDeleteDocument = async (doc: DocumentSummaryResponse) => {
    if (!doc || !doc.id) return;
    setIsProcessingAction(true);
    setError(null);
    try {
      await documentApi.deleteDocument(doc.id);
      setDocuments((prev) => (prev || []).filter((d) => d && d.id !== doc.id));

      setSelectedDocIds((prev) => {
        const next = new Set(prev);
        next.delete(doc.id);
        return next;
      });

      setSelectedDoc((prev) => {
        if (prev && prev.id === doc.id) {
          const remaining = documents.filter((d) => d && d.id !== doc.id);
          return remaining.length > 0 ? remaining[0] : null;
        }
        return prev;
      });
    } catch (err: any) {
      setError(err.message || 'Failed to delete document.');
    } finally {
      setIsProcessingAction(false);
    }
  };

  const activeDocument = selectedDoc;

  const [activeView, setActiveView] = useState<'manager' | 'assistant'>('manager');

  const handleSelectAssistant = () => {
    setActiveView('assistant');
    const textarea = document.getElementById('legal-qa-textarea');
    if (textarea) {
      textarea.scrollIntoView({ behavior: 'smooth', block: 'center' });
      (textarea as HTMLTextAreaElement).focus();
    }
  };

  const handleSelectManager = () => {
    setActiveView('manager');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSelectCitation = (citation: CitationResponse) => {
    setActiveCitation(citation);
    setTimeout(() => {
      const viewerEl = document.getElementById('main-workspace-source-viewer');
      if (viewerEl) {
        viewerEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 50);
  };

  const citationTargetDocId = activeCitation?.documentId || activeDocument?.id || (selectedDocIds.size > 0 ? Array.from(selectedDocIds)[0] : null);
  const citationTargetDocObj = documents.find((d) => d.id === citationTargetDocId);
  const citationTargetFilename = citationTargetDocObj?.filename || activeDocument?.filename || 'Legal Document';

  return (
    <div className="dashboard-container">
      {/* Top Header Card matching reference image */}
      <Navbar />

      {/* 3-Column Dashboard Body */}
      <div className="dashboard-body">
        {/* Column 1: Left Navigation & Justice Scale Illustration */}
        <aside className="sidebar-panel">
          <nav className="sidebar-nav">
            <div
              className={`nav-item ${activeView === 'manager' ? 'active' : ''}`}
              onClick={handleSelectManager}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleSelectManager(); }}
              role="button"
              tabIndex={0}
              aria-label="Document Manager View"
            >
              <svg className="nav-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
              <span>Document Manager</span>
            </div>

            <div
              className={`nav-item ${activeView === 'assistant' ? 'active' : ''}`}
              onClick={handleSelectAssistant}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleSelectAssistant(); }}
              role="button"
              tabIndex={0}
              aria-label="Legal Assistant View"
            >
              <svg className="nav-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <span>Legal Assistant</span>
            </div>
          </nav>

          <div className="sidebar-bottom">
            <div className="sidebar-info-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="16" x2="12" y2="12" />
                <line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
              <span>AI-Powered Legal Assistance</span>
            </div>

            {/* LOWER-LEFT CORNER: Plain Milky White Container with Justice Scale PNG */}
            <div className="justice-scale-container">
              <img
                src="/Justice%20logo.png"
                alt=""
                className="scale-img"
              />
            </div>
          </div>
        </aside>

        {/* Column 2: Central Main Workspace */}
        <main className="main-workspace">
          {error && (
            <div className="content-card" style={{ borderLeft: '4px solid var(--status-failed-text)', color: 'var(--status-failed-text)', fontSize: '0.85rem' }}>
              <strong>Connection Notice:</strong> {error}
            </div>
          )}

          {/* Section 1: Upload Your Legal Document */}
          <DocumentUpload onUploadSuccess={handleUploadSuccess} />

          {/* Section 2: Your Documents Table */}
          <DocumentList
            documents={documents}
            selectedDocumentId={activeDocument?.id || null}
            selectedDocIds={selectedDocIds}
            onSelectDocument={setSelectedDoc}
            onToggleSelectDocument={handleToggleSelectDocument}
            onToggleSelectAll={handleToggleSelectAll}
            onViewDocument={handleViewDocument}
            onReplaceDocument={handleReplaceDocument}
            onDeleteDocument={handleDeleteDocument}
            isLoading={isLoading}
            isProcessingAction={isProcessingAction}
          />

          {/* Section 3: Processing Pipeline */}
          <PipelineControl
            document={activeDocument}
            onStatusUpdated={handleStatusUpdated}
          />

          {/* Section 4: Main Workspace Citation Source & PDF Viewer */}
          {activeCitation && citationTargetDocId && (
            <div id="main-workspace-source-viewer" style={{ width: '100%', marginTop: '18px' }}>
              <CitationViewerModal
                documentId={citationTargetDocId}
                documentFilename={citationTargetFilename}
                citation={activeCitation}
                onClose={() => setActiveCitation(null)}
              />
            </div>
          )}
        </main>

        {/* Column 3: Right Legal Q&A Panel */}
        <aside className="qa-workspace">
          <LegalQaPanel
            document={activeDocument}
            documentsList={documents}
            selectedDocIds={selectedDocIds}
            onSelectDocument={setSelectedDoc}
            onSelectCitation={handleSelectCitation}
          />
        </aside>
      </div>
    </div>
  );
};
