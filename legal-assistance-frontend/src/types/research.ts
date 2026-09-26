import type { CitationResponse } from './document';

export interface ResearchIssue {
  id: string;
  title: string;
  description: string;
  relatedCitations?: string[];
}

export interface ResearchFinding {
  id: string;
  title: string;
  statement: string;
  supportStatus: 'SUPPORTED' | 'PARTIALLY_SUPPORTED' | 'CONFLICTING' | 'INSUFFICIENT_EVIDENCE';
  evidenceType: 'DIRECT' | 'INDIRECT' | 'INFERRED' | 'MISSING';
  citations: string[];
}

export interface EvidenceMatrixItem {
  findingTitle: string;
  sourceDocument: string;
  pageNumber: number;
  evidenceType: string;
  supportStatus: string;
}

export interface ResearchConflict {
  id: string;
  topic: string;
  sourceAExcerpt: string;
  sourceBExcerpt: string;
  analysis: string;
  resolutionStatus: 'UNRESOLVED' | 'RESOLVED_BY_SOURCE' | 'INSUFFICIENT_INFORMATION';
  citations: string[];
}

export interface EvidenceGap {
  description: string;
  whyItMatters: string;
  relatedIssue?: string;
}

export interface LegalResearchRequest {
  researchQuestion: string;
  documentIds: string[];
  researchType?: string;
  jurisdiction?: string;
  relevantDate?: string;
}

export interface LegalResearchResponse {
  id: string;
  researchQuestion: string;
  researchType: string;
  jurisdiction?: string;
  relevantDate?: string;
  selectedDocumentIds: string[];
  issues: ResearchIssue[];
  findings: ResearchFinding[];
  evidenceMatrix: EvidenceMatrixItem[];
  conflicts: ResearchConflict[];
  evidenceGaps: EvidenceGap[];
  followUpQuestions: string[];
  sources: CitationResponse[];
  createdAt: string;
}

export interface ResearchSessionSummary {
  id: string;
  researchQuestion: string;
  researchType: string;
  documentCount: number;
  findingsCount: number;
  gapsCount: number;
  conflictsCount: number;
  createdAt: string;
}

export interface FollowUpResearchRequest {
  question: string;
}

export interface FollowUpResearchResponse {
  sessionId: string;
  question: string;
  answer: string;
  citations: CitationResponse[];
  createdAt: string;
}
