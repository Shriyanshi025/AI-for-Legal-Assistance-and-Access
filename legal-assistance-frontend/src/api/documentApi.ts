import type {
  DocumentResponse,
  DocumentSummaryResponse,
  DocumentChunkResponse,
  DocumentPageResponse,
  LegalAnswerResponse,
  UserProfileResponse,
  ApiErrorResponse
} from '../types/document';
import { API_ROOT_URL, withCsrfHeaders } from './config';

const BASE_URL = `${API_ROOT_URL}/api/documents`;
const USER_API_URL = `${API_ROOT_URL}/api/users`;

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const errorJson: ApiErrorResponse = await response.json();
      if (errorJson && errorJson.message) {
        errorMessage = errorJson.message;
      }
    } catch {
      // Ignore JSON parse errors for non-JSON error responses
    }
    throw new Error(errorMessage);
  }
  return response.json();
}

export const documentApi = {
  /**
   * Upload a legal PDF document.
   * Endpoint: POST /api/documents
   */
  async uploadDocument(file: File): Promise<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(BASE_URL, {
      method: 'POST',
      headers: withCsrfHeaders(),
      body: formData,
      credentials: 'include',
    });
    return handleResponse<DocumentResponse>(response);
  },

  /**
   * List all documents for the authenticated user.
   * Endpoint: GET /api/documents
   */
  async getUserDocuments(): Promise<DocumentSummaryResponse[]> {
    const response = await fetch(BASE_URL, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    return handleResponse<DocumentSummaryResponse[]>(response);
  },

  /**
   * Get metadata details for a specific document.
   * Endpoint: GET /api/documents/{id}
   */
  async getDocument(id: string): Promise<DocumentResponse> {
    const response = await fetch(`${BASE_URL}/${id}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    return handleResponse<DocumentResponse>(response);
  },

  /**
   * Trigger PDF text extraction and page creation.
   * Endpoint: POST /api/documents/{id}/extract
   */
  async extractText(id: string): Promise<DocumentResponse> {
    const response = await fetch(`${BASE_URL}/${id}/extract`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Accept': 'application/json',
      }),
      credentials: 'include',
    });
    return handleResponse<DocumentResponse>(response);
  },

  /**
   * Trigger text chunking across pages.
   * Endpoint: POST /api/documents/{id}/chunks
   */
  async chunkDocument(id: string): Promise<DocumentChunkResponse[]> {
    const response = await fetch(`${BASE_URL}/${id}/chunks`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Accept': 'application/json',
      }),
      credentials: 'include',
    });
    return handleResponse<DocumentChunkResponse[]>(response);
  },

  /**
   * Trigger 768-d Gemini embedding generation for document chunks.
   * Endpoint: POST /api/documents/{id}/embeddings
   */
  async generateEmbeddings(id: string): Promise<DocumentChunkResponse[]> {
    const response = await fetch(`${BASE_URL}/${id}/embeddings`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Accept': 'application/json',
      }),
      credentials: 'include',
    });
    return handleResponse<DocumentChunkResponse[]>(response);
  },

  /**
   * Helper method to execute the complete document preparation pipeline:
   * Extract -> Chunk -> Generate Embeddings
   */
  async prepareDocument(
    id: string,
    onProgress?: (step: 'EXTRACTING' | 'CHUNKING' | 'EMBEDDING' | 'COMPLETED') => void
  ): Promise<DocumentResponse> {
    if (onProgress) onProgress('EXTRACTING');
    const extractRes = await this.extractText(id);

    if (extractRes.status === 'FAILED') {
      throw new Error('PDF text extraction failed. Document may be scanned or unreadable.');
    }

    if (onProgress) onProgress('CHUNKING');
    await this.chunkDocument(id);

    if (onProgress) onProgress('EMBEDDING');
    await this.generateEmbeddings(id);

    if (onProgress) onProgress('COMPLETED');
    return this.getDocument(id);
  },

  /**
   * Ask a question against a grounded legal document.
   * Endpoint: POST /api/documents/{id}/ask
   */
  async askQuestion(id: string, question: string): Promise<LegalAnswerResponse> {
    const response = await fetch(`${BASE_URL}/${id}/ask`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      }),
      credentials: 'include',
      body: JSON.stringify({ question, documentIds: [id] }),
    });
    return handleResponse<LegalAnswerResponse>(response);
  },

  /**
   * Ask a question scoped to multiple selected legal documents.
   * Endpoint: POST /api/documents/ask
   */
  async askMultiDocumentQuestion(documentIds: string[], question: string): Promise<LegalAnswerResponse> {
    const response = await fetch(`${BASE_URL}/ask`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      }),
      credentials: 'include',
      body: JSON.stringify({ question, documentIds }),
    });
    return handleResponse<LegalAnswerResponse>(response);
  },

  /**
   * Get direct view URL for inline browser PDF viewing.
   * Endpoint: GET /api/documents/{id}/view
   */
  getDocumentViewUrl(id: string): string {
    return `${BASE_URL}/${id}/view`;
  },

  /**
   * Delete an uploaded document and all associated RAG chunks/embeddings.
   * Endpoint: DELETE /api/documents/{id}
   */
  async deleteDocument(id: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: withCsrfHeaders(),
      credentials: 'include',
    });
    if (!response.ok) {
      let errorMessage = `Failed to delete document (${response.status})`;
      try {
        const errorJson = await response.json();
        if (errorJson && errorJson.message) {
          errorMessage = errorJson.message;
        }
      } catch {
        // Ignore JSON error parse
      }
      throw new Error(errorMessage);
    }
  },

  /**
   * Get all extracted pages for a document.
   * Endpoint: GET /api/documents/{id}/pages
   */
  async getDocumentPages(id: string): Promise<DocumentPageResponse[]> {
    const response = await fetch(`${BASE_URL}/${id}/pages`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    return handleResponse<DocumentPageResponse[]>(response);
  },

  /**
   * Replace an existing document with a new PDF.
   * Endpoint: POST /api/documents/{id}/replace
   */
  async replaceDocument(id: string, file: File): Promise<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${BASE_URL}/${id}/replace`, {
      method: 'POST',
      headers: withCsrfHeaders(),
      body: formData,
      credentials: 'include',
    });
    return handleResponse<DocumentResponse>(response);
  },

  /**
   * Fetch public user profile for authenticated user.
   * Endpoint: GET /api/users/profile
   */
  async getUserProfile(): Promise<UserProfileResponse> {
    const response = await fetch(`${USER_API_URL}/profile`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    return handleResponse<UserProfileResponse>(response);
  },
};
