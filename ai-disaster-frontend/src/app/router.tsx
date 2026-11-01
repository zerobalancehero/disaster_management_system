import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import Layout from '../components/Layout'
import { HomeRedirect, RequireRole } from '../features/auth/guards'
import LoginPage from '../pages/LoginPage'

const UserOverview = lazy(() => import('../pages/user/UserOverview'))
const UserPage = lazy(() => import('../pages/UserPage'))            // existing report form
const ResponderAssignments = lazy(() => import('../pages/responder/ResponderAssignments'))
const AdminDashboard = lazy(() => import('../pages/admin/AdminDashboard'))
const AdminPage = lazy(() => import('../pages/AdminPage'))          // existing incident queue + AI dispatch

const S = (el: React.ReactNode) => <Suspense fallback={<p className="p-6 text-sm text-mute">Loading…</p>}>{el}</Suspense>
const Forbidden = () => <p role="alert" className="p-10 text-center">403 — your role cannot open this area.</p>
const NotFound = () => <p role="alert" className="p-10 text-center">404 — page not found.</p>

const area = (role: 'USER' | 'RESPONDER' | 'ADMIN', base: string, kids: { index?: boolean; path?: string; element: React.ReactNode }[]) => ({
  element: <RequireRole role={role} />,
  children: [{ element: <Layout />, children: [{ path: base, children: kids.map((c) => ({ ...c, element: S(c.element) })) }] }],
})

export const router = createBrowserRouter([
  { path: '/', element: <HomeRedirect /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/forbidden', element: <Forbidden /> },
  area('USER', '/user', [{ index: true, element: <UserOverview /> }, { path: 'report', element: <UserPage /> }]),
  area('RESPONDER', '/responder', [{ index: true, element: <ResponderAssignments /> }]),
  area('ADMIN', '/admin', [{ index: true, element: <AdminDashboard /> }, { path: 'incidents', element: <AdminPage /> }]),
  { path: '*', element: <NotFound /> },
])
