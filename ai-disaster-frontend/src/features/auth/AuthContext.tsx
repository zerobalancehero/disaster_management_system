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
  const [localUser, setLocalUser] = useState<User | null>(() => authApi.getStoredUser())

  const me = useQuery<User | null, ApiError>({
    queryKey: ['me'],
    queryFn: () =>
      authApi.getMe().catch((e: ApiError) => {
        if (e.status === 401) {
          setLocalUser(null)
          return null
        }
        throw e
      }),
    staleTime: 5 * 60 * 1000,
  })

  const login = async (creds: { username: string; password: string }): Promise<User> => {
    const u = await authApi.login(creds)
    setLocalUser(u)
    qc.setQueryData(['me'], u)
    return u
  }

  const logout = async (): Promise<void> => {
    setLocalUser(null)
    qc.clear()
    qc.setQueryData(['me'], null)
    await authApi.logout()
  }

  const activeUser = me.data !== undefined ? me.data : localUser
  const hasToken = typeof window !== 'undefined' && Boolean(localStorage.getItem('disaster_token'))
  const loading = me.isLoading && !activeUser && hasToken

  return (
    <AuthCtx.Provider value={{ user: activeUser, loading, login, logout }}>
      {children}
    </AuthCtx.Provider>
  )
}
export const useAuth = () => {
  const c = useContext(AuthCtx)
  if (!c) throw new Error('AuthProvider missing')
  return c
}

