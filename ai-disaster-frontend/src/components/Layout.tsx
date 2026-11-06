import { useState, useSyncExternalStore } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Activity, Bell, ClipboardList, LayoutDashboard, LogOut, Menu, PanelLeft, PlusCircle, Radio, ShieldAlert, WifiOff, X } from 'lucide-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../features/auth/AuthContext'
import { responderApi } from '../api/endpoints'
import { config } from '../lib/config'
import type { Role } from '../types'

const NAV: Record<Role, { to: string; label: string; icon: typeof Menu; end?: boolean }[]> = {
  USER: [
    { to: '/user', label: 'Overview', icon: LayoutDashboard, end: true },
    { to: '/user/report', label: 'Report emergency', icon: PlusCircle },
  ],
  RESPONDER: [{ to: '/responder', label: 'My assignments', icon: ClipboardList, end: true }],
  ADMIN: [
    { to: '/admin', label: 'Command center', icon: Radio, end: true },
    { to: '/admin/incidents', label: 'Incidents & dispatch', icon: ShieldAlert },
  ],
}
const ACCENT: Record<Role, string> = { USER: 'text-ok', RESPONDER: 'text-accent', ADMIN: 'text-crit' }

function useOnline() {
  return useSyncExternalStore(
    (cb) => { addEventListener('online', cb); addEventListener('offline', cb); return () => { removeEventListener('online', cb); removeEventListener('offline', cb) } },
    () => navigator.onLine)
}

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const qc = useQueryClient()
  const [open, setOpen] = useState(false)
  const [slim, setSlim] = useState(false)
  const [showNotifs, setShowNotifs] = useState(false)
  const isOnline = useOnline()
  const role = user?.role ?? 'USER'

  const notifsQ = useQuery({
    queryKey: ['notifications', user?.id],
    queryFn: async () => {
      try {
        const res = role === 'RESPONDER' ? await responderApi.notifications() : await responderApi.userNotifications()
        return Array.isArray(res) ? res : []
      } catch {
        return []
      }
    },
    enabled: !!user,
    refetchInterval: 20_000,
  })

  const markReadMutation = useMutation({
    mutationFn: (id: string) => (role === 'RESPONDER' ? responderApi.markRead(id) : responderApi.markUserRead(id)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['notifications', user?.id] })
    },
  })

  const unreadCount = (notifsQ.data ?? []).filter((n: any) => !n.read && !n.isRead).length

  const links = (
    <nav aria-label="Primary" className="flex flex-col gap-1 p-3">
      {NAV[role].map(({ to, label, icon: Icon, end }) => (
        <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}
          className={({ isActive }) => `flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors ${isActive ? 'bg-raised text-ink ring-1 ring-edge' : 'text-mute hover:bg-surface hover:text-ink'}`}>
          <Icon size={18} aria-hidden /> {!slim && <span>{label}</span>}
        </NavLink>
      ))}
    </nav>
  )

  return (
    <div className="flex min-h-screen">
      <aside className={`hidden shrink-0 border-r border-edge bg-side md:block ${slim ? 'w-16' : 'w-60'}`}>
        <div className="flex h-14 items-center gap-2 border-b border-edge px-4">
          <Activity size={20} className={ACCENT[role]} aria-hidden />
          {!slim && <span className="font-bold tracking-tight">Disaster Ops</span>}
        </div>
        {links}
      </aside>

      {open && (
        <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <button className="absolute inset-0 bg-black/60 backdrop-blur-sm" aria-label="Close menu" onClick={() => setOpen(false)} />
          <aside className="relative h-full w-64 border-r border-edge bg-side">
            <button className="absolute right-3 top-3 p-2" aria-label="Close" onClick={() => setOpen(false)}><X size={18} /></button>
            <div className="h-14" />{links}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        {config.useMocks && <div className="bg-warn px-3 py-1 text-center text-xs font-semibold text-bg">Demo data — not connected to the backend</div>}
        {!isOnline && <div role="status" className="flex items-center justify-center gap-2 bg-crit px-3 py-1 text-xs font-semibold"><WifiOff size={14} /> You are offline. Showing cached data; new reports will not send.</div>}
        <header className="sticky top-0 z-40 flex h-14 items-center gap-3 border-b border-edge bg-bg2/80 px-4 backdrop-blur-md">
          <button className="p-2 md:hidden" aria-label="Open menu" onClick={() => setOpen(true)}><Menu size={20} /></button>
          <button className="hidden p-2 text-mute hover:text-ink md:block" aria-label="Collapse sidebar" onClick={() => setSlim((s) => !s)}><PanelLeft size={18} /></button>
          <div className="ml-auto flex items-center gap-3 text-sm">
            {/* Notification Bell */}
            <div className="relative">
              <button
                type="button"
                className="relative p-2 text-mute hover:text-ink rounded-lg hover:bg-raised transition-colors"
                aria-label="Notifications"
                onClick={() => setShowNotifs((s) => !s)}
              >
                <Bell size={18} />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-crit text-[10px] font-bold text-white">
                    {unreadCount}
                  </span>
                )}
              </button>

              {showNotifs && (
                <div className="absolute right-0 mt-2 w-80 rounded-xl border border-edge bg-side p-3 shadow-2xl z-50 space-y-2">
                  <div className="flex items-center justify-between pb-2 border-b border-edge">
                    <span className="font-semibold text-xs text-ink uppercase tracking-wider">Alerts & Notifications</span>
                    <span className="text-[11px] text-mute">{unreadCount} unread</span>
                  </div>
                  <div className="max-h-64 overflow-y-auto space-y-2">
                    {(notifsQ.data ?? []).length === 0 ? (
                      <p className="text-xs text-mute py-4 text-center">No notifications yet</p>
                    ) : (
                      (notifsQ.data ?? []).map((n: any) => {
                        const isUnread = !n.read && !n.isRead
                        return (
                          <div
                            key={n.id}
                            className={`p-2.5 rounded-lg border text-xs space-y-1 transition-colors ${
                              isUnread ? 'bg-raised/80 border-sky-500/30' : 'bg-surface/40 border-edge/50'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <span className="font-semibold text-white">{n.title}</span>
                              {isUnread && (
                                <button
                                  type="button"
                                  className="text-[10px] text-sky-400 hover:underline"
                                  onClick={() => markReadMutation.mutate(n.id)}
                                >
                                  Mark read
                                </button>
                              )}
                            </div>
                            <p className="text-slate-300 text-[11px] leading-snug">{n.message}</p>
                            <span className="text-[10px] text-slate-500 block">
                              {n.createdAt ? new Date(n.createdAt).toLocaleTimeString() : 'Recently'}
                            </span>
                          </div>
                        )
                      })
                    )}
                  </div>
                </div>
              )}
            </div>

            <div className="text-right leading-tight">
              <div className="font-semibold">{user?.name}</div>
              <div className={`mono text-[11px] ${ACCENT[role]}`}>{role}</div>
            </div>
            <button className="flex min-h-11 items-center gap-2 rounded-lg border border-edge px-3 text-mute hover:border-crit hover:text-crit"
              onClick={async () => { await logout(); nav('/login', { replace: true }) }}>
              <LogOut size={16} /> <span className="hidden sm:inline">Log out</span>
            </button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 space-y-6 p-4 sm:p-6"><Outlet /></main>
      </div>
    </div>
  )
}
