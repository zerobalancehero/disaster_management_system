import { useQuery } from '@tanstack/react-query'
import { adminApi, type Json } from '../../api/endpoints'
import { Card, QueryState } from '../../components/ui'
import { KV } from '../../components/KV'
import type { ApiError } from '../../types'

// Renders whatever /admin/dashboard returns: scalars as KPI tiles, nested objects as their own panels.
export default function AdminDashboard() {
  const q = useQuery<Json, ApiError>({ queryKey: ['dashboard'], queryFn: adminApi.dashboard, refetchInterval: 30_000 })
  const nested = Object.entries(q.data ?? {}).filter(([, v]) => v && typeof v === 'object' && !Array.isArray(v))
  return (
    <>
      <h1 className="text-xl font-bold">Command center</h1>
      <QueryState loading={q.isLoading} error={q.error} retry={() => void q.refetch()} />
      {q.data && <KV data={q.data} className="sm:grid-cols-4" />}
      {nested.map(([k, v]) => (
        <Card key={k}><h2 className="mb-3 font-bold">{k.replace(/([A-Z])/g, ' $1').toLowerCase()}</h2><KV data={v as Json} /></Card>
      ))}
    </>
  )
}
