import type {
  DocumentResponse,
  DocumentSummaryResponse,
  DocumentChunkResponse,
  LegalAnswerResponse,
  ApiErrorResponse
} from '../types/document';

const BASE_URL = 'http://localhost:8080/api/documents';

export const DEFAULT_USER_ID = '00000000-0000-0000-0000-000000000001';

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
  async uploadDocument(file: File, userId: string = DEFAULT_USER_ID): Promise<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId);

    const response = await fetch(BASE_URL, {
      method: 'POST',
      body: formData,
    });
    return handleResponse<DocumentResponse>(response);
  },

  /**
   * List all documents for a user.
   * Endpoint: GET /api/documents?userId={userId}
   */
  async getUserDocuments(userId: string = DEFAULT_USER_ID): Promise<DocumentSummaryResponse[]> {
    const response = await fetch(`${BASE_URL}?userId=${encodeURIComponent(userId)}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
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
      headers: {
        'Accept': 'application/json',
      },
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
      headers: {
        'Accept': 'application/json',
      },
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
      headers: {
        'Accept': 'application/json',
      },
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
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ question }),
    });
    return handleResponse<LegalAnswerResponse>(response);
  },
};
