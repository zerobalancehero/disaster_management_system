import type { ApiError } from '../types'

export const Card = ({ children, className }: { children: React.ReactNode; className?: string }) => (
  <section className={`rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl backdrop-blur-sm ${className ?? ''}`}>
    {children}
  </section>
)

export const Btn = (p: React.ButtonHTMLAttributes<HTMLButtonElement>) => (
  <button
    {...p}
    className={`inline-flex items-center justify-center rounded-xl bg-sky-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-md shadow-sky-950/40 transition-all hover:bg-sky-500 hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-sky-400 disabled:opacity-50 disabled:pointer-events-none cursor-pointer ${p.className ?? ''}`}
  />
)

export function QueryState({
  loading,
  error,
  empty,
  retry,
}: {
  loading: boolean
  error: ApiError | null
  empty?: boolean
  retry: () => void
}) {
  if (loading) {
    return (
      <div className="flex items-center justify-center p-8 text-xs text-slate-400 gap-2">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-sky-400 border-t-transparent" />
        Syncing real-time emergency records...
      </div>
    )
  }
  if (error) {
    return (
      <div role="alert" className="rounded-xl border border-red-500/30 bg-red-950/20 p-4 text-xs text-red-300 flex items-center justify-between">
        <span>⚠️ {error.status === 403 ? 'Forbidden: you lack required permissions for this action.' : error.message}</span>
        <button className="underline font-semibold ml-2 hover:text-white" onClick={retry}>
          Retry
        </button>
      </div>
    )
  }
  if (empty) {
    return (
      <div className="rounded-xl border border-dashed border-slate-800 p-6 text-center text-xs text-slate-400">
        No records found currently.
      </div>
    )
  }
  return null
}

const BADGE_STYLES: Record<string, string> = {
  // Severity
  SEVERE: 'bg-red-500/10 text-red-400 border border-red-500/30 shadow-red-500/10',
  HIGH: 'bg-orange-500/10 text-orange-400 border border-orange-500/30 shadow-orange-500/10',
  MODERATE: 'bg-amber-500/10 text-amber-300 border border-amber-500/30 shadow-amber-500/10',
  LOW: 'bg-slate-500/10 text-slate-300 border border-slate-500/30',
  // Incident Status
  PENDING: 'bg-amber-500/15 text-amber-300 border border-amber-500/30 animate-pulse',
  VERIFIED: 'bg-blue-500/15 text-blue-300 border border-blue-500/30',
  REJECTED: 'bg-rose-500/15 text-rose-300 border border-rose-500/30',
  ASSIGNED: 'bg-indigo-500/15 text-indigo-300 border border-indigo-500/30',
  RESOLVED: 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30',
  // Assignment Status
  APPROVED: 'bg-cyan-500/15 text-cyan-300 border border-cyan-500/30',
  EN_ROUTE: 'bg-violet-500/15 text-violet-300 border border-violet-500/30 animate-pulse',
  ON_SCENE: 'bg-yellow-500/15 text-yellow-300 border border-yellow-500/30',
  // Availability
  AVAILABLE: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30',
  OFF_DUTY: 'bg-slate-500/15 text-slate-400 border border-slate-500/30',
}

export const Badge = ({ text }: { text: string }) => (
  <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold tracking-wide uppercase shadow-sm ${BADGE_STYLES[text] ?? 'bg-slate-800 text-slate-300 border border-slate-700'}`}>
    {text.replace('_', ' ')}
  </span>
)
