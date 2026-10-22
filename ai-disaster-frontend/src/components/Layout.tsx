import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { config } from '../lib/config'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      navigate('/login', { replace: true })
    }
  }

  const roleTheme = {
    ADMIN: 'from-red-600/20 to-transparent border-red-500/30 text-red-400',
    RESPONDER: 'from-blue-600/20 to-transparent border-blue-500/30 text-blue-400',
    USER: 'from-emerald-600/20 to-transparent border-emerald-500/30 text-emerald-400',
  }[user?.role ?? 'USER']

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {config.useMocks && (
        <div className="bg-amber-500/90 text-slate-950 text-center text-xs font-bold py-1 tracking-wide uppercase">
          ⚠️ Mock / Simulated Mode Active (VITE_USE_MOCKS=true)
        </div>
      )}
      <header className="sticky top-0 z-40 border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-md px-6 py-3.5 shadow-md">
        <div className="mx-auto max-w-6xl flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-red-600/20 text-red-500 border border-red-500/30 shadow-inner">
              ⚡
            </span>
            <div>
              <h1 className="font-bold text-sm tracking-tight text-white flex items-center gap-2">
                Disaster Emergency Ops
                <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${roleTheme}`}>
                  {user?.role} PORTAL
                </span>
              </h1>
              <p className="text-[11px] text-slate-400">Autonomous Incident Response & GIS Command</p>
            </div>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <div className="flex flex-col text-right">
              <span className="font-semibold text-slate-200">{user?.name}</span>
              <span className="text-[10px] text-slate-400">{user?.email || 'Authenticated User'}</span>
            </div>
            <button
              className="rounded-xl border border-slate-700 bg-slate-800/80 px-3 py-1.5 font-medium text-slate-300 hover:bg-red-500/10 hover:border-red-500/30 hover:text-red-400 transition-all cursor-pointer"
              onClick={handleLogout}
            >
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl w-full flex-1 p-6 space-y-6">
        <Outlet />
      </main>
    </div>
  )
}

