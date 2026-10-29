import { createContext, useContext, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import * as authApi from '../../api/auth.api'
import type { ApiError, User } from '../../types'

interface Ctx {
  user: User | null
  loading: boolean
  login: (creds: { username: string; password: string }) => Promise<User>
  logout: () => Promise<void>
}
const AuthCtx = createContext<Ctx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [user, setUser] = useState<User | null>(() => {
    if (typeof window !== 'undefined' && !localStorage.getItem('disaster_token')) {
      return null
    }
    return authApi.getStoredUser()
  })

  const me = useQuery<User | null, ApiError>({
    queryKey: ['me'],
    queryFn: async () => {
      try {
        const u = await authApi.getMe()
        if (u) {
          setUser(u)
        }
        return u
      } catch (e) {
        const err = e as ApiError
        if (err.status === 401) {
          setUser(null)
          return null
        }
        throw e
      }
    },
    staleTime: 5 * 60 * 1000,
  })

  const login = async (creds: { username: string; password: string }): Promise<User> => {
    const u = await authApi.login(creds)
    setUser(u)
    qc.setQueryData(['me'], u)
    return u
  }

  const logout = async (): Promise<void> => {
    setUser(null)
    qc.setQueryData(['me'], null)
    qc.removeQueries()
    await authApi.logout()
  }

  const hasToken = typeof window !== 'undefined' && Boolean(localStorage.getItem('disaster_token'))
  const loading = me.isLoading && !user && hasToken

  return (
    <AuthCtx.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthCtx.Provider>
  )
}
export const useAuth = () => {
  const c = useContext(AuthCtx)
  if (!c) throw new Error('AuthProvider missing')
  return c
}

