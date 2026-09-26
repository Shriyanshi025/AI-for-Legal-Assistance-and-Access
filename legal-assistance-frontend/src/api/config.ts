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
