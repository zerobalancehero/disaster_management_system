import { createBrowserRouter } from 'react-router-dom'
import Layout from '../components/Layout'
import { HomeRedirect, RequireRole } from '../features/auth/guards'
import LoginPage from '../pages/LoginPage'
import UserPage from '../pages/UserPage'
import ResponderPage from '../pages/ResponderPage'
import AdminPage from '../pages/AdminPage'

const Forbidden = () => <p className="p-6" role="alert">403 — You do not have access to this area.</p>

export const router = createBrowserRouter([
  { path: '/', element: <HomeRedirect /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/forbidden', element: <Forbidden /> },
  { element: <RequireRole role="USER" />, children: [{ element: <Layout />, children: [{ path: '/user', element: <UserPage /> }] }] },
  { element: <RequireRole role="RESPONDER" />, children: [{ element: <Layout />, children: [{ path: '/responder', element: <ResponderPage /> }] }] },
  { element: <RequireRole role="ADMIN" />, children: [{ element: <Layout />, children: [{ path: '/admin', element: <AdminPage /> }] }] },
])
