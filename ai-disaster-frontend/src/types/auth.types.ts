export type UserRole = 'USER' | 'RESPONDER' | 'ADMIN'

export interface User {
  id: string
  name: string
  email?: string
  phone?: string
  role: UserRole
  active?: boolean
  teamId?: string
  createdAt?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresInMs: number
  user: User
}
