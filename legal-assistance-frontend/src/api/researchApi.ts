import type { LegalResearchRequest, LegalResearchResponse, ResearchSessionSummary } from '../types/research';
import { API_ROOT_URL, withCsrfHeaders } from './config';

const API_BASE_URL = `${API_ROOT_URL}/api/research`;

export const researchApi = {
  async startResearch(request: LegalResearchRequest): Promise<LegalResearchResponse> {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Content-Type': 'application/json',
      }),
      credentials: 'include',
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      let errorMessage = '';
      let errorCode = 'RESEARCH_ERROR';
      let errorBody: any = null;

      try {
        errorBody = await response.json();
        errorMessage = errorBody.message || errorBody.error || '';
        errorCode = errorBody.code || errorBody.error || errorCode;
      } catch {
        // Fallback if body is not JSON
      }

      console.error('Research API error', {
        status: response.status,
        statusText: response.statusText,
        url: response.url,
        body: errorBody,
      });

      if (!errorMessage) {
        if (response.status === 404) {
          errorMessage = 'The requested research resource or document was not found (404).';
        } else if (response.status === 400) {
          errorMessage = 'Please select at least one processed document and enter a valid research question.';
        } else if (response.status === 401 || response.status === 403) {
          errorMessage = 'Your session is not authorized for this research request.';
        } else if (response.status === 429) {
          errorMessage = 'The AI service is temporarily busy. Please try again in a few moments.';
        } else if (response.status === 503) {
          errorMessage = 'The AI service is temporarily unavailable. Please try again in a few moments.';
        } else {
          errorMessage = `Research request failed with status ${response.status}.`;
        }
      }

      const err: any = new Error(errorMessage);
      err.code = errorCode;
      err.status = response.status;
      throw err;
    }

    return response.json();
  },

  async getUserResearchSessions(): Promise<ResearchSessionSummary[]> {
    const response = await fetch(`${API_BASE_URL}/sessions`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) {
      throw new Error('Failed to fetch research sessions');
    }
    return response.json();
  },

  async getResearchSession(id: string): Promise<LegalResearchResponse> {
    const response = await fetch(`${API_BASE_URL}/sessions/${id}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    if (!response.ok) {
      throw new Error('Failed to load research session dossier');
    }
    return response.json();
  },

  async askFollowUp(sessionId: string, question: string): Promise<import('../types/research').FollowUpResearchResponse> {
    const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}/followup`, {
      method: 'POST',
      headers: withCsrfHeaders({
        'Content-Type': 'application/json',
      }),
      credentials: 'include',
      body: JSON.stringify({ question }),
    });

    if (!response.ok) {
      let msg = 'Failed to execute follow-up research';
      try {
        const errJson = await response.json();
        if (errJson.message) msg = errJson.message;
      } catch {}
      throw new Error(msg);
    }
    return response.json();
  },

  async renameResearchSession(sessionId: string, title: string): Promise<ResearchSessionSummary> {
    const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}`, {
      method: 'PATCH',
      headers: withCsrfHeaders({
        'Content-Type': 'application/json',
      }),
      credentials: 'include',
      body: JSON.stringify({ title }),
    });

    if (!response.ok) {
      let msg = 'Failed to rename research session';
      try {
        const errJson = await response.json();
        if (errJson.message) msg = errJson.message;
      } catch {}
      throw new Error(msg);
    }
    return response.json();
  },

  async deleteResearchSession(sessionId: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: withCsrfHeaders(),
      credentials: 'include',
    });

    if (!response.ok) {
      let msg = 'Failed to delete research session';
      try {
        const errJson = await response.json();
        if (errJson.message) msg = errJson.message;
      } catch {}
      throw new Error(msg);
    }
  },
};
