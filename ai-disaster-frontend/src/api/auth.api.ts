import { z } from 'zod'
import { api } from './client'
import type { ApiError, User } from '../types'

const userSchema = z.object({
  id: z.string(),
  name: z.string(),
  role: z.enum(['USER', 'RESPONDER', 'ADMIN']),
  email: z.string().optional(),
  phone: z.string().optional(),
  teamId: z.string().optional(),
})

function normalizeUserPayload(raw: unknown): User {
  if (!raw || typeof raw !== 'object') {
    throw { status: 502, message: 'Invalid user payload received' } as ApiError
  }
  const obj = raw as Record<string, unknown>
  const candidate = {
    id: String(obj.id ?? ''),
    name: String(obj.name ?? obj.fullName ?? obj.username ?? 'User'),
    role: String(obj.role ?? 'USER').toUpperCase() as User['role'],
    email: obj.email ? String(obj.email) : undefined,
    phone: obj.phone ? String(obj.phone) : undefined,
    teamId: obj.teamId ? String(obj.teamId) : undefined,
  }

  const r = userSchema.safeParse(candidate)
  if (!r.success) {
    throw { status: 502, message: 'Failed to parse user role or profile' } as ApiError
  }
  return r.data
}

// Convert shorthand username to mock/seed email if needed
function resolveEmail(identifier: string): string {
  const trimmed = identifier.trim()
  if (trimmed.includes('@')) return trimmed
  const lower = trimmed.toLowerCase()
  if (lower === 'admin') return 'admin@disaster.local'
  if (lower === 'responder') return 'responder@disaster.local'
  if (lower === 'user') return 'user@disaster.local'
  return `${lower}@disaster.local`
}

export const login = async (b: { username: string; password: string }): Promise<User> => {
  const email = resolveEmail(b.username)
  const payload = {
    email,
    password: b.password,
    username: b.username, // keep for mock handlers if enabled
  }

  const response = await api.post('/auth/login', payload)
  const data = response.data

  // Spring Boot backend returns: { token: "...", tokenType: "Bearer", user: { ... } }
  if (data && typeof data === 'object' && 'token' in data) {
    const token = String((data as { token: string }).token)
    localStorage.setItem('disaster_token', token)
    const rawUser = (data as { user?: unknown }).user ?? data
    return normalizeUserPayload(rawUser)
  }

  // Fallback for mock service worker returning direct user
  return normalizeUserPayload(data)
}

export const getMe = async (): Promise<User | null> => {
  const token = localStorage.getItem('disaster_token')
  if (!token && !import.meta.env.VITE_USE_MOCKS) {
    return null
  }
  try {
    const r = await api.get('/users/me')
    return normalizeUserPayload(r.data)
  } catch (err) {
    const error = err as ApiError
    if (error.status === 401) {
      localStorage.removeItem('disaster_token')
      return null
    }
    throw error
  }
}

export const logout = async (): Promise<void> => {
  localStorage.removeItem('disaster_token')
  try {
    await api.post('/auth/logout')
  } catch {
    // Backend JWT logout is stateless
  }
}