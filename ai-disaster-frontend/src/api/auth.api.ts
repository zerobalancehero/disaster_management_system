import { api } from './client'
import type { LoginRequest, LoginResponse, User } from '../types'

function resolveEmail(identifier: string): string {
  const trimmed = identifier.trim()
  if (trimmed.includes('@')) return trimmed
  const lower = trimmed.toLowerCase()
  if (lower === 'admin') return 'admin@disaster.local'
  if (lower === 'responder') return 'responder@disaster.local'
  if (lower === 'user') return 'user@disaster.local'
  return `${lower}@disaster.local`
}

export const getStoredUser = (): User | null => {
  try {
    const raw = localStorage.getItem('disaster_user')
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export const login = async (creds: { username?: string; email?: string; password: string }): Promise<User> => {
  const email = creds.email ? creds.email : resolveEmail(creds.username ?? '')
  const payload: LoginRequest = {
    email,
    password: creds.password,
  }

  const response = await api.post<LoginResponse>('/auth/login', payload)
  const data = response.data

  if (data && 'token' in data && data.token) {
    localStorage.setItem('disaster_token', data.token)
    const user = data.user
    localStorage.setItem('disaster_user', JSON.stringify(user))
    return user
  }

  // Fallback for direct user mock
  const directUser = (data as unknown as { user?: User }).user ?? (data as unknown as User)
  localStorage.setItem('disaster_user', JSON.stringify(directUser))
  return directUser
}

export const logout = async (): Promise<void> => {
  try {
    await api.post('/auth/logout')
  } catch {
    // Statelessly ignore network errors on logout
  } finally {
    localStorage.removeItem('disaster_token')
    localStorage.removeItem('disaster_user')
  }
}

export const getMe = async (): Promise<User | null> => {
  const token = localStorage.getItem('disaster_token')
  if (!token && !import.meta.env.VITE_USE_MOCKS) {
    return null
  }
  const response = await api.get<User>('/users/me')
  const user = response.data
  localStorage.setItem('disaster_user', JSON.stringify(user))
  return user
}