export const API_BASE = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8081/api'

export const getAuthHeaders = (token?: string) => {
  const authToken = token || localStorage.getItem('cravely_token') || ''
  return {
    'Content-Type': 'application/json',
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
  }
}

