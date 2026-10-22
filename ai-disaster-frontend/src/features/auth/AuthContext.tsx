import { createContext, useContext, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as authApi from '../../api/auth.api'
import type { ApiError, User } from '../../types'

interface Ctx { user: User | null; loading: boolean; login: typeof authApi.login; logout: () => Promise<void> }
const AuthCtx = createContext<Ctx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const me = useQuery<User | null, ApiError>({
    queryKey: ['me'],
    queryFn: () => authApi.getMe().catch((e: ApiError) => { if (e.status === 401) return null; throw e }),
  })
  const loginM = useMutation({ mutationFn: authApi.login, onSuccess: (u) => qc.setQueryData(['me'], u) })
  const logoutM = useMutation({ mutationFn: authApi.logout, onSuccess: () => { qc.clear(); qc.setQueryData(['me'], null) } })
  return (
    <AuthCtx.Provider value={{ user: me.data ?? null, loading: me.isLoading, login: loginM.mutateAsync, logout: () => logoutM.mutateAsync() }}>
      {children}
    </AuthCtx.Provider>
  )
}
export const useAuth = () => { const c = useContext(AuthCtx); if (!c) throw new Error('AuthProvider missing'); return c }
