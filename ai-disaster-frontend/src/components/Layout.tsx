import { Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { config } from '../lib/config'

export default function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen">
      {config.useMocks && <div className="bg-amber-500 text-slate-900 text-center text-sm font-semibold py-1">DEMO / SIMULATED DATA — not real disaster information</div>}
      <header className="flex items-center justify-between border-b border-slate-800 bg-slate-900 px-4 py-3">
        <h1 className="font-semibold">Disaster Response · {user?.role}</h1>
        <div className="flex items-center gap-3 text-sm">
          <span>{user?.name}</span>
          <button className="rounded bg-slate-700 px-3 py-1 hover:bg-slate-600" onClick={() => void logout()}>Log out</button>
        </div>
      </header>
      <main className="mx-auto max-w-4xl space-y-6 p-4"><Outlet /></main>
    </div>
  )
}
