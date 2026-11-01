import type { Json } from '../api/endpoints'
const label = (k: string) => k.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').toLowerCase()
export function KV({ data, className }: { data: Json; className?: string }) {
  const rows = Object.entries(data).filter(([, v]) => v === null || ['string', 'number', 'boolean'].includes(typeof v))
  return (
    <dl className={`grid grid-cols-2 gap-3 sm:grid-cols-3 ${className ?? ''}`}>
      {rows.map(([k, v]) => (
        <div key={k} className="rounded-xl border border-edge bg-raised/60 p-3">
          <dt className="text-xs text-mute">{label(k)}</dt>
          <dd className="mono mt-1 break-words text-lg font-medium">{String(v ?? '—')}</dd>
        </div>
      ))}
    </dl>
  )
}
