import { useState, useEffect, useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MapContainer, TileLayer, CircleMarker, Popup, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { getMyAssignments } from '../../api/responder.api'
import { responderApi, type AssignmentStatus } from '../../api/endpoints'
import { Badge, Btn, Card, QueryState } from '../../components/ui'
import { nextStatuses } from '../../lib/transitions'
import type { ApiError, AssignmentView } from '../../types'
import '../../styles/ResponderAssignments.css'

const DEFAULT_MSG: Record<AssignmentStatus, string> = {
  APPROVED: 'Assignment approved',
  ACCEPTED: 'Assignment accepted by response team',
  EN_ROUTE: 'Response team is moving to the incident location',
  ON_SCENE: 'Response team has arrived on scene',
  RESOLVED: 'Incident resolved by response team',
}

const STATUS_COLOR: Record<AssignmentStatus, string> = {
  APPROVED: '#22d3ee',
  ACCEPTED: '#818cf8',
  EN_ROUTE: '#60a5fa',
  ON_SCENE: '#fbbf24',
  RESOLVED: '#34d399',
}

const gps = () => new Promise<{ latitude: number; longitude: number } | null>((res) =>
  navigator.geolocation ? navigator.geolocation.getCurrentPosition(
    (p) => res({ latitude: p.coords.latitude, longitude: p.coords.longitude }),
    () => res(null), { timeout: 4000 }) : res(null)
)

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap()
  useEffect(() => {
    if (points.length === 0) return
    if (points.length === 1) {
      map.setView(points[0], 13)
    } else {
      map.fitBounds(points, { padding: [50, 50], maxZoom: 14 })
    }
  }, [points, map])
  return null
}

export default function ResponderAssignments() {
  const qc = useQueryClient()
  const [team, setTeam] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)

  const list = useQuery<AssignmentView[], ApiError>({
    queryKey: ['my-assignments'],
    queryFn: getMyAssignments,
    refetchInterval: 30_000,
  })

  const move = useMutation<unknown, ApiError, { id: string; s: AssignmentStatus }>({
    mutationFn: async ({ id, s }) => responderApi.setStatus(id, {
      status: s,
      message: DEFAULT_MSG[s],
      location: await gps(),
    }),
    onSettled: () => void qc.invalidateQueries({ queryKey: ['my-assignments'] }),
  })

  const duty = useMutation<unknown, ApiError, 'AVAILABLE' | 'OFF_DUTY'>({
    mutationFn: responderApi.setTeamStatus,
    onSuccess: (_d, v) => setTeam(v),
  })

  const toggle = (v: 'AVAILABLE' | 'OFF_DUTY') => {
    if (window.confirm(`Set the team to ${v.replace('_', ' ')}?`)) duty.mutate(v)
  }

  const points = useMemo<[number, number][]>(
    () => (list.data ?? []).map((a) => [a.incident.lat, a.incident.lon] as [number, number]),
    [list.data]
  )

  return (
    <div className="responder-page">

      {/* Team status card */}
      <Card className="responder-card responder-team-card">
        <div className="responder-team-row">
          <div>
            <h2 className="responder-h2">Team status</h2>
            <p className="responder-subtle">
              {team ? <>Now <Badge text={team} /></> : 'Controls whether dispatch can recommend your team.'}
            </p>
          </div>
          <div className="responder-btn-group">
            <Btn
              className="responder-btn responder-btn-success"
              disabled={duty.isPending}
              onClick={() => toggle('AVAILABLE')}
            >
              Go available
            </Btn>
            <Btn
              className="responder-btn responder-btn-neutral"
              disabled={duty.isPending}
              onClick={() => toggle('OFF_DUTY')}
            >
              Go off duty
            </Btn>
          </div>
        </div>
        {duty.error && <p role="alert" className="responder-alert">{duty.error.message}</p>}
      </Card>

      {/* Assignments card */}
      <Card className="responder-card">
        <h2 className="responder-h2">
          My assignments <span className="responder-count">({list.data?.length ?? 0})</span>
        </h2>
        <QueryState
          loading={list.isLoading}
          error={list.error}
          empty={list.data?.length === 0}
          retry={() => void list.refetch()}
        />

        {/* Map — only render when there are points */}
        {points.length > 0 && (
          <div className="responder-map-wrap">
            <div className="responder-map-legend">
              {(Object.keys(STATUS_COLOR) as AssignmentStatus[]).map((s) => (
                <span key={s} className="responder-legend-item">
                  <span className="responder-legend-dot" style={{ background: STATUS_COLOR[s] }} />
                  {s.replace('_', ' ')}
                </span>
              ))}
            </div>
            <MapContainer
              center={points[0]}
              zoom={12}
              scrollWheelZoom={false}
              className="responder-map"
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <FitBounds points={points} />
              {list.data?.map((a) => {
                const isSelected = selected === a.id
                const color = STATUS_COLOR[a.status]
                return (
                  <CircleMarker
                    key={a.id}
                    center={[a.incident.lat, a.incident.lon]}
                    radius={isSelected ? 14 : 10}
                    pathOptions={{
                      color,
                      fillColor: color,
                      fillOpacity: isSelected ? 0.85 : 0.55,
                      weight: 2,
                    }}
                    eventHandlers={{
                      click: () => setSelected(a.id),
                    }}
                  >
                    <Popup>
                      <strong>{a.incident.title || `${a.incident.type} mission`}</strong>
                      <br />
                      <span className="responder-popup-status" style={{ color }}>
                        {a.status.replace('_', ' ')}
                      </span>
                      <br />
                      <small>{a.incident.description}</small>
                      <br />
                      <a
                        href={`https://www.google.com/maps/dir/?api=1&destination=${a.incident.lat},${a.incident.lon}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Get directions →
                      </a>
                    </Popup>
                  </CircleMarker>
                )
              })}
            </MapContainer>
          </div>
        )}

        {move.error && <p role="alert" className="responder-alert">{move.error.message}</p>}

        <ul className="responder-list">
          {list.data?.map((a) => (
            <li
              key={a.id}
              className={`responder-item${selected === a.id ? ' is-selected' : ''}`}
              onMouseEnter={() => setSelected(a.id)}
              onMouseLeave={() => setSelected(null)}
            >
              <div className="responder-item-header">
                <strong className="responder-item-title">
                  {a.incident.title || `${a.incident.type} mission`}
                </strong>
                <Badge text={a.status} />
              </div>
              <p className="responder-item-desc">{a.incident.description}</p>
              <div className="responder-item-footer">
                <a
                  className="responder-map-link"
                  target="_blank"
                  rel="noreferrer"
                  href={`https://www.google.com/maps/dir/?api=1&destination=${a.incident.lat},${a.incident.lon}`}
                >
                  Directions · {a.incident.lat.toFixed(4)}, {a.incident.lon.toFixed(4)}
                </a>
                <div className="responder-item-actions">
                  {nextStatuses(a.status).map((s) => (
                    <Btn
                      key={s}
                      className="responder-btn responder-btn-sm"
                      disabled={move.isPending}
                      onClick={() => {
                        if (window.confirm(
                          `Mark this mission ${s.replace('_', ' ')}? Your GPS position will be sent if available.`
                        )) move.mutate({ id: a.id, s })
                      }}
                    >
                      Mark {s.replace('_', ' ').toLowerCase()}
                    </Btn>
                  ))}
                  {!nextStatuses(a.status).length && (
                    <span className="responder-complete">Mission complete</span>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}