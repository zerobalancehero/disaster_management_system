import { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  useMap,
  useMapEvents,
} from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { getActiveAlerts, getSafetyInstructions, reportIncident } from '../api/alerts.api'
import { incidentsApi } from '../api/endpoints'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import type { Alert, ApiError, Incident, SafetyInstructionResponse, DisasterType } from '../types'
import '../styles/UserPage.css'

const schema = z.object({
  title: z.string().min(3, 'Title is required (min 3 characters)'),
  type: z.enum(['FLOOD', 'EARTHQUAKE', 'CYCLONE', 'LANDSLIDE', 'WILDFIRE', 'GENERAL_EMERGENCY']),
  description: z.string().min(10, 'Describe the incident (min 10 characters)'),
  lat: z.number().min(-90).max(90),
  lon: z.number().min(-180).max(180),
})
type Form = z.infer<typeof schema>

const DEFAULT_CENTER: [number, number] = [13.0827, 80.2707]

/** Fires when the user clicks anywhere on the map. */
function MapClickHandler({
  onPick,
}: {
  onPick: (lat: number, lon: number) => void
}) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

/** Flies the map when lat/lon change (from presets or manual input). */
function MapRecenter({ lat, lon }: { lat: number; lon: number }) {
  const map = useMap()
  useEffect(() => {
    const center = map.getCenter()
    if (Math.hypot(center.lat - lat, center.lng - lon) > 0.0002) {
      map.flyTo([lat, lon], Math.max(map.getZoom(), 12), { duration: 0.6 })
    }
  }, [lat, lon, map])
  return null
}

export default function UserPage() {
  const qc = useQueryClient()
  const alerts = useQuery<Alert[], ApiError>({
    queryKey: ['alerts'],
    queryFn: getActiveAlerts,
  })
  const [msg, setMsg] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: 'Waterlogging & Street Flooding',
      type: 'FLOOD',
      description:
        'Rising water levels observed blocking main road and residential ground floors.',
      lat: 13.0827,
      lon: 80.2707,
    },
  })

  const currentType = watch('type')

  const instructions = useQuery<SafetyInstructionResponse[], ApiError>({
    queryKey: ['safety-instructions', currentType],
    queryFn: () => getSafetyInstructions(currentType as DisasterType),
    staleTime: 60_000,
  })

  const myReports = useQuery<Incident[], ApiError>({
    queryKey: ['my-reports'],
    queryFn: async () => {
      const res = await incidentsApi.mine()
      return Array.isArray(res) ? (res as unknown as Incident[]) : []
    },
    refetchInterval: 15_000,
  })

  const m = useMutation({
    mutationFn: (v: Form) =>
      reportIncident({
        title: v.title,
        description: v.description,
        disasterType: v.type,
        location: { latitude: v.lat, longitude: v.lon },
      }),
    onSuccess: (data) => {
      setMsg(
        `Report #${(data as { id: string }).id.slice(0, 8)} submitted successfully. It is now PENDING admin verification.`
      )
      reset()
      qc.invalidateQueries({ queryKey: ['my-reports'] })
    },
    onError: (e: ApiError) => setMsg(`Submission Failed: ${e.message}`),
  })

  const submit = (v: Form) => {
    if (
      window.confirm('Submit this disaster incident report to Emergency Dispatch?')
    ) {
      m.mutate(v)
    }
  }

  const setLocationPreset = (lat: number, lon: number) => {
    setValue('lat', lat, { shouldValidate: true })
    setValue('lon', lon, { shouldValidate: true })
  }

  // watch lat/lon so the map stays in sync with the form
  const lat = watch('lat')
  const lon = watch('lon')
  const hasValidLocation =
    Number.isFinite(lat) &&
    Number.isFinite(lon) &&
    Math.abs(lat) <= 90 &&
    Math.abs(lon) <= 180

  const mapCenter: [number, number] = hasValidLocation
    ? [lat, lon]
    : DEFAULT_CENTER

  const isError = msg?.startsWith('Submission Failed')

  return (
    <div className="user-page">
      {/* ── Active Alerts card ──────────────────────── */}
      <Card className="user-card">
        <div className="user-card-header">
          <div className="user-card-header-left">
            <span className="user-icon user-icon-warn">⚠️</span>
            <h2 className="user-card-title">Active Disaster Alerts</h2>
          </div>
          <span className="user-card-subtitle">Live External Feeds</span>
        </div>

        <QueryState
          loading={alerts.isLoading}
          error={alerts.error}
          empty={alerts.data?.length === 0}
          retry={() => void alerts.refetch()}
        />

        <ul className="user-alert-list">
          {alerts.data?.map((a) => (
            <li key={a.id} className="user-alert-item">
              <div className="user-alert-header">
                <span className="user-alert-title">{a.title}</span>
                <Badge text={a.severity} />
              </div>
              {a.description && (
                <p className="user-alert-desc">{a.description}</p>
              )}
              <div className="user-alert-meta">
                <span>📍 Area: {a.area}</span>
                <span>
                  Valid until:{' '}
                  {new Date(a.expiresAt ?? a.expiryTime ?? '').toLocaleString()}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </Card>

      {/* ── Report Incident card ────────────────────── */}
      <Card className="user-card">
        <div className="user-card-header">
          <div className="user-card-header-left">
            <span className="user-icon">📢</span>
            <h2 className="user-card-title">Report an Emergency Incident</h2>
          </div>
        </div>

        <p className="user-card-subtitle" style={{ marginBottom: 18, display: 'block' }}>
          Citizen emergency reports are forwarded directly to emergency
          operations for instant verification and responder squad dispatch.
        </p>

        <form className="user-form-grid" onSubmit={handleSubmit(submit)}>
          {/* Title */}
          <div className="user-field user-field-full">
            <label className="user-label">Incident Headline / Title</label>
            <input
              className="user-input"
              {...register('title')}
            />
            {errors.title && (
              <p className="user-error">{errors.title.message}</p>
            )}
          </div>

          {/* Disaster type */}
          <div className="user-field">
            <label className="user-label">Disaster Category</label>
            <select className="user-select" {...register('type')}>
              {[
                'FLOOD',
                'EARTHQUAKE',
                'CYCLONE',
                'LANDSLIDE',
                'WILDFIRE',
                'GENERAL_EMERGENCY',
              ].map((t) => (
                <option key={t} value={t}>
                  {t.replace('_', ' ')}
                </option>
              ))}
            </select>
          </div>

          {/* Location preset + coords */}
          <div className="user-field">
            <div className="user-preset-header">
              <label className="user-label" style={{ margin: 0 }}>
                Location Presets
              </label>
              <div className="user-preset-group">
                <button
                  type="button"
                  className="user-preset-btn"
                  onClick={() => setLocationPreset(13.0827, 80.2707)}
                >
                  Central
                </button>
                ·
                <button
                  type="button"
                  className="user-preset-btn"
                  onClick={() => setLocationPreset(12.9784, 80.2185)}
                >
                  Velachery
                </button>
                ·
                <button
                  type="button"
                  className="user-preset-btn"
                  onClick={() => setLocationPreset(13.05, 80.2824)}
                >
                  Marina
                </button>
              </div>
            </div>
            <div className="user-coord-grid">
              <input
                type="number"
                step="any"
                placeholder="Latitude"
                className="user-input"
                {...register('lat', { valueAsNumber: true })}
              />
              <input
                type="number"
                step="any"
                placeholder="Longitude"
                className="user-input"
                {...register('lon', { valueAsNumber: true })}
              />
            </div>
          </div>

          {/* Description */}
          <div className="user-field user-field-full">
            <label className="user-label">
              Detailed Description & Stranded Persons
            </label>
            <textarea
              rows={3}
              className="user-textarea"
              {...register('description')}
            />
            {errors.description && (
              <p className="user-error">{errors.description.message}</p>
            )}
          </div>

          {/* Map preview / picker */}
          <div className="user-field user-field-full">
            <label className="user-label">Incident Location on Map</label>
            <div className="user-map-wrap">
              <div className="user-map-hint">
                Click the map to pin the incident location
              </div>
              <MapContainer
                center={mapCenter}
                zoom={12}
                scrollWheelZoom={false}
                className="user-map"
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <MapClickHandler
                  onPick={(newLat, newLon) => {
                    setValue('lat', Number(newLat.toFixed(6)), {
                      shouldValidate: true,
                    })
                    setValue('lon', Number(newLon.toFixed(6)), {
                      shouldValidate: true,
                    })
                  }}
                />
                {hasValidLocation && <MapRecenter lat={lat} lon={lon} />}
                {hasValidLocation && (
                  <CircleMarker
                    center={[lat, lon]}
                    radius={11}
                    pathOptions={{
                      color: '#14b8a6',
                      fillColor: '#14b8a6',
                      fillOpacity: 0.65,
                      weight: 2,
                    }}
                  />
                )}
              </MapContainer>
            </div>
          </div>

          {/* Submit row */}
          <div className="user-field-full user-form-footer">
            <Btn
              type="submit"
              disabled={m.isPending}
              className="user-submit"
            >
              {m.isPending
                ? 'Submitting to Dispatch...'
                : '🚨 Submit Incident Report'}
            </Btn>
          </div>
        </form>

        {msg && (
          <div
            role="status"
            className={isError ? 'user-error-banner' : 'user-success'}
          >
            {isError ? '⚠️' : '✅'} {msg}
          </div>
        )}
      </Card>

      {/* ── Safety Instructions & Survival Protocol ─── */}
      <Card className="user-card">
        <div className="user-card-header">
          <div className="user-card-header-left">
            <span className="user-icon">🛡️</span>
            <h2 className="user-card-title">Live Safety & Evacuation Protocols</h2>
          </div>
          <span className="user-card-subtitle">Hazard: {currentType.replace('_', ' ')}</span>
        </div>

        <QueryState
          loading={instructions.isLoading}
          error={instructions.error}
          empty={instructions.data?.length === 0}
          retry={() => void instructions.refetch()}
        />

        <div className="space-y-4">
          {instructions.data?.map((inst) => (
            <div key={inst.id} className="p-4 rounded-lg bg-surface/80 border border-edge">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold text-ink text-sm sm:text-base">{inst.title}</h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-sky-500/20 text-sky-300">
                  {inst.targetAudience.replace('_', ' ')}
                </span>
              </div>
              <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-line">{inst.instructionText}</p>
            </div>
          ))}
        </div>
      </Card>

      {/* ── Citizen Submitted Reports Tracking ──────── */}
      <Card className="user-card">
        <div className="user-card-header">
          <div className="user-card-header-left">
            <span className="user-icon">📋</span>
            <h2 className="user-card-title">My Emergency Reports</h2>
          </div>
          <span className="user-card-subtitle">{myReports.data?.length ?? 0} submitted</span>
        </div>

        <QueryState
          loading={myReports.isLoading}
          error={myReports.error}
          empty={myReports.data?.length === 0}
          retry={() => void myReports.refetch()}
        />

        <div className="space-y-3">
          {myReports.data?.map((r) => {
            const status = r.verificationStatus || r.status || 'PENDING'
            const statusColor =
              status === 'VERIFIED'
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                : status === 'REJECTED'
                ? 'bg-rose-500/20 text-rose-300 border-rose-500/30'
                : 'bg-amber-500/20 text-amber-300 border-amber-500/30'

            const timeStr = r.reportedTime || r.createdAt
            const timeLabel = timeStr ? new Date(timeStr).toLocaleString() : 'Recently'

            return (
              <div key={r.id} className="p-4 rounded-lg bg-surface/70 border border-edge space-y-2">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-white">{r.title || 'Emergency Report'}</span>
                    <Badge text={r.disasterType || r.type || 'EMERGENCY'} />
                  </div>
                  <span className={`text-xs px-2.5 py-0.5 rounded font-bold border ${statusColor}`}>
                    {status}
                  </span>
                </div>
                <p className="text-sm text-slate-300">{r.description}</p>
                {r.rejectionReason && (
                  <p className="text-xs text-rose-300 bg-rose-950/40 p-2 rounded border border-rose-900/50">
                    <strong>Rejection reason:</strong> {r.rejectionReason}
                  </p>
                )}
                <div className="flex items-center justify-between text-xs text-slate-400 pt-1 border-t border-edge/50">
                  <span>📍 Coordinates: {r.lat != null ? r.lat.toFixed(4) : '—'}, {r.lon != null ? r.lon.toFixed(4) : '—'}</span>
                  <span>Reported {timeLabel}</span>
                </div>
              </div>
            )
          })}
        </div>
      </Card>
    </div>
  )
}