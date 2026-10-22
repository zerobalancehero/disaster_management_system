import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { homePath } from '../../lib/transitions'
import type { Role } from '../../types'

export function RequireRole({ role }: { role: Role }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="p-6" aria-busy="true">Restoring session…</p>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== role) return <Navigate to="/forbidden" replace />
  return <Outlet />
}
export function HomeRedirect() {
  const { user, loading } = useAuth()
  if (loading) return <p className="p-6">Loading…</p>
  return <Navigate to={user ? homePath(user.role) : '/login'} replace />
}
