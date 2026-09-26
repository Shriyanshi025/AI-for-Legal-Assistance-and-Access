/**
 * Dynamic API Base URL resolution for local development & production deployments.
 * Evaluates import.meta.env.VITE_API_BASE_URL if set, or defaults to http://localhost:8080.
 */
function resolveApiRootUrl(): string {
  const envUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim();
  if (envUrl && envUrl.length > 0) {
    // Strip trailing slashes and any trailing /api segment to avoid double /api/api paths
    return envUrl.replace(/\/+$/, '').replace(/\/api$/, '');
  }
  return 'http://localhost:8080';
}

export const API_ROOT_URL = resolveApiRootUrl();

/**
 * Extract XSRF-TOKEN cookie value from document.cookie
 */
export function getCsrfToken(): string | null {
  if (typeof document === 'undefined' || !document.cookie) {
    return null;
  }
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Return headers merged with X-XSRF-TOKEN header if CSRF cookie is present.
 */
export function withCsrfHeaders(existingHeaders: Record<string, string> = {}): Record<string, string> {
  const headers = { ...existingHeaders };
  const token = getCsrfToken();
  if (token) {
    headers['X-XSRF-TOKEN'] = token;
  }
  return headers;
}
