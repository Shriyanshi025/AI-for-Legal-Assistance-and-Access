import React from 'react';
import type { LegalAnswerResponse, CitationResponse } from '../types/document';
import { CitationCard } from './CitationCard';
import { StructuredAnswer } from './StructuredAnswer';

interface AnswerDisplayProps {
  response: LegalAnswerResponse | null;
  onSelectCitation?: (citation: CitationResponse) => void;
}

export const AnswerDisplay: React.FC<AnswerDisplayProps> = ({ response, onSelectCitation }) => {
  return (
    <>
      {/* Answer Sub-card */}
      <div className="qa-result-subcard">
        <div className="subcard-header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          Answer
        </div>

        {!response ? (
          <div className="empty-state-box">
            <div className="empty-state-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                <circle cx="11" cy="14" r="3" />
                <line x1="16" y1="19" x2="13.2" y2="16.2" />
              </svg>
            </div>
            <div className="empty-state-title">Your answer will appear here</div>
            <div className="empty-state-desc">Ask a question to get started</div>
          </div>
        ) : (
          <div>
            <div className={`grounded-tag ${response.grounded ? 'true' : 'false'}`}>
              {response.grounded ? '🛡️ Grounded Legal Answer' : '⚠️ Insufficient Document Context'}
            </div>
            <div className="answer-body-text">
              <StructuredAnswer
                answer={response.answer}
                citations={response.citations || []}
                onSelectCitation={onSelectCitation}
              />
            </div>
          </div>
        )}
      </div>

      {/* Citations Sub-card */}
      <div className="qa-result-subcard" style={{ marginTop: '16px' }}>
        <div className="subcard-header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" color="#3B82F6">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
          </svg>
          Citations
        </div>
        <p style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', marginTop: '-8px', marginBottom: '12px' }}>
          Click any citation source to navigate directly to that document page and context
        </p>

        {!response || !response.citations || response.citations.length === 0 ? (
          <div className="empty-state-box">
            <div className="empty-state-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                <path d="m9 15 2 2 4-4" />
              </svg>
            </div>
            <div className="empty-state-title">No citations yet</div>
            <div className="empty-state-desc">Ask a question to see source references</div>
          </div>
        ) : (
          <div>
            {response.citations.map((cit, idx) => (
              <CitationCard
                key={idx}
                citation={cit}
                index={idx}
                onSelectCitation={onSelectCitation}
              />
            ))}
          </div>
        )}
      </div>
    </>
  );
};
