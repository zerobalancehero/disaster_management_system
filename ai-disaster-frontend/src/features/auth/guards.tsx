import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { homePath } from '../../lib/transitions'
import type { Role } from '../../types'

export function RequireRole({ role }: { role: Role }) {
  const { user, loading } = useAuth()
  const hasToken = typeof window !== 'undefined' && Boolean(localStorage.getItem('disaster_token'))

  if (loading || (hasToken && !user)) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <div className="flex items-center gap-3 text-slate-300">
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-red-500 border-t-transparent" />
          <span className="text-sm font-medium">Restoring session…</span>
        </div>
      </div>
    )
  }

  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <Navigate to="/forbidden" replace />
  return <Outlet />
}

export function HomeRedirect() {
  const { user, loading } = useAuth()
  const hasToken = typeof window !== 'undefined' && Boolean(localStorage.getItem('disaster_token'))

  if (loading || (hasToken && !user)) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <div className="flex items-center gap-3 text-slate-300">
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-red-500 border-t-transparent" />
        </div>
      </div>
    )
  }

  return <Navigate to={user ? homePath(user.role) : '/login'} replace />
}
