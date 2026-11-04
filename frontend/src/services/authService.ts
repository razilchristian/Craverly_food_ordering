import { API_BASE, getAuthHeaders } from '../config/api'

export interface UserInfo {
  id: number
  fullName: string
  email: string
  role: 'CUSTOMER' | 'RESTAURANT_OWNER' | 'ADMIN'
}

export interface AuthResponse {
  success: boolean
  message: string
  token: string
  user: UserInfo
}

export interface RegisterRequest {
  fullName: string
  email: string
  phone?: string
  password: string
  role?: 'CUSTOMER' | 'RESTAURANT_OWNER'
}

export interface LoginRequest {
  email: string
  password: string
}

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Registration failed')
    }
    if (body.token) {
      this.setSession(body.token, body.user)
    }
    return body
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    const body = await res.json()
    if (!res.ok || !body.success) {
      throw new Error(body.message || 'Login failed')
    }
    if (body.token) {
      this.setSession(body.token, body.user)
    }
    return body
  },

  setSession(token: string, user?: UserInfo) {
    localStorage.setItem('cravely_token', token)
    if (user) {
      localStorage.setItem('cravely_user', JSON.stringify(user))
      if (user.role === 'ADMIN') {
        localStorage.setItem('cravely_admin_email', user.email)
      }
    }
    window.dispatchEvent(new Event('cravely-auth-change'))
  },

  logout() {
    localStorage.removeItem('cravely_token')
    localStorage.removeItem('cravely_user')
    localStorage.removeItem('cravely_admin_email')
    window.dispatchEvent(new Event('cravely-auth-change'))
  },

  getStoredToken(): string | null {
    return localStorage.getItem('cravely_token')
  },

  getStoredUser(): UserInfo | null {
    const userStr = localStorage.getItem('cravely_user')
    if (!userStr) return null
    try {
      return JSON.parse(userStr)
    } catch {
      return null
    }
  },

  isAuthenticated(): boolean {
    return !!this.getStoredToken()
  },

  getUserRole(): 'CUSTOMER' | 'RESTAURANT_OWNER' | 'ADMIN' | null {
    const user = this.getStoredUser()
    return user ? user.role : null
  },
}

