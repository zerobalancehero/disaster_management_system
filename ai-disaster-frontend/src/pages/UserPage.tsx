import { useMutation, useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { getActiveAlerts, reportIncident } from '../api/alerts.api'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import type { Alert, ApiError } from '../types'

const schema = z.object({
  type: z.enum(['FLOOD', 'FIRE', 'EARTHQUAKE', 'OTHER']),
  description: z.string().min(10, 'Describe the incident (min 10 characters)'),
  lat: z.coerce.number().min(-90).max(90),
  lon: z.coerce.number().min(-180).max(180),
})
type Form = z.infer<typeof schema>

export default function UserPage() {
  const alerts = useQuery<Alert[], ApiError>({ queryKey: ['alerts'], queryFn: getActiveAlerts })
  const [msg, setMsg] = useState<string | null>(null)
  const { register, handleSubmit, reset, formState: { errors } } = useForm({ resolver: zodResolver(schema) })
  const m = useMutation({
    mutationFn: reportIncident,
    onSuccess: () => { setMsg('Report submitted. It is PENDING admin verification — not a verified incident.'); reset() },
    onError: (e: ApiError) => setMsg(`Failed: ${e.message}`),
  })
  const submit = (v: unknown) => {
    const d = v as Form
    if (window.confirm('Submit this incident report?')) m.mutate(d)
  }
  return (
    <>
      <Card>
        <h2 className="mb-2 font-semibold">Active alerts</h2>
        <QueryState loading={alerts.isLoading} error={alerts.error} empty={alerts.data?.length === 0} retry={() => void alerts.refetch()} />
        <ul className="space-y-2">
          {alerts.data?.map((a) => (
            <li key={a.id} className="flex items-center justify-between rounded bg-slate-800 p-2">
              <span>{a.title} <span className="text-xs text-slate-400">· {a.area} · expires {new Date(a.expiresAt).toLocaleString()}</span></span>
              <Badge text={a.severity} />
            </li>))}
        </ul>
      </Card>
      <Card>
        <h2 className="mb-2 font-semibold">Report an incident</h2>
        <form className="grid gap-3 sm:grid-cols-2" onSubmit={handleSubmit(submit)}>
          <label className="text-sm">Type
            <select className="mt-1 w-full rounded bg-slate-800 p-2" {...register('type')}>
              {['FLOOD', 'FIRE', 'EARTHQUAKE', 'OTHER'].map((t) => <option key={t}>{t}</option>)}
            </select></label>
          <label className="text-sm">Latitude<input className="mt-1 w-full rounded bg-slate-800 p-2" {...register('lat')} /></label>
          <label className="text-sm">Longitude<input className="mt-1 w-full rounded bg-slate-800 p-2" {...register('lon')} /></label>
          <label className="text-sm sm:col-span-2">Description
            <textarea className="mt-1 w-full rounded bg-slate-800 p-2" {...register('description')} /></label>
          {Object.values(errors).map((e, i) => <p key={i} className="text-sm text-red-400 sm:col-span-2">{e?.message as string}</p>)}
          <Btn type="submit" disabled={m.isPending}>{m.isPending ? 'Submitting…' : 'Submit report'}</Btn>
        </form>
        {msg && <p role="status" className="mt-2 text-sm">{msg}</p>}
      </Card>
    </>
  )
}
