import { useQuery } from '@tanstack/react-query'
import { Phone } from 'lucide-react'
import { alertsApi, weatherApi, type Json } from '../../api/endpoints'
import { Badge, Card, QueryState } from '../../components/ui'
import { KV } from '../../components/KV'
import type { ApiError } from '../../types'

const LAT = 13.0827, LON = 80.2707 // Chennai default until the geolocation picker lands
const SOS = [['112', 'National emergency'], ['108', 'Ambulance'], ['101', 'Fire & rescue']]

export default function UserOverview() {
  const alerts = useQuery<Json[], ApiError>({ queryKey: ['alerts'], queryFn: alertsApi.active, refetchInterval: 60_000 })
  const wx = useQuery<Json, ApiError>({ queryKey: ['wx', LAT, LON], queryFn: () => weatherApi.current(LAT, LON), refetchInterval: 300_000 })
  const top = alerts.data?.find((a) => a.severity === 'SEVERE')

  return (
    <>
      {top && (
        <div role="alert" className="rounded-xl border border-crit/50 bg-crit/10 p-4">
          <div className="flex items-center justify-between gap-3">
            <strong>{String(top.headline ?? top.title ?? 'Severe alert in effect')}</strong><Badge text="SEVERE" />
          </div>
          {top.description != null && <p className="mt-1 text-sm text-mute">{String(top.description)}</p>}
        </div>
      )}
      <Card>
        <h2 className="mb-3 text-lg font-bold">Active alerts</h2>
        <QueryState loading={alerts.isLoading} error={alerts.error} empty={alerts.data?.length === 0} retry={() => void alerts.refetch()} />
        <ul className="space-y-3">
          {alerts.data?.map((a, i) => (
            <li key={String(a.id ?? i)} className="rounded-xl border border-edge bg-raised/50 p-4">
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold">{String(a.headline ?? a.title ?? 'Alert')}</span>
                <Badge text={String(a.severity ?? 'LOW')} />
              </div>
              {a.description != null && <p className="mt-1 text-sm text-mute">{String(a.description)}</p>}
            </li>
          ))}
        </ul>
      </Card>
      <Card>
        <h2 className="mb-1 text-lg font-bold">Current weather</h2>
        <p className="mono mb-3 text-xs text-mute">{LAT}, {LON}</p>
        <QueryState loading={wx.isLoading} error={wx.error} retry={() => void wx.refetch()} />
        {wx.data && <KV data={wx.data} />}
      </Card>

      <nav aria-label="Emergency numbers" className="fixed bottom-4 right-4 z-30 flex flex-col items-end gap-2">
        {SOS.map(([n, l]) => (
          <a key={n} href={`tel:${n}`} className="sos-pulse flex min-h-12 items-center gap-2 rounded-full bg-crit px-4 text-sm font-bold shadow-lg">
            <Phone size={16} aria-hidden /> <span className="mono">{n}</span> <span className="hidden font-medium sm:inline">{l}</span>
          </a>
        ))}
      </nav>
    </>
  )
}
