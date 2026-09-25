import React from 'react';
import type { CitationResponse } from '../types/document';

interface StructuredAnswerProps {
  answer: string;
  citations: CitationResponse[];
  onSelectCitation?: (citation: CitationResponse) => void;
}

/**
 * Helper to parse inline markdown formatting (bold, italic, code) and citation tags ([SRC-N], [Page X], [N]).
 * Converts raw markdown tags into clean React elements and interactive citation buttons.
 */
function renderFormattedInlineText(
  text: string,
  citations: CitationResponse[],
  onSelectCitation?: (cit: CitationResponse) => void
): React.ReactNode[] {
  if (!text) return [];

  // Regex tokenizing citation tags ([SRC-1], [Page 6], [1], (Page 6)), bold/italic, and inline code
  const tokenRegex = /(\[\s*(?:SRC-\d+|Source\s*[•:-]?\s*Page\s*\d+|Page\s*\d+|\d+)\s*\]|\(Page\s*\d+\)|\*\*\*[^*]+\*\*\*|\*\*[^*]+\*\*|\*[^*]+\*|___[^_]+___|__[^_]+__|_[^_]+_|`[^`]+`)/gi;

  const parts = text.split(tokenRegex);

  return parts.map((part, idx) => {
    if (!part) return null;

    // 1. Check Citation match: [SRC-1] or [Page 6] or [1] or (Page 6)
    const srcMatch = part.match(/SRC-(\d+)/i);
    const pageMatch = part.match(/(?:Page\s*(\d+))|\[(\d+)\]/i);

    if (srcMatch) {
      const srcIdx = parseInt(srcMatch[1], 10) - 1;
      const citation = citations[srcIdx] || citations[0];
      if (citation) {
        return (
          <button
            key={`cit-${idx}`}
            type="button"
            className="inline-citation-btn"
            onClick={() => onSelectCitation && onSelectCitation(citation)}
            title={`Click to view source on Page ${citation.pageNumber}`}
            aria-label={`Open source on page ${citation.pageNumber}`}
          >
            📄 Page {citation.pageNumber}
          </button>
        );
      }
    } else if (pageMatch && (part.startsWith('[') || part.startsWith('('))) {
      const pageNum = pageMatch[1] ? parseInt(pageMatch[1], 10) : parseInt(pageMatch[2], 10);
      const citation = citations.find((c) => c.pageNumber === pageNum) || citations[0];
      if (citation) {
        return (
          <button
            key={`cit-${idx}`}
            type="button"
            className="inline-citation-btn"
            onClick={() => onSelectCitation && onSelectCitation(citation)}
            title={`Click to view source on Page ${citation.pageNumber}`}
            aria-label={`Open source on page ${citation.pageNumber}`}
          >
            📄 Page {citation.pageNumber}
          </button>
        );
      }
    }

    // 2. Bold + Italic: ***text***
    if (part.startsWith('***') && part.endsWith('***') && part.length > 6) {
      return (
        <strong key={idx}>
          <em>{part.slice(3, -3)}</em>
        </strong>
      );
    }

    // 3. Bold: **text** or __text__
    if (
      (part.startsWith('**') && part.endsWith('**') && part.length > 4) ||
      (part.startsWith('__') && part.endsWith('__') && part.length > 4)
    ) {
      return <strong key={idx}>{part.slice(2, -2)}</strong>;
    }

    // 4. Italic: *text* or _text_
    if (
      (part.startsWith('*') && part.endsWith('*') && part.length > 2) ||
      (part.startsWith('_') && part.endsWith('_') && part.length > 2)
    ) {
      return <em key={idx}>{part.slice(1, -1)}</em>;
    }

    // 5. Code: `text`
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return (
        <code
          key={idx}
          style={{
            background: 'rgba(0, 0, 0, 0.06)',
            padding: '2px 6px',
            borderRadius: '4px',
            fontSize: '0.85em',
            fontFamily: 'monospace',
          }}
        >
          {part.slice(1, -1)}
        </code>
      );
    }

    return <span key={idx}>{part}</span>;
  });
}

export const StructuredAnswer: React.FC<StructuredAnswerProps> = ({
  answer,
  citations,
  onSelectCitation,
}) => {
  if (!answer) return null;

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

    // 1. Headings: ###, ##, #
    if (trimmed.startsWith('#')) {
      flushList();
      const level = trimmed.match(/^#+/)?.[0].length || 1;
      const text = trimmed.replace(/^#+\s*/, '');
      const content = renderFormattedInlineText(text, citations, onSelectCitation);

      if (level >= 3) {
        elements.push(
          <h4 key={lineIdx} className="answer-heading-4">
            {content}
          </h4>
        );
      } else if (level === 2) {
        elements.push(
          <h3 key={lineIdx} className="answer-heading-3">
            {content}
          </h3>
        );
      } else {
        elements.push(
          <h2 key={lineIdx} className="answer-heading-2">
            {content}
          </h2>
        );
      }
      return;
    }

    // 2. Bold standalone headings: **Heading:** or **Heading**
    if (/^\*\*[^*]+(?::)?\*\*$/.test(trimmed)) {
      flushList();
      const text = trimmed.replace(/^\*\*/, '').replace(/\*\*$/, '');
      const content = renderFormattedInlineText(text, citations, onSelectCitation);
      elements.push(
        <h4
          key={lineIdx}
          className="answer-heading-4"
          style={{ color: 'var(--text-heading)', fontWeight: 700, marginTop: '10px' }}
        >
          {content}
        </h4>
      );
      return;
    }

    // 3. Unordered list items: •, -, *, +
    if (
      trimmed.startsWith('•') ||
      trimmed.startsWith('- ') ||
      trimmed.startsWith('* ') ||
      trimmed.startsWith('+ ')
    ) {
      const text = trimmed.replace(/^[•\-*+]\s*/, '');
      const content = renderFormattedInlineText(text, citations, onSelectCitation);
      if (!currentList || currentList.type !== 'ul') {
        flushList();
        currentList = { type: 'ul', items: [content] };
      } else {
        currentList.items.push(content);
      }
      return;
    }

    // 4. Numbered list items: 1., 2., 1), 2), etc.
    const numMatch = trimmed.match(/^(\d+)[\.\)]\s+(.*)/);
    if (numMatch) {
      const text = numMatch[2];
      const content = renderFormattedInlineText(text, citations, onSelectCitation);
      if (!currentList || currentList.type !== 'ol') {
        flushList();
        currentList = { type: 'ol', items: [content] };
      } else {
        currentList.items.push(content);
      }
      return;
    }

    // 5. Normal paragraph
    flushList();
    const content = renderFormattedInlineText(trimmed, citations, onSelectCitation);
    elements.push(
      <p key={lineIdx} className="answer-paragraph">
        {content}
      </p>
    );
  });

  flushList();

  return <div className="structured-answer-container">{elements}</div>;
};
