import { z } from 'zod'
import { api } from './client'
import type { ApiError, User } from '../types'

const userSchema = z.object({
  id: z.string(),
  name: z.string(),
  role: z.enum(['USER', 'RESPONDER', 'ADMIN']),
  teamId: z.string().optional(),
})

function parseUser(data: unknown): User {
  const r = userSchema.safeParse(data)
  if (!r.success) {
    const e: ApiError = { status: 502, message: 'Unexpected API response. Is the backend (or VITE_USE_MOCKS=true) running?' }
    throw e
  }
  return r.data // zod also strips fields like password
}

export const login = (b: { username: string; password: string }) =>
  api.post('/auth/login', b).then((r) => parseUser(r.data))
export const getMe = () => api.get('/users/me').then((r) => parseUser(r.data))
export const logout = () => api.post('/auth/logout').then(() => undefined)