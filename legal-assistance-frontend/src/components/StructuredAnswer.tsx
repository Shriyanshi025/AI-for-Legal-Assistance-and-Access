import React from 'react';
import type { CitationResponse } from '../types/document';

interface StructuredAnswerProps {
  answer: string;
  citations: CitationResponse[];
  onSelectCitation?: (citation: CitationResponse) => void;
}

export const StructuredAnswer: React.FC<StructuredAnswerProps> = ({
  answer,
  citations,
  onSelectCitation,
}) => {
  if (!answer) return null;

  // Render text segment and convert inline citation references [Page X] or [Source • Page X] into interactive buttons
  const renderTextWithCitations = (text: string) => {
    // Regex matching citation patterns like [Page 6], [Page 6, Chunk #7], [Source • Page 6], (Page 6), or [1]
    const citationRegex = /(\[(?:Source\s*[•:-]\s*)?(?:Page\s*\d+[^\]]*|\d+)\]|\(Page\s*\d+\))/gi;
    const parts = text.split(citationRegex);

    return parts.map((part, idx) => {
      const match = part.match(/(?:Page\s*(\d+))|\[(\d+)\]/i);
      if (match) {
        const pageNum = match[1] ? parseInt(match[1], 10) : parseInt(match[2], 10);
        const matchingCitation = citations.find((c) => c.pageNumber === pageNum) || citations[0];

        if (matchingCitation) {
          return (
            <button
              key={idx}
              type="button"
              className="inline-citation-btn"
              onClick={() => onSelectCitation && onSelectCitation(matchingCitation)}
              title={`Click to view source on Page ${matchingCitation.pageNumber}`}
              aria-label={`Open source on page ${matchingCitation.pageNumber}`}
            >
              📄 Page {matchingCitation.pageNumber}
            </button>
          );
        }
      }
      return <span key={idx}>{part}</span>;
    });
  };

  // Split lines into structured paragraphs, headings, and list items
  const lines = answer.split('\n');
  const elements: React.ReactNode[] = [];
  let currentList: { type: 'ul' | 'ol'; items: React.ReactNode[] } | null = null;

  const flushList = () => {
    if (currentList) {
      if (currentList.type === 'ul') {
        elements.push(
          <ul key={`list-${elements.length}`} className="answer-bullet-list">
            {currentList.items.map((item, i) => (
              <li key={i}>{item}</li>
            ))}
          </ul>
        );
      } else {
        elements.push(
          <ol key={`list-${elements.length}`} className="answer-numbered-list">
            {currentList.items.map((item, i) => (
              <li key={i}>{item}</li>
            ))}
          </ol>
        );
      }
      currentList = null;
    }
  };

  lines.forEach((line, lineIdx) => {
    const trimmed = line.trim();
    if (!trimmed) {
      flushList();
      return;
    }

    // Headings: ###, ##, #
    if (trimmed.startsWith('#')) {
      flushList();
      const level = trimmed.match(/^#+/)?.[0].length || 1;
      const text = trimmed.replace(/^#+\s*/, '');
      const content = renderTextWithCitations(text);

      if (level >= 3) {
        elements.push(<h4 key={lineIdx} className="answer-heading-4">{content}</h4>);
      } else if (level === 2) {
        elements.push(<h3 key={lineIdx} className="answer-heading-3">{content}</h3>);
      } else {
        elements.push(<h2 key={lineIdx} className="answer-heading-2">{content}</h2>);
      }
      return;
    }

    // Unordered list items: •, -, *
    if (trimmed.startsWith('•') || trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
      const text = trimmed.replace(/^[•\-*]\s*/, '');
      const content = renderTextWithCitations(text);
      if (!currentList || currentList.type !== 'ul') {
        flushList();
        currentList = { type: 'ul', items: [content] };
      } else {
        currentList.items.push(content);
      }
      return;
    }

    // Numbered list items: 1., 2., etc.
    const numMatch = trimmed.match(/^(\d+)\.\s+(.*)/);
    if (numMatch) {
      const text = numMatch[2];
      const content = renderTextWithCitations(text);
      if (!currentList || currentList.type !== 'ol') {
        flushList();
        currentList = { type: 'ol', items: [content] };
      } else {
        currentList.items.push(content);
      }
      return;
    }

    // Normal paragraph
    flushList();
    elements.push(
      <p key={lineIdx} className="answer-paragraph">
        {renderTextWithCitations(trimmed)}
      </p>
    );
  });

  flushList();

  return <div className="structured-answer-container">{elements}</div>;
};
