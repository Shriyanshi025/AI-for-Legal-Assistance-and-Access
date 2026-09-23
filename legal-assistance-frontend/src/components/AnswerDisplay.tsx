import React from 'react';
import type { LegalAnswerResponse } from '../types/document';
import { CitationCard } from './CitationCard';

interface AnswerDisplayProps {
  response: LegalAnswerResponse;
}

export const AnswerDisplay: React.FC<AnswerDisplayProps> = ({ response }) => {
  return (
    <div className="answer-box">
      <div className={`grounded-banner ${response.grounded ? 'grounded-true' : 'grounded-false'}`}>
        {response.grounded ? (
          <>
            <span>🛡️</span>
            <span>Grounded Legal Answer — Supported by Document Citations</span>
          </>
        ) : (
          <>
            <span>⚠️</span>
            <span>Insufficient Context — Document Contains No Matching Clauses</span>
          </>
        )}
      </div>

      <div className="answer-text">
        {response.answer}
      </div>

      {response.grounded && response.citations && response.citations.length > 0 && (
        <div>
          <h4 className="citations-title">Verified Source Provenance & Citations ({response.citations.length})</h4>
          <div className="citations-grid">
            {response.citations.map((cit, idx) => (
              <CitationCard key={idx} citation={cit} index={idx} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
