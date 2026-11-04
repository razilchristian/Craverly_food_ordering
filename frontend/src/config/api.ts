const rawUrl = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8081/api'
export const API_BASE = rawUrl.replace(/\/+$/, '')

export const getAuthHeaders = (token?: string) => {
  const authToken = token || (typeof window !== 'undefined' ? localStorage.getItem('cravely_token') : '') || ''
  return {
    'Content-Type': 'application/json',
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
  }
}
