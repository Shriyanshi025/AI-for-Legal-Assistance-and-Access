import type { UserResponse, RegisterRequest, LoginRequest } from '../types/auth';
import type { ApiErrorResponse } from '../types/document';
import { API_ROOT_URL } from './config';

const AUTH_URL = `${API_ROOT_URL}/api/auth`;

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const errorJson: ApiErrorResponse = await response.json();
      if (errorJson && errorJson.message) {
        errorMessage = errorJson.message;
      }
    } catch {
      // Non-JSON response
    }
    throw new Error(errorMessage);
  }
  return response.json();
}

export const authApi = {
  async register(data: RegisterRequest): Promise<UserResponse> {
    const response = await fetch(`${AUTH_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(data),
    });
    return handleResponse<UserResponse>(response);
  },

  async login(data: LoginRequest): Promise<UserResponse> {
    const response = await fetch(`${AUTH_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(data),
    });
    return handleResponse<UserResponse>(response);
  },

  async logout(): Promise<void> {
    const response = await fetch(`${AUTH_URL}/logout`, {
      method: 'POST',
      credentials: 'include',
    });
    if (!response.ok) {
      throw new Error('Failed to logout');
    }
  },

  async me(): Promise<UserResponse> {
    const response = await fetch(`${AUTH_URL}/me`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
      credentials: 'include',
    });
    return handleResponse<UserResponse>(response);
  },
};
