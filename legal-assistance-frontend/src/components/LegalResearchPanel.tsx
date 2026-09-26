import React, { useState, useEffect } from 'react';
import { researchApi } from '../api/researchApi';
import type { DocumentSummaryResponse } from '../types/document';
import type {
  LegalResearchResponse,
  ResearchSessionSummary,
  ResearchFinding,
  ResearchConflict,
  EvidenceGap,
} from '../types/research';

interface LegalResearchPanelProps {
  documentsList: DocumentSummaryResponse[];
  selectedDocIds: Set<string>;
  onNavigateToUpload?: () => void;
}

const FormattedLegalText: React.FC<{ content: string }> = ({ content }) => {
  if (!content) return null;

  const lines = content.split('\n');
  const blocks: React.ReactNode[] = [];
  let currentList: React.ReactNode[] = [];

  const parseInline = (text: string): React.ReactNode[] => {
    const parts: React.ReactNode[] = [];
    const regex = /(\*\*.*?\*\*|\*.*?\*)/g;
    let lastIndex = 0;
    let match;

    while ((match = regex.exec(text)) !== null) {
      if (match.index > lastIndex) {
        parts.push(text.substring(lastIndex, match.index));
      }
      const matchedStr = match[0];
      if (matchedStr.startsWith('**') && matchedStr.endsWith('**')) {
        parts.push(<strong key={match.index}>{matchedStr.slice(2, -2)}</strong>);
      } else if (matchedStr.startsWith('*') && matchedStr.endsWith('*')) {
        parts.push(<em key={match.index}>{matchedStr.slice(1, -1)}</em>);
      }
      lastIndex = regex.lastIndex;
    }
    if (lastIndex < text.length) {
      parts.push(text.substring(lastIndex));
    }
    return parts;
  };

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      if (currentList.length > 0) {
        blocks.push(<ul key={`ul-${index}`} style={{ margin: '8px 0', paddingLeft: '20px' }}>{currentList}</ul>);
        currentList = [];
      }
      return;
    }

    if (trimmed.startsWith('- ') || trimmed.startsWith('* ') || trimmed.startsWith('• ')) {
      const itemText = trimmed.replace(/^[-*•]\s+/, '');
      currentList.push(<li key={`li-${index}`} style={{ marginBottom: '4px' }}>{parseInline(itemText)}</li>);
    } else {
      if (currentList.length > 0) {
        blocks.push(<ul key={`ul-${index}`} style={{ margin: '8px 0', paddingLeft: '20px' }}>{currentList}</ul>);
        currentList = [];
      }
      if (trimmed.startsWith('### ') || trimmed.startsWith('## ') || trimmed.startsWith('# ')) {
        const headerText = trimmed.replace(/^#+\s+/, '');
        blocks.push(<h5 key={`h-${index}`} style={{ margin: '12px 0 6px 0', color: '#0369A1', fontWeight: 700 }}>{parseInline(headerText)}</h5>);
      } else {
        blocks.push(<p key={`p-${index}`} style={{ margin: '0 0 8px 0', lineHeight: 1.6 }}>{parseInline(trimmed)}</p>);
      }
    }
  });

  if (currentList.length > 0) {
    blocks.push(<ul key={`ul-end`} style={{ margin: '8px 0', paddingLeft: '20px' }}>{currentList}</ul>);
  }

  return <div style={{ color: '#0C4A6E', fontSize: '0.84rem' }}>{blocks}</div>;
};

export const LegalResearchPanel: React.FC<LegalResearchPanelProps> = ({
  documentsList,
  selectedDocIds,
  onNavigateToUpload,
}) => {
  const [researchQuestion, setResearchQuestion] = useState<string>('');
  const [researchType, setResearchType] = useState<string>('Comprehensive Research');
  const [jurisdiction, setJurisdiction] = useState<string>('');
  const [relevantDate, setRelevantDate] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [currentDossier, setCurrentDossier] = useState<LegalResearchResponse | null>(null);
  const [savedSessions, setSavedSessions] = useState<ResearchSessionSummary[]>([]);

  const [activeMenuSessionId, setActiveMenuSessionId] = useState<string | null>(null);
  const [renamingSessionId, setRenamingSessionId] = useState<string | null>(null);
  const [renamingTitle, setRenamingTitle] = useState<string>('');
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(null);

  const [followUpAnswers, setFollowUpAnswers] = useState<Array<{ question: string; answer: string; citations: any[] }>>([]);
  const [activeFollowUpQuestion, setActiveFollowUpQuestion] = useState<string | null>(null);
  const [customFollowUp, setCustomFollowUp] = useState<string>('');

  const fetchSessions = async () => {
    try {
      const sessions = await researchApi.getUserResearchSessions();
      setSavedSessions(sessions || []);
    } catch {
      // Non-blocking history fetch error
    }
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  const selectedDocsArray = documentsList.filter((d) => selectedDocIds.has(d.id));

  const handleStartResearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!researchQuestion.trim()) return;
    if (selectedDocIds.size === 0) {
      setError('Please select at least one document from the sidebar scope before running research.');
      return;
    }

    setIsLoading(true);
    setError(null);
    setFollowUpAnswers([]);

    try {
      const docIdsArray = Array.from(selectedDocIds);
      console.log('[Research] Submitting research request with selected document IDs:', docIdsArray);

      const response = await researchApi.startResearch({
        researchQuestion: researchQuestion.trim(),
        documentIds: docIdsArray,
        researchType,
        jurisdiction: jurisdiction.trim() || undefined,
        relevantDate: relevantDate.trim() || undefined,
      });

      setCurrentDossier(response);
      fetchSessions();
    } catch (err: any) {
      setError(err.message || 'Failed to generate legal research dossier.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRestoreSession = async (sessionId: string) => {
    setIsLoading(true);
    setError(null);
    setFollowUpAnswers([]);
    try {
      const dossier = await researchApi.getResearchSession(sessionId);
      setCurrentDossier(dossier);
      setResearchQuestion(dossier.researchQuestion);
    } catch (err: any) {
      setError(err.message || 'Failed to restore research session.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmRename = async () => {
    if (!renamingSessionId || !renamingTitle.trim()) return;
    try {
      const updated = await researchApi.renameResearchSession(renamingSessionId, renamingTitle.trim());
      setSavedSessions((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      if (currentDossier && currentDossier.id === renamingSessionId) {
        setCurrentDossier((prev) => (prev ? { ...prev, researchQuestion: renamingTitle.trim() } : null));
      }
    } catch (err: any) {
      setError(err.message || 'Failed to rename research session.');
    } finally {
      setRenamingSessionId(null);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingSessionId) return;
    try {
      await researchApi.deleteResearchSession(deletingSessionId);
      setSavedSessions((prev) => prev.filter((s) => s.id !== deletingSessionId));
      if (currentDossier && currentDossier.id === deletingSessionId) {
        setCurrentDossier(null);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to delete research session.');
    } finally {
      setDeletingSessionId(null);
    }
  };

  const handleAskFollowUp = async (questionText: string) => {
    if (!questionText.trim()) return;
    if (!currentDossier || !currentDossier.id) {
      setError('No active research session to run follow-up research.');
      return;
    }

    const trimmedQuestion = questionText.trim();
    setActiveFollowUpQuestion(trimmedQuestion);
    setError(null);

    try {
      const response = await researchApi.askFollowUp(currentDossier.id, trimmedQuestion);
      setFollowUpAnswers((prev) => [
        ...prev,
        {
          question: response.question,
          answer: response.answer,
          citations: response.citations || [],
        },
      ]);
    } catch (err: any) {
      setError(err.message || 'Failed to execute follow-up research.');
    } finally {
      setActiveFollowUpQuestion(null);
    }
  };

  const handleExportPrint = () => {
    window.print();
  };

  const renderSupportBadge = (status: ResearchFinding['supportStatus']) => {
    switch (status) {
      case 'SUPPORTED':
        return <span className="status-pill ready">SUPPORTED</span>;
      case 'PARTIALLY_SUPPORTED':
        return <span className="status-pill processing">PARTIALLY SUPPORTED</span>;
      case 'CONFLICTING':
        return <span className="status-pill failed">CONFLICTING EVIDENCE</span>;
      case 'INSUFFICIENT_EVIDENCE':
      default:
        return <span className="status-pill failed" style={{ background: '#FEF3C7', color: '#B45309' }}>INSUFFICIENT EVIDENCE</span>;
    }
  };

  const renderEvidenceTypeBadge = (type: ResearchFinding['evidenceType']) => {
    switch (type) {
      case 'DIRECT':
        return <span className="badge-page" style={{ background: '#DCFCE7', color: '#15803D' }}>DIRECT EVIDENCE</span>;
      case 'INDIRECT':
        return <span className="badge-page" style={{ background: '#E0F2FE', color: '#0369A1' }}>INDIRECT</span>;
      case 'INFERRED':
        return <span className="badge-page" style={{ background: '#F3E8FF', color: '#6B21A8' }}>INFERRED</span>;
      case 'MISSING':
      default:
        return <span className="badge-page" style={{ background: '#FEE2E2', color: '#B91C1C' }}>MISSING</span>;
    }
  };

  return (
    <div className="qa-panel">
      {/* Top Header Card */}
      <div className="content-card" style={{ borderLeft: '4px solid #2563EB' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h2 className="section-title">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2563EB" strokeWidth="2.2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
              Evidence-Bounded Legal Research Workspace
            </h2>
            <p className="section-subtitle">
              Transform legal questions into grounded, evidence-traceable research dossiers with cross-document gap and conflict analysis.
            </p>
          </div>
        </div>

        {/* Form Container */}
        <form onSubmit={handleStartResearch} className="question-form" style={{ marginTop: '16px' }}>
          {/* Selected RAG Scope Indicator */}
          <div className="doc-selector-box">
            <div className="doc-selector-info">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#0284C7" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <path d="m9 12 2 2 4-4" />
              </svg>
              <span>
                {selectedDocsArray.length > 0
                  ? `${selectedDocsArray.length} Document${selectedDocsArray.length > 1 ? 's' : ''} Selected for Research Scope`
                  : 'No documents selected. Please check documents in Document Manager.'}
              </span>
            </div>

            {selectedDocsArray.length > 0 && (
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {selectedDocsArray.slice(0, 3).map((d) => (
                  <span key={d.id} className="user-badge" style={{ fontSize: '0.72rem', padding: '3px 10px' }}>
                    {d.filename}
                  </span>
                ))}
                {selectedDocsArray.length > 3 && (
                  <span className="user-badge" style={{ fontSize: '0.72rem', padding: '3px 8px' }}>
                    +{selectedDocsArray.length - 3} more
                  </span>
                )}
              </div>
            )}
          </div>

          <label className="form-label" style={{ marginTop: '12px' }}>
            Primary Research Question or Legal Inquiry:
          </label>
          <textarea
            className="question-textarea"
            placeholder="e.g. Does the termination clause permit immediate termination without notice under these agreements? Identify conflicting terms and evidence gaps."
            value={researchQuestion}
            onChange={(e) => setResearchQuestion(e.target.value)}
            disabled={isLoading}
            style={{ height: '100px' }}
          />

          {/* Research Mode & Context Row */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', marginTop: '8px' }}>
            <div>
              <label className="form-label">Research Mode:</label>
              <select
                className="search-input-box"
                style={{ width: '100%', height: '36px', paddingLeft: '10px' }}
                value={researchType}
                onChange={(e) => setResearchType(e.target.value)}
                disabled={isLoading}
              >
                <option value="Comprehensive Research">Comprehensive Research</option>
                <option value="Issue Analysis">Issue Analysis</option>
                <option value="Evidence Analysis">Evidence Analysis</option>
                <option value="Conflict Detection">Conflict Detection</option>
              </select>
            </div>

            <div>
              <label className="form-label">Jurisdiction (Optional):</label>
              <input
                type="text"
                className="search-input-box"
                style={{ width: '100%', height: '36px', paddingLeft: '10px' }}
                placeholder="e.g. New York, Delaware, UK"
                value={jurisdiction}
                onChange={(e) => setJurisdiction(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div>
              <label className="form-label">Relevant Date/Period (Optional):</label>
              <input
                type="text"
                className="search-input-box"
                style={{ width: '100%', height: '36px', paddingLeft: '10px' }}
                placeholder="e.g. FY 2025, Post-2024"
                value={relevantDate}
                onChange={(e) => setRelevantDate(e.target.value)}
                disabled={isLoading}
              />
            </div>
          </div>

          {error && (
            <div style={{ color: '#DC2626', fontSize: '0.82rem', marginTop: '8px', padding: '8px 12px', background: '#FEE2E2', borderRadius: '8px' }}>
              <strong>Research Error:</strong> {error}
            </div>
          )}

          <button
            type="submit"
            className="ask-btn"
            disabled={isLoading || !researchQuestion.trim() || selectedDocIds.size === 0}
            style={{ marginTop: '12px' }}
          >
            {isLoading ? (
              <>
                <span className="spinner" /> Analyzing Evidence & Building Dossier...
              </>
            ) : (
              <>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
                Start Evidence Research
              </>
            )}
          </button>
        </form>
      </div>

      {/* Saved Research Sessions History */}
      {savedSessions.length > 0 && !currentDossier && (
        <div className="content-card">
          <h3 className="section-title" style={{ fontSize: '0.98rem' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            Recent Research Sessions
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px', marginTop: '12px' }}>
            {savedSessions.map((s) => (
              <div
                key={s.id}
                onClick={() => handleRestoreSession(s.id)}
                style={{
                  background: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '10px',
                  padding: '12px',
                  position: 'relative',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                }}
                className="interactive-citation-card"
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.85rem', color: '#0F172A', marginBottom: '4px', flex: 1 }}>
                    {s.researchQuestion}
                  </div>
                  <div style={{ position: 'relative' }} onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      aria-label="Research session actions"
                      onClick={(e) => {
                        e.stopPropagation();
                        setActiveMenuSessionId((prev) => (prev === s.id ? null : s.id));
                      }}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        fontSize: '1.2rem',
                        cursor: 'pointer',
                        padding: '2px 6px',
                        borderRadius: '4px',
                        color: '#64748B',
                      }}
                    >
                      ⋮
                    </button>
                    {activeMenuSessionId === s.id && (
                      <div
                        style={{
                          position: 'absolute',
                          right: 0,
                          top: '100%',
                          background: '#FFFFFF',
                          border: '1px solid #CBD5E1',
                          borderRadius: '8px',
                          boxShadow: '0 4px 14px rgba(0,0,0,0.12)',
                          zIndex: 10,
                          minWidth: '130px',
                          overflow: 'hidden',
                        }}
                      >
                        <button
                          type="button"
                          style={{
                            display: 'block',
                            width: '100%',
                            textAlign: 'left',
                            padding: '8px 12px',
                            border: 'none',
                            background: 'transparent',
                            fontSize: '0.82rem',
                            cursor: 'pointer',
                            color: '#334155',
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setActiveMenuSessionId(null);
                            setRenamingSessionId(s.id);
                            setRenamingTitle(s.researchQuestion);
                          }}
                        >
                          ✏️ Rename
                        </button>
                        <button
                          type="button"
                          style={{
                            display: 'block',
                            width: '100%',
                            textAlign: 'left',
                            padding: '8px 12px',
                            border: 'none',
                            background: 'transparent',
                            fontSize: '0.82rem',
                            cursor: 'pointer',
                            color: '#DC2626',
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setActiveMenuSessionId(null);
                            setDeletingSessionId(s.id);
                          }}
                        >
                          🗑 Delete
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                <div style={{ fontSize: '0.75rem', color: '#64748B', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <span>{s.documentCount} docs</span> •
                  <span>{s.findingsCount} findings</span> •
                  {s.conflictsCount > 0 && <span style={{ color: '#DC2626' }}>{s.conflictsCount} conflicts</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Rename Session Modal */}
      {renamingSessionId && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', width: '90%', maxWidth: '440px', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>
            <h3 style={{ margin: '0 0 12px 0', fontSize: '1rem', color: '#0F172A' }}>Rename Research Session</h3>
            <input
              type="text"
              className="search-input-box"
              style={{ width: '100%', height: '38px', paddingLeft: '10px', marginBottom: '16px' }}
              value={renamingTitle}
              onChange={(e) => setRenamingTitle(e.target.value)}
              autoFocus
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="view-btn"
                style={{ background: '#F1F5F9', color: '#475569' }}
                onClick={() => setRenamingSessionId(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="view-btn"
                style={{ background: '#2563EB', color: '#FFFFFF' }}
                onClick={handleConfirmRename}
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Session Modal */}
      {deletingSessionId && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', width: '90%', maxWidth: '440px', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '1rem', color: '#DC2626' }}>Confirm Session Deletion</h3>
            <p style={{ fontSize: '0.85rem', color: '#475569', marginBottom: '16px' }}>
              Are you sure you want to delete this research session? The underlying uploaded legal documents and vector chunks will NOT be deleted.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="view-btn"
                style={{ background: '#F1F5F9', color: '#475569' }}
                onClick={() => setDeletingSessionId(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="view-btn"
                style={{ background: '#DC2626', color: '#FFFFFF' }}
                onClick={handleConfirmDelete}
              >
                Delete Session
              </button>
            </div>
          </div>
        </div>
      )}

      {/* RESULT DOSSIER DISPLAY */}
      {currentDossier && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {/* Action Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
            <div style={{ fontSize: '0.85rem', color: '#475569' }}>
              <strong>Dossier Generated:</strong> {new Date(currentDossier.createdAt).toLocaleString()}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={() => setCurrentDossier(null)}
                className="view-btn"
                style={{ background: '#F1F5F9', color: '#475569' }}
              >
                ← Back to Form
              </button>
              <button
                type="button"
                onClick={handleExportPrint}
                className="view-btn"
              >
                🖨 Export / Print Dossier
              </button>
            </div>
          </div>

          {/* Section 1: Overview & Primary Issues */}
          <div className="content-card">
            <h3 className="section-title" style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
              Legal Issue Identification
            </h3>
            <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {currentDossier.issues && currentDossier.issues.length > 0 ? (
                currentDossier.issues.map((iss, idx) => (
                  <div key={iss.id || idx} className="qa-result-subcard">
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#0F172A', marginBottom: '4px' }}>
                      Issue #{idx + 1}: {iss.title}
                    </div>
                    <p style={{ fontSize: '0.84rem', color: '#334155', lineHeight: 1.5 }}>{iss.description}</p>
                  </div>
                ))
              ) : (
                <div style={{ fontSize: '0.84rem', color: '#64748B' }}>Primary inquiry: {currentDossier.researchQuestion}</div>
              )}
            </div>
          </div>

          {/* Section 2: Key Findings */}
          <div className="content-card">
            <h3 className="section-title" style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
              Key Evidence Findings ({currentDossier.findings.length})
            </h3>
            <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {currentDossier.findings.map((finding: ResearchFinding, idx: number) => (
                <div key={finding.id || idx} className="qa-result-subcard">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', marginBottom: '8px' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.92rem', color: '#0F172A' }}>
                      Finding #{idx + 1}: {finding.title}
                    </span>
                    <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                      {renderEvidenceTypeBadge(finding.evidenceType)}
                      {renderSupportBadge(finding.supportStatus)}
                    </div>
                  </div>

                  <p style={{ fontSize: '0.86rem', color: '#1E293B', lineHeight: 1.6, marginBottom: '8px' }}>
                    {finding.statement}
                  </p>

                  {/* Inline Citations / Supporting Evidence Tags */}
                  {finding.citations && finding.citations.length > 0 && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap', marginTop: '6px' }}>
                      <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600 }}>Supporting Evidence:</span>
                      {finding.citations.map((cit, cIdx) => (
                        <span
                          key={cIdx}
                          style={{
                            fontSize: '0.72rem',
                            fontWeight: 600,
                            background: '#F1F5F9',
                            color: '#334155',
                            border: '1px solid #CBD5E1',
                            padding: '2px 8px',
                            borderRadius: '6px',
                          }}
                        >
                          📌 {cit}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Section 3: Evidence Matrix Table */}
          {currentDossier.evidenceMatrix && currentDossier.evidenceMatrix.length > 0 && (
            <div className="content-card">
              <h3 className="section-title" style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
                Evidence Matrix
              </h3>
              <div className="doc-table-container" style={{ marginTop: '12px' }}>
                <table className="doc-table">
                  <thead>
                    <tr>
                      <th>Finding</th>
                      <th>Source Document</th>
                      <th>Page #</th>
                      <th>Evidence Type</th>
                      <th>Support Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentDossier.evidenceMatrix.map((row, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 600 }}>{row.findingTitle}</td>
                        <td>{row.sourceDocument}</td>
                        <td>Page {row.pageNumber || '—'}</td>
                        <td>{renderEvidenceTypeBadge(row.evidenceType as any)}</td>
                        <td>{renderSupportBadge(row.supportStatus as any)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Section 4: Conflicts / Inconsistencies */}
          <div className="content-card" style={{ borderLeft: currentDossier.conflicts.length > 0 ? '4px solid #EF4444' : '1px solid #E2E8F0' }}>
            <h3 className="section-title" style={{ color: currentDossier.conflicts.length > 0 ? '#DC2626' : '#0F172A', borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
              Document Conflicts & Inconsistencies ({currentDossier.conflicts.length})
            </h3>
            <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {currentDossier.conflicts.length > 0 ? (
                currentDossier.conflicts.map((conf: ResearchConflict, idx: number) => (
                  <div key={conf.id || idx} className="qa-result-subcard" style={{ border: '1px solid #FCA5A5', background: '#FEF2F2' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#991B1B', marginBottom: '6px' }}>
                      Potential Contradiction #{idx + 1}: {conf.topic}
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '10px', marginBottom: '10px' }}>
                      <div style={{ background: '#FFFFFF', padding: '10px', borderRadius: '8px', border: '1px solid #FECACA' }}>
                        <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#B91C1C' }}>Source A Excerpt:</div>
                        <div style={{ fontSize: '0.78rem', color: '#451A1A', fontStyle: 'italic', marginTop: '4px' }}>"{conf.sourceAExcerpt}"</div>
                      </div>
                      <div style={{ background: '#FFFFFF', padding: '10px', borderRadius: '8px', border: '1px solid #FECACA' }}>
                        <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#B91C1C' }}>Source B Excerpt:</div>
                        <div style={{ fontSize: '0.78rem', color: '#451A1A', fontStyle: 'italic', marginTop: '4px' }}>"{conf.sourceBExcerpt}"</div>
                      </div>
                    </div>

                    <p style={{ fontSize: '0.82rem', color: '#7F1D1D', lineHeight: 1.5 }}>
                      <strong>Analysis:</strong> {conf.analysis}
                    </p>
                  </div>
                ))
              ) : (
                <div style={{ fontSize: '0.84rem', color: '#16A34A', padding: '10px', background: '#F0FDF4', borderRadius: '8px' }}>
                  ✓ No direct conflicts or contradictions detected among the selected documents.
                </div>
              )}
            </div>
          </div>

          {/* Section 5: Evidence Gap Analysis ("What is Missing?") */}
          <div className="content-card" style={{ borderLeft: '4px solid #F59E0B' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
              <h3 className="section-title" style={{ color: '#B45309' }}>
                Evidence Gap Analysis ("What is Missing?")
              </h3>
              {onNavigateToUpload && (
                <button
                  type="button"
                  onClick={onNavigateToUpload}
                  className="view-btn"
                  style={{ background: '#FEF3C7', color: '#B45309' }}
                >
                  + Upload Missing Document
                </button>
              )}
            </div>

            <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {currentDossier.evidenceGaps && currentDossier.evidenceGaps.length > 0 ? (
                currentDossier.evidenceGaps.map((gap: EvidenceGap, idx: number) => (
                  <div key={idx} style={{ background: '#FFFBEB', border: '1px solid #FCD34D', padding: '12px', borderRadius: '10px' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.85rem', color: '#92400E', marginBottom: '4px' }}>
                      ⚠️ Gap #{idx + 1}: {gap.description}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#78350F' }}>
                      <strong>Why It Matters:</strong> {gap.whyItMatters}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ fontSize: '0.84rem', color: '#64748B' }}>No major evidence gaps identified.</div>
              )}
            </div>
          </div>

          {/* Section 6: Recommended Follow-up Questions & Follow-up Q&A */}
          <div className="content-card">
            <h3 className="section-title" style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
              Recommended Follow-up Research Questions
            </h3>

            {/* Custom follow-up input row */}
            <div style={{ display: 'flex', gap: '8px', marginTop: '12px', marginBottom: '14px', flexWrap: 'wrap' }}>
              <input
                type="text"
                className="search-input-box"
                placeholder="Type a follow-up inquiry grounded in this research session..."
                value={customFollowUp}
                onChange={(e) => setCustomFollowUp(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && customFollowUp.trim() && activeFollowUpQuestion === null) {
                    handleAskFollowUp(customFollowUp.trim());
                    setCustomFollowUp('');
                  }
                }}
                disabled={activeFollowUpQuestion !== null}
                style={{ flex: 1, minWidth: '240px', height: '38px', paddingLeft: '12px' }}
              />
              <button
                type="button"
                className="ask-btn"
                disabled={activeFollowUpQuestion !== null || !customFollowUp.trim()}
                onClick={() => {
                  if (customFollowUp.trim()) {
                    handleAskFollowUp(customFollowUp.trim());
                    setCustomFollowUp('');
                  }
                }}
                style={{
                  height: '38px',
                  padding: '0 16px',
                  marginTop: 0,
                  opacity: activeFollowUpQuestion !== null && activeFollowUpQuestion !== customFollowUp ? 0.5 : 1,
                  cursor: activeFollowUpQuestion !== null ? 'not-allowed' : 'pointer',
                }}
              >
                {activeFollowUpQuestion === customFollowUp ? (
                  <>
                    <span className="spinner" style={{ width: '12px', height: '12px' }} /> Analyzing...
                  </>
                ) : (
                  'Ask Follow-up'
                )}
              </button>
            </div>

            {/* Recommended follow-up list */}
            {currentDossier.followUpQuestions && currentDossier.followUpQuestions.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {currentDossier.followUpQuestions.map((qText, idx) => {
                  const isThisRowActive = activeFollowUpQuestion === qText;
                  const isAnyActive = activeFollowUpQuestion !== null;
                  return (
                    <div
                      key={idx}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        background: '#F8FAFC',
                        border: '1px solid #E2E8F0',
                        padding: '10px 14px',
                        borderRadius: '8px',
                        flexWrap: 'wrap',
                        gap: '8px',
                      }}
                    >
                      <span style={{ fontSize: '0.84rem', color: '#1E293B', flex: 1 }}>
                        {idx + 1}. {qText}
                      </span>
                      <button
                        type="button"
                        className="view-btn"
                        disabled={isAnyActive}
                        onClick={() => handleAskFollowUp(qText)}
                        style={{
                          minWidth: '115px',
                          display: 'inline-flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '6px',
                          opacity: isAnyActive && !isThisRowActive ? 0.5 : 1,
                          cursor: isAnyActive ? 'not-allowed' : 'pointer',
                        }}
                      >
                        {isThisRowActive ? (
                          <>
                            <span className="spinner" style={{ width: '12px', height: '12px' }} /> Analyzing...
                          </>
                        ) : (
                          'Ask Follow-up'
                        )}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Follow-up Q&A Answer Area */}
            {followUpAnswers.length > 0 && (
              <div style={{ marginTop: '18px', paddingTop: '14px', borderTop: '2px dashed #CBD5E1', display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <h4 style={{ margin: 0, fontSize: '0.9rem', color: '#0F172A', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span>💡</span> Follow-up Q&A Answers ({followUpAnswers.length})
                </h4>
                {followUpAnswers.map((ans, aIdx) => (
                  <div key={aIdx} className="qa-result-subcard" style={{ background: '#F0F9FF', border: '1px solid #BAE6FD' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.86rem', color: '#0369A1', marginBottom: '8px' }}>
                      Follow-up #{aIdx + 1}: {ans.question}
                    </div>
                    <div style={{ margin: '0 0 8px 0' }}>
                      <FormattedLegalText content={ans.answer} />
                    </div>
                    {ans.citations && ans.citations.length > 0 && (
                      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.74rem', color: '#0284C7', fontWeight: 600 }}>Citations:</span>
                        {ans.citations.map((c, cIdx) => (
                          <span key={cIdx} className="badge-page" style={{ background: '#E0F2FE', color: '#0369A1' }}>
                            📌 Page {c.pageNumber} (Chunk #{c.chunkIndex})
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section 7: Source Trail & Provenance */}
          {currentDossier.sources && currentDossier.sources.length > 0 && (
            <div className="content-card">
              <h3 className="section-title" style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
                Evidence Source Trail ({currentDossier.sources.length} Excerpts)
              </h3>
              <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {currentDossier.sources.map((src, idx) => (
                  <div key={idx} className="citation-item">
                    <div className="citation-item-header">
                      <span className="badge-page">[{src.citationTag || `SRC-${idx + 1}`}]</span>
                      <span className="badge-chunk">Page {src.pageNumber}</span>
                      <span className="badge-ref">Chunk #{src.chunkIndex}</span>
                    </div>
                    <div className="citation-item-text">"{src.excerpt}"</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Disclaimer Footer */}
          <div style={{ fontSize: '0.75rem', color: '#64748B', textAlign: 'center', padding: '12px', background: 'rgba(255,255,255,0.7)', borderRadius: '10px' }}>
            🔒 <strong>Legal Research Disclaimer:</strong> This research dossier is generated strictly from retrieved evidence in the selected documents for informational and research assistance. It does not constitute formal legal advice or professional representation.
          </div>
        </div>
      )}
    </div>
  );
};
