import { useMutation, useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { getActiveAlerts, reportIncident } from '../api/alerts.api'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import type { Alert, ApiError } from '../types'

const schema = z.object({
  title: z.string().min(3, 'Title is required (min 3 characters)'),
  type: z.enum(['FLOOD', 'EARTHQUAKE', 'CYCLONE', 'LANDSLIDE', 'WILDFIRE', 'GENERAL_EMERGENCY']),
  description: z.string().min(10, 'Describe the incident (min 10 characters)'),
  lat: z.number().min(-90).max(90),
  lon: z.number().min(-180).max(180),
})
type Form = z.infer<typeof schema>

export default function UserPage() {
  const alerts = useQuery<Alert[], ApiError>({ queryKey: ['alerts'], queryFn: getActiveAlerts })
  const [msg, setMsg] = useState<string | null>(null)
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: 'Waterlogging & Street Flooding',
      type: 'FLOOD',
      description: 'Rising water levels observed blocking main road and residential ground floors.',
      lat: 13.0827,
      lon: 80.2707,
    },
  })
  const m = useMutation({
    mutationFn: reportIncident,
    onSuccess: (data) => {
      setMsg(`Report #${data.id.slice(0, 8)} submitted successfully. It is now PENDING admin verification.`)
      reset()
    },
    onError: (e: ApiError) => setMsg(`Submission Failed: ${e.message}`),
  })
  const submit = (v: Form) => {
    if (window.confirm('Submit this disaster incident report to Emergency Dispatch?')) {
      m.mutate(v)
    }
  }

  const setLocationPreset = (lat: number, lon: number) => {
    setValue('lat', lat, { shouldValidate: true })
    setValue('lon', lon, { shouldValidate: true })
  }

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <span className="text-xl">⚠️</span>
            <h2 className="font-bold text-lg text-white">Active Disaster Alerts</h2>
          </div>
          <span className="text-xs text-slate-400">Live External Feeds</span>
        </div>
        <QueryState loading={alerts.isLoading} error={alerts.error} empty={alerts.data?.length === 0} retry={() => void alerts.refetch()} />
        <ul className="space-y-3">
          {alerts.data?.map((a) => (
            <li key={a.id} className="rounded-xl border border-red-500/30 bg-red-950/20 p-4 transition-all hover:bg-red-950/30">
              <div className="flex items-center justify-between mb-1">
                <span className="font-semibold text-red-200">{a.title}</span>
                <Badge text={a.severity} />
              </div>
              {a.description && <p className="text-xs text-slate-300 mt-1 mb-2">{a.description}</p>}
              <div className="flex items-center justify-between text-[11px] text-slate-400">
                <span>📍 Area: {a.area}</span>
                <span>Valid until: {new Date(a.expiresAt).toLocaleString()}</span>
              </div>
            </li>))}
        </ul>
      </Card>

      <Card>
        <div className="flex items-center gap-2 mb-3">
          <span className="text-xl">📢</span>
          <h2 className="font-bold text-lg text-white">Report an Emergency Incident</h2>
        </div>
        <p className="text-xs text-slate-400 mb-4">
          Citizen emergency reports are forwarded directly to emergency operations for instant verification and responder squad dispatch.
        </p>

        <form className="grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit(submit)}>
          <div className="sm:col-span-2">
            <label className="block text-xs font-medium text-slate-300 mb-1">Incident Headline / Title</label>
            <input className="w-full rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-sm text-white focus:border-red-500 focus:outline-none" {...register('title')} />
            {errors.title && <p className="mt-1 text-xs text-red-400">{errors.title.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Disaster Category</label>
            <select className="w-full rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-sm text-white focus:border-red-500 focus:outline-none" {...register('type')}>
              {['FLOOD', 'EARTHQUAKE', 'CYCLONE', 'LANDSLIDE', 'WILDFIRE', 'GENERAL_EMERGENCY'].map((t) => (
                <option key={t} value={t}>{t.replace('_', ' ')}</option>
              ))}
            </select>
          </div>

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs font-medium text-slate-300">Location Presets</label>
              <div className="flex gap-1 text-[10px]">
                <button type="button" onClick={() => setLocationPreset(13.0827, 80.2707)} className="text-red-400 hover:underline cursor-pointer">Central</button> ·
                <button type="button" onClick={() => setLocationPreset(12.9784, 80.2185)} className="text-red-400 hover:underline cursor-pointer">Velachery</button> ·
                <button type="button" onClick={() => setLocationPreset(13.0500, 80.2824)} className="text-red-400 hover:underline cursor-pointer">Marina</button>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <input type="number" step="any" placeholder="Latitude" className="w-full rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-sm text-white focus:border-red-500 focus:outline-none" {...register('lat', { valueAsNumber: true })} />
              <input type="number" step="any" placeholder="Longitude" className="w-full rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-sm text-white focus:border-red-500 focus:outline-none" {...register('lon', { valueAsNumber: true })} />
            </div>
          </div>

          <div className="sm:col-span-2">
            <label className="block text-xs font-medium text-slate-300 mb-1">Detailed Description & Stranded Persons</label>
            <textarea rows={3} className="w-full rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-sm text-white focus:border-red-500 focus:outline-none" {...register('description')} />
            {errors.description && <p className="mt-1 text-xs text-red-400">{errors.description.message}</p>}
          </div>

          <div className="sm:col-span-2 flex items-center justify-between pt-2">
            <Btn type="submit" disabled={m.isPending} className="bg-red-600 hover:bg-red-500 font-semibold px-6 py-2.5 shadow-lg shadow-red-950/50">
              {m.isPending ? 'Submitting to Dispatch...' : '🚨 Submit Incident Report'}
            </Btn>
          </div>
        </form>

        {msg && (
          <div role="status" className="mt-4 rounded-lg border border-emerald-500/30 bg-emerald-950/20 p-3 text-xs text-emerald-300">
            ✅ {msg}
          </div>
        )}
      </Card>
    </div>
  )
}
