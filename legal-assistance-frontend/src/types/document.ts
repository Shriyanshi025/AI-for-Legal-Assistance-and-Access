export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED';

export interface DocumentResponse {
  id: string;
  filename: string;
  documentType: string;
  fileSize: number;
  status: DocumentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentSummaryResponse {
  id: string;
  filename: string;
  documentType: string;
  fileSize: number;
  status: DocumentStatus;
  createdAt: string;
}

export interface DocumentPageResponse {
  id: string;
  documentId: string;
  pageNumber: number;
  content: string;
  createdAt: string;
}

export interface DocumentChunkResponse {
  id: string;
  documentId: string;
  pageNumber: number;
  section: string | null;
  clause: string | null;
  content: string;
  chunkIndex: number;
}

export interface SimilaritySearchResultResponse {
  chunkId: string;
  documentId: string;
  pageNumber: number;
  chunkIndex: number;
  content: string;
  section: string | null;
  clause: string | null;
  similarityScore: number;
}

export interface LegalAskRequest {
  question: string;
}

export interface CitationResponse {
  pageNumber: number;
  chunkIndex: number;
  excerpt: string;
}

export interface LegalAnswerResponse {
  answer: string;
  grounded: boolean;
  citations: CitationResponse[];
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
