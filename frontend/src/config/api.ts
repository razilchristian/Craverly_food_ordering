const rawUrl: string = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8081/api'

function normalizeApiBase(url: string): string {
  if (!url || typeof url !== 'string') {
    return 'http://localhost:8081/api'
  }
  const trimmed = url.trim().replace(/\/+$/, '')
  // If the user specified a base URL without /api (e.g. https://cravely-backend.onrender.com), append /api
  if (!trimmed.endsWith('/api')) {
    return `${trimmed}/api`
  }
  return trimmed
}

export const API_BASE = normalizeApiBase(rawUrl)

export const getAuthHeaders = (token?: string) => {
  const authToken = token || (typeof window !== 'undefined' ? localStorage.getItem('cravely_token') : '') || ''
  return {
    'Content-Type': 'application/json',
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
  }
}
