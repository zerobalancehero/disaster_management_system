import type { ApiError } from '../types'

export const Card = ({ children }: { children: React.ReactNode }) =>
  <section className="rounded-lg border border-slate-800 bg-slate-900 p-4">{children}</section>

export const Btn = (p: React.ButtonHTMLAttributes<HTMLButtonElement>) =>
  <button {...p} className={`rounded bg-sky-600 px-3 py-1 text-sm font-medium hover:bg-sky-500 focus:outline-2 focus:outline-sky-300 disabled:opacity-50 ${p.className ?? ''}`} />

export function QueryState({ loading, error, empty, retry }: { loading: boolean; error: ApiError | null; empty?: boolean; retry: () => void }) {
  if (loading) return <div className="h-16 animate-pulse rounded bg-slate-800" aria-busy="true" />
  if (error) return (
    <div role="alert" className="text-red-400">
      {error.status === 403 ? 'Forbidden: you lack permission.' : error.message}{' '}
      <button className="underline" onClick={retry}>Retry</button>
    </div>)
  if (empty) return <p className="text-slate-400">Nothing here yet.</p>
  return null
}

const COLORS: Record<string, string> = { SEVERE: 'bg-red-600', HIGH: 'bg-orange-600', MODERATE: 'bg-yellow-600', LOW: 'bg-slate-600' }
export const Badge = ({ text }: { text: string }) =>
  <span className={`rounded px-2 py-0.5 text-xs ${COLORS[text] ?? 'bg-slate-700'}`}>{text}</span>
