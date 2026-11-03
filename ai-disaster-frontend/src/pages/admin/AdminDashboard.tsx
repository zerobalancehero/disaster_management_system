import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ShieldAlert,
  Users,
  Activity,
  Cpu,
  Database,
  Radio,
  Clock,
  Layers,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Zap,
  TrendingUp,
  UserCheck,
  Award,
} from 'lucide-react'
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Tooltip,
  ZoomControl,
  useMap,
} from 'react-leaflet'
import { divIcon } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { adminApi, alertsApi, type Json } from '../../api/endpoints'
import { getIncidents } from '../../api/admin.api'
import { Badge, Card, QueryState } from '../../components/ui'
import type { ApiError, Incident } from '../../types'
import type {
  AdminDashboardResponse,
  ResponsePerformanceReport,
  DataSourceStatusResponse,
  TeamResponse,
} from '../../types/admin.types'
import '../../styles/AdminDashboard.css'

type TabType = 'overview' | 'teams' | 'users' | 'datasources' | 'performance'

const DEFAULT_CENTER: [number, number] = [13.0827, 80.2707]

/* ─────────────── Map helpers ─────────────── */

type MapLayerMode = 'ALL' | 'CITIZEN' | 'ALERTS'

const str = (v: unknown): string | null =>
  typeof v === 'string' ? v : typeof v === 'number' ? String(v) : null

const num = (v: unknown): number | null => {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'string') {
    const n = parseFloat(v)
    return Number.isFinite(n) ? n : null
  }
  return null
}

const COORD_RE = /\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\)/

const coordsFromText = (text: string | null): [number, number] | null => {
  if (!text) return null
  const m = text.match(COORD_RE)
  if (!m) return null
  const lat = parseFloat(m[1])
  const lon = parseFloat(m[2])
  if (
    !Number.isFinite(lat) ||
    !Number.isFinite(lon) ||
    Math.abs(lat) > 90 ||
    Math.abs(lon) > 180
  ) {
    return null
  }
  return [lat, lon]
}

const AREA_COORDS: Record<string, [number, number]> = {
  central: [13.0827, 80.2707],
  chennai: [13.0827, 80.2707],
  velachery: [12.9784, 80.2185],
  marina: [13.05, 80.2824],
  adyar: [13.0012, 80.2565],
  mylapore: [13.0339, 80.2693],
}

const coordsForAlert = (alert: Json, index: number): [number, number] => {
  const lat = num(alert.lat) ?? num(alert.latitude)
  const lon = num(alert.lon) ?? num(alert.lng) ?? num(alert.longitude)
  if (
    lat != null &&
    lon != null &&
    Math.abs(lat) <= 90 &&
    Math.abs(lon) <= 180
  ) {
    return [lat, lon]
  }
  const fromDesc = coordsFromText(str(alert.description))
  if (fromDesc) return fromDesc
  const area = (str(alert.area) ?? '').toLowerCase().trim()
  for (const [key, coords] of Object.entries(AREA_COORDS)) {
    if (area && area.includes(key)) return coords
  }
  const hash = String(alert.id ?? index)
    .split('')
    .reduce((s, c) => s + c.charCodeAt(0), 0)
  const offsetLat = ((hash % 100) / 100 - 0.5) * 0.08
  const offsetLon = (((hash >> 3) % 100) / 100 - 0.5) * 0.08
  return [DEFAULT_CENTER[0] + offsetLat, DEFAULT_CENTER[1] + offsetLon]
}

const STATUS_COLORS: Record<string, { color: string; label: string }> = {
  PENDING: { color: '#fbbf24', label: 'Pending' },
  VERIFIED: { color: '#38bdf8', label: 'Verified' },
  ASSIGNED: { color: '#c084fc', label: 'Assigned' },
  RESOLVED: { color: '#34d399', label: 'Resolved' },
  REJECTED: { color: '#94a3b8', label: 'Rejected' },
}

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#ef4444',
  EXTREME: '#ef4444',
  SEVERE: '#f97316',
  HIGH: '#f97316',
  MODERATE: '#eab308',
  MEDIUM: '#eab308',
  MINOR: '#38bdf8',
  LOW: '#14b8a6',
}

const statusColor = (s: string) => STATUS_COLORS[s]?.color ?? '#38bdf8'

const markerIcon = (status: string) =>
  divIcon({
    className: `ad-marker-wrap ad-marker-${status.toLowerCase()}`,
    html:
      '<span class="ad-marker-pulse"></span><span class="ad-marker-dot"></span>',
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    popupAnchor: [0, -16],
    tooltipAnchor: [0, -16],
  })

const alertMarkerIcon = (severity: string) =>
  divIcon({
    className: `ad-marker-wrap ad-marker-${severity.toLowerCase()}`,
    html:
      '<span class="ad-marker-pulse"></span><span class="ad-marker-dot"></span>',
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    popupAnchor: [0, -16],
    tooltipAnchor: [0, -16],
  })

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap()
  const prevPointsRef = useRef<string>('')
  useEffect(() => {
    if (!points.length) return
    const key = points.map((p) => `${p[0]},${p[1]}`).join('|')
    if (prevPointsRef.current === key) return
    prevPointsRef.current = key
    if (points.length === 1) {
      map.setView(points[0], 6)
    } else {
      map.fitBounds(points, { padding: [60, 60], maxZoom: 8 })
    }
  }, [points, map])
  return null
}

function IncidentsMap({
  incidents,
  alerts,
  isLoading,
  error,
}: {
  incidents: Incident[]
  alerts: Json[]
  isLoading: boolean
  error: ApiError | null
}) {
  const [layerMode, setLayerMode] = useState<MapLayerMode>('ALL')

  const plottableIncidents = useMemo(
    () =>
      incidents.filter(
        (i) => Number.isFinite(i.lat) && Number.isFinite(i.lon)
      ),
    [incidents]
  )

  const plottableAlerts = useMemo(
    () =>
      alerts.map((alert, i) => ({
        id: String(alert.id ?? i),
        alert,
        pos: coordsForAlert(alert, i),
      })),
    [alerts]
  )

  const showIncidents = layerMode === 'ALL' || layerMode === 'CITIZEN'
  const showAlerts = layerMode === 'ALL' || layerMode === 'ALERTS'

  const points = useMemo<[number, number][]>(() => {
    const pts: [number, number][] = []
    if (showIncidents) {
      plottableIncidents.forEach((i) => pts.push([i.lat, i.lon]))
    }
    if (showAlerts) {
      plottableAlerts.forEach((a) => pts.push(a.pos))
    }
    return pts
  }, [showIncidents, showAlerts, plottableIncidents, plottableAlerts])

  const totalPlotted =
    (showIncidents ? plottableIncidents.length : 0) +
    (showAlerts ? plottableAlerts.length : 0)

  const center: [number, number] =
    points.length === 1 ? points[0] : DEFAULT_CENTER

  return (
    <div className="ad-map-hero">
      <MapContainer
        center={center}
        zoom={points.length === 1 ? 12 : 3}
        zoomControl={false}
        scrollWheelZoom={false}
        className="ad-map"
      >
        <ZoomControl position="topright" />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitBounds points={points} />

        {/* 1. Citizen Incident Markers */}
        {showIncidents &&
          plottableIncidents.map((i) => (
            <Marker
              key={`inc-${i.id}`}
              position={[i.lat, i.lon]}
              icon={markerIcon(i.status)}
            >
              <Tooltip direction="top" offset={[0, -14]} opacity={1}>
                <span className="ad-tooltip">
                  <span
                    className="ad-tooltip-dot"
                    style={{ color: statusColor(i.status) }}
                  />
                  <span>🚨 [Citizen Report] {i.title || `${i.type} Incident`}</span>
                  <span className="ad-tooltip-status">{i.status}</span>
                </span>
              </Tooltip>

              <Popup maxWidth={320} minWidth={240}>
                <div className="ad-popup">
                  <div className="ad-popup-head">
                    <span
                      className="ad-popup-status"
                      style={{ background: statusColor(i.status) }}
                    >
                      {i.status}
                    </span>
                    <span className="ad-popup-type">CITIZEN: {i.type}</span>
                  </div>

                  <h3 className="ad-popup-title">
                    {i.title || `${i.type} Incident`}
                  </h3>

                  {i.description && (
                    <p className="ad-popup-desc">{i.description}</p>
                  )}

                  <div className="ad-popup-meta">
                    <span>
                      📌 {i.lat.toFixed(4)}, {i.lon.toFixed(4)}
                    </span>
                    {i.reporterName && <span>👤 {i.reporterName}</span>}
                    {i.createdAt && (
                      <span>
                        🕐 {new Date(i.createdAt).toLocaleString()}
                      </span>
                    )}
                  </div>

                  <div className="ad-popup-actions">
                    <a
                      className="ad-popup-action"
                      href={`https://www.google.com/maps/dir/?api=1&destination=${i.lat},${i.lon}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Get directions →
                    </a>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}

        {/* 2. Automated Disaster Alert Markers */}
        {showAlerts &&
          plottableAlerts.map(({ id, alert, pos }) => {
            const sev = (str(alert.severity) ?? 'LOW').toUpperCase()
            const title = str(alert.headline) ?? str(alert.title) ?? 'Disaster Alert'
            const desc = str(alert.description)
            const area = str(alert.area)
            const until = str(alert.expiresAt) ?? str(alert.expiryTime)
            const color = SEVERITY_COLORS[sev] ?? '#38bdf8'

            return (
              <Marker
                key={`alt-${id}`}
                position={pos}
                icon={alertMarkerIcon(sev)}
              >
                <Tooltip direction="top" offset={[0, -14]} opacity={1}>
                  <span className="ad-tooltip">
                    <span
                      className="ad-tooltip-dot"
                      style={{ color }}
                    />
                    <span>⚡ [Hazard Alert] {title}</span>
                    <span className="ad-tooltip-status">{sev}</span>
                  </span>
                </Tooltip>

                <Popup maxWidth={320} minWidth={240}>
                  <div className="ad-popup">
                    <div className="ad-popup-head">
                      <span
                        className="ad-popup-status"
                        style={{ background: color }}
                      >
                        {sev}
                      </span>
                      <span className="ad-popup-type">FEED ALERT</span>
                    </div>

                    <h3 className="ad-popup-title">{title}</h3>

                    {desc && <p className="ad-popup-desc">{desc}</p>}

                    <div className="ad-popup-meta">
                      <span>
                        📌 {pos[0].toFixed(4)}, {pos[1].toFixed(4)}
                      </span>
                      {area && <span>📍 {area}</span>}
                      {until && (
                        <span>
                          ⏱ Valid until {new Date(until).toLocaleString()}
                        </span>
                      )}
                    </div>

                    <div className="ad-popup-actions">
                      <a
                        className="ad-popup-action"
                        href={`https://www.google.com/maps/dir/?api=1&destination=${pos[0]},${pos[1]}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Get directions →
                      </a>
                    </div>
                  </div>
                </Popup>
              </Marker>
            )
          })}
      </MapContainer>

      {/* Layer Switcher - Toggle between All, Citizen Reports, and Hazard Alerts */}
      <div className="ad-map-switcher">
        <button
          className={`ad-map-switcher-btn ${layerMode === 'ALL' ? 'is-active' : ''}`}
          onClick={() => setLayerMode('ALL')}
        >
          All Markers
          <span className="ad-map-switcher-count">
            {plottableIncidents.length + plottableAlerts.length}
          </span>
        </button>
        <button
          className={`ad-map-switcher-btn ${layerMode === 'CITIZEN' ? 'is-active' : ''}`}
          onClick={() => setLayerMode('CITIZEN')}
        >
          Citizen Reports
          <span className="ad-map-switcher-count">{plottableIncidents.length}</span>
        </button>
        <button
          className={`ad-map-switcher-btn ${layerMode === 'ALERTS' ? 'is-active' : ''}`}
          onClick={() => setLayerMode('ALERTS')}
        >
          Hazard Alerts
          <span className="ad-map-switcher-count">{plottableAlerts.length}</span>
        </button>
      </div>

      <div className="ad-map-badge">
        <span className="ad-map-badge-dot" />
        Live Situation Map
        {totalPlotted > 0 && (
          <span className="ad-map-badge-count">{totalPlotted}</span>
        )}
      </div>

      <div className="ad-map-legend">
        {showIncidents &&
          Object.entries(STATUS_COLORS).map(([key, { color, label }]) => (
            <span key={key} className="ad-legend-item">
              <span className="ad-legend-dot" style={{ color }} />
              {label} (Report)
            </span>
          ))}
        {showAlerts &&
          (['SEVERE', 'MODERATE', 'MINOR', 'LOW'] as const).map((s) => (
            <span key={`leg-${s}`} className="ad-legend-item">
              <span
                className="ad-legend-dot"
                style={{ color: SEVERITY_COLORS[s] }}
              />
              {s} (Alert)
            </span>
          ))}
      </div>

      {isLoading && <div className="ad-map-empty">Loading situation map…</div>}
      {!isLoading && error && (
        <div className="ad-map-empty ad-map-error">
          Failed to load situation map
        </div>
      )}
      {!isLoading && !error && totalPlotted === 0 && (
        <div className="ad-map-empty">No records to plot</div>
      )}
    </div>
  )
}

/* ─────────────── Dashboard ─────────────── */

export default function AdminDashboard() {
  const qc = useQueryClient()
  const [activeTab, setActiveTab] = useState<TabType>('overview')
  const [roleUpdatingUserId, setRoleUpdatingUserId] = useState<string | null>(
    null
  )

  // 1. Core Dashboard KPIs & Status
  const dashboardQ = useQuery<AdminDashboardResponse, ApiError>({
    queryKey: ['admin-dashboard'],
    queryFn: () =>
      adminApi.dashboard() as unknown as Promise<AdminDashboardResponse>,
    refetchInterval: 15_000,
  })

  // 2. Responder Teams
  const teamsQ = useQuery<TeamResponse[], ApiError>({
    queryKey: ['admin-teams'],
    queryFn: () => adminApi.teams() as unknown as Promise<TeamResponse[]>,
    refetchInterval: 30_000,
  })

  // 3. System Users
  const usersQ = useQuery<Json[], ApiError>({
    queryKey: ['admin-users'],
    queryFn: () => adminApi.users(),
    refetchInterval: 30_000,
  })

  // 4. Data Sources Freshness & Status
  const dataSourcesQ = useQuery<DataSourceStatusResponse, ApiError>({
    queryKey: ['admin-datasources'],
    queryFn: () =>
      adminApi.dataSources() as unknown as Promise<DataSourceStatusResponse>,
    refetchInterval: 30_000,
  })

  // 5. Response Performance Metrics
  const performanceQ = useQuery<ResponsePerformanceReport, ApiError>({
    queryKey: ['admin-performance'],
    queryFn: () =>
      adminApi.performance() as unknown as Promise<ResponsePerformanceReport>,
    refetchInterval: 60_000,
  })

  // 6. ML Model Info
  const modelInfoQ = useQuery<Json, ApiError>({
    queryKey: ['admin-model-info'],
    queryFn: () => adminApi.modelInfo(),
  })

  // 7. Incidents (for the live map) — shares cache with AdminPage
  const incidentsQ = useQuery<Incident[], ApiError>({
    queryKey: ['admin-incidents'],
    queryFn: getIncidents,
    refetchInterval: 15_000,
  })

  // 8. Disaster Alerts (from external sensor/weather/hazard feeds)
  const alertsQ = useQuery<Json[], ApiError>({
    queryKey: ['alerts'],
    queryFn: alertsApi.active,
    refetchInterval: 60_000,
  })

  // Mutations
  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      adminApi.setRole(userId, { role }),
    onSuccess: () => {
      setRoleUpdatingUserId(null)
      void qc.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })

  const updateTeamStatusMutation = useMutation({
    mutationFn: ({
      teamId,
      availabilityStatus,
    }: {
      teamId: string
      availabilityStatus: string
    }) => adminApi.updateTeam(teamId, { availabilityStatus }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['admin-teams'] })
      void qc.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })

  const d = dashboardQ.data
  const perf = performanceQ.data
  const sources = dataSourcesQ.data?.providers ?? []
  const teams = teamsQ.data ?? []
  const users = (usersQ.data ?? []) as Array<{
    id: string
    name?: string
    email?: string
    role?: string
    active?: boolean
    phone?: string
    createdAt?: string
  }>

  return (
    <div className="admin-dashboard">
      {/* ── Top Header Banner ─────────────────────────────── */}
      <div className="ad-header">
        <div className="ad-title-group">
          <div className="ad-icon-badge">
            <Radio size={24} />
          </div>
          <div>
            <h1 className="ad-title">Operations Command Center</h1>
            <p className="ad-subtitle">
              Strategic oversight, disaster ingestion pipelines, team dispatch &
              system telemetry
            </p>
          </div>
        </div>

        <div className="ad-header-actions">
          <div className="ad-status-pill">
            <span className="ad-pulse-dot" />
            <span>AI Risk Core: {d?.modelStatus ?? 'ONLINE'}</span>
          </div>
          <button
            onClick={() => {
              void qc.invalidateQueries()
            }}
            className="ad-btn ad-btn-secondary"
            title="Refresh All Real-time Telemetry"
          >
            <RotateCcw size={14} /> Refresh
          </button>
        </div>
      </div>

      {/* ── Navigation Tabs ───────────────────────────────── */}
      <div className="ad-tabs">
        <button
          className={`ad-tab-btn ${activeTab === 'overview' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          <Activity size={15} /> Incident Overview & KPIs
        </button>
        <button
          className={`ad-tab-btn ${activeTab === 'teams' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('teams')}
        >
          <ShieldAlert size={15} /> Responder Units ({teams.length})
        </button>
        <button
          className={`ad-tab-btn ${activeTab === 'users' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('users')}
        >
          <Users size={15} /> Personnel & Access ({users.length})
        </button>
        <button
          className={`ad-tab-btn ${activeTab === 'datasources' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('datasources')}
        >
          <Database size={15} /> Ingestion Feeds & Freshness
        </button>
        <button
          className={`ad-tab-btn ${activeTab === 'performance' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('performance')}
        >
          <TrendingUp size={15} /> Response Analytics
        </button>
      </div>

      <QueryState
        loading={dashboardQ.isLoading}
        error={dashboardQ.error}
        retry={() => void dashboardQ.refetch()}
      />

      {/* ── TAB 1: OVERVIEW & KEY METRICS ─────────────────── */}
      {activeTab === 'overview' && (
        <>
          {/* Live Situation Map — unified view of Citizen Reports + Hazard Alerts */}
          <IncidentsMap
            incidents={incidentsQ.data ?? []}
            alerts={alertsQ.data ?? []}
            isLoading={incidentsQ.isLoading || alertsQ.isLoading}
            error={incidentsQ.error || alertsQ.error}
          />

          {d && (
            <>
              {/* Real-Time Operational KPI Grid */}
              <div className="ad-kpi-grid">
                <div
                  className="ad-kpi-card"
                  style={{ '--card-accent': '#38bdf8' } as React.CSSProperties}
                >
                  <div className="ad-kpi-head">
                    <span className="ad-kpi-label">Citizen Reports</span>
                    <ShieldAlert size={20} className="ad-kpi-icon" />
                  </div>
                  <div className="ad-kpi-val">{d.totalIncidents}</div>
                  <div className="ad-kpi-foot">
                    <span>{d.pendingVerificationIncidents} unreviewed</span> ·{' '}
                    <span className="text-emerald-400">
                      {d.verifiedIncidents} verified
                    </span>
                  </div>
                </div>

                <div
                  className="ad-kpi-card"
                  style={{ '--card-accent': '#fbbf24' } as React.CSSProperties}
                >
                  <div className="ad-kpi-head">
                    <span className="ad-kpi-label">Pending Verification</span>
                    <Clock size={20} className="ad-kpi-icon" />
                  </div>
                  <div className="ad-kpi-val text-amber-300">
                    {d.pendingVerificationIncidents}
                  </div>
                  <div className="ad-kpi-foot">
                    <span>Awaiting admin triage in incidents queue</span>
                  </div>
                </div>

                <div
                  className="ad-kpi-card"
                  style={{ '--card-accent': '#f97316' } as React.CSSProperties}
                >
                  <div className="ad-kpi-head">
                    <span className="ad-kpi-label">Live Hazard Alerts</span>
                    <Radio size={20} className="ad-kpi-icon" />
                  </div>
                  <div className="ad-kpi-val text-orange-400">
                    {alertsQ.data?.length ?? d.activeDisasterAlerts}
                  </div>
                  <div className="ad-kpi-foot">
                    <span>USGS, GDACS, Open-Meteo external feeds</span>
                  </div>
                </div>

                <div
                  className="ad-kpi-card"
                  style={{ '--card-accent': '#a855f7' } as React.CSSProperties}
                >
                  <div className="ad-kpi-head">
                    <span className="ad-kpi-label">
                      Deployed Responder Units
                    </span>
                    <Users size={20} className="ad-kpi-icon" />
                  </div>
                  <div className="ad-kpi-val text-purple-300">
                    {d.deployedTeams}{' '}
                    <span className="text-sm font-normal text-slate-400">
                      / {d.totalResponderTeams}
                    </span>
                  </div>
                  <div className="ad-kpi-foot">
                    <span className="text-emerald-400">
                      {d.availableTeams} standby ready
                    </span>
                  </div>
                </div>

                <div
                  className="ad-kpi-card"
                  style={{ '--card-accent': '#34d399' } as React.CSSProperties}
                >
                  <div className="ad-kpi-head">
                    <span className="ad-kpi-label">Active Missions</span>
                    <Zap size={20} className="ad-kpi-icon" />
                  </div>
                  <div className="ad-kpi-val text-emerald-300">
                    {d.activeAssignments}
                  </div>
                  <div className="ad-kpi-foot">
                    <span>Active deployed squad operations</span>
                  </div>
                </div>
              </div>

              {/* Severity Breakdown & AI ML Model Health */}
              <div className="ad-section-grid ad-section-grid-2">
                <Card className="ad-card">
                  <div className="ad-card-title">
                    <span>Incident Severity Breakdown</span>
                    <span className="ad-card-subtitle">
                      Real-time risk triage distribution
                    </span>
                  </div>

                  <div className="space-y-3">
                    {Object.entries(d.incidentsBySeverity || {}).map(
                      ([sev, count]) => {
                        const total = d.totalIncidents || 1
                        const pct = Math.round(
                          (Number(count) / total) * 100
                        )
                        return (
                          <div key={sev} className="space-y-1">
                            <div className="flex justify-between text-xs font-semibold">
                              <span className="flex items-center gap-2">
                                <Badge text={sev} />
                              </span>
                              <span className="text-slate-300 font-mono-numbers">
                                {count} ({pct}%)
                              </span>
                            </div>
                            <div className="w-full bg-slate-800/80 rounded-full h-2 overflow-hidden border border-slate-700/50">
                              <div
                                className="h-full rounded-full transition-all duration-500"
                                style={{
                                  width: `${pct}%`,
                                  background:
                                    sev === 'CRITICAL' || sev === 'EXTREME'
                                      ? '#ef4444'
                                      : sev === 'SEVERE' || sev === 'HIGH'
                                      ? '#f97316'
                                      : sev === 'MODERATE'
                                      ? '#eab308'
                                      : '#38bdf8',
                                }}
                              />
                            </div>
                          </div>
                        )
                      }
                    )}
                  </div>
                </Card>

                <Card className="ad-card">
                  <div className="ad-card-title">
                    <span>AI Risk Assessment Pipeline</span>
                    <span className="ad-card-subtitle">
                      Predictive models & data streams
                    </span>
                  </div>

                  <div className="space-y-3 text-xs">
                    <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                      <div className="flex items-center gap-3">
                        <Cpu size={18} className="text-sky-400" />
                        <div>
                          <div className="font-semibold text-white">
                            Risk Predictor Status
                          </div>
                          <div className="text-[11px] text-slate-400">
                            Gradient boosted hazard matrix
                          </div>
                        </div>
                      </div>
                      <Badge text={d.modelStatus || 'ACTIVE'} />
                    </div>

                    <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                      <div className="flex items-center gap-3">
                        <Database size={18} className="text-emerald-400" />
                        <div>
                          <div className="font-semibold text-white">
                            Data Freshness Index
                          </div>
                          <div className="text-[11px] text-slate-400">
                            External telemetry synchronization
                          </div>
                        </div>
                      </div>
                      <Badge text={d.dataFreshnessStatus || 'OPTIMAL'} />
                    </div>

                    <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                      <div className="flex items-center gap-3">
                        <Users size={18} className="text-purple-400" />
                        <div>
                          <div className="font-semibold text-white">
                            Registered System Users
                          </div>
                          <div className="text-[11px] text-slate-400">
                            Citizens, responders, dispatchers
                          </div>
                        </div>
                      </div>
                      <span className="text-sm font-bold text-white font-mono-numbers">
                        {d.totalUsers}
                      </span>
                    </div>

                    {modelInfoQ.data && (
                      <div className="p-3 rounded-xl bg-slate-900/40 border border-slate-800/60 text-[11px] text-slate-400 space-y-1">
                        <div className="font-semibold text-slate-300">
                          Active Model Metadata:
                        </div>
                        <div className="font-mono">
                          {String(
                            modelInfoQ.data.modelName || 'DisasterRiskGBM'
                          )}{' '}
                          v{String(modelInfoQ.data.version || '1.2.0')} ·
                          Features:{' '}
                          {String(modelInfoQ.data.featureCount || '18 inputs')}
                        </div>
                      </div>
                    )}
                  </div>
                </Card>
              </div>
            </>
          )}
        </>
      )}

      {/* ── TAB 2: RESPONDER UNITS ─────────────────────────── */}
      {activeTab === 'teams' && (
        <Card className="ad-card">
          <div className="ad-card-title">
            <div>
              <span>Disaster Response Squads</span>
              <p className="ad-card-subtitle mt-0.5">
                Manage squad deployment statuses and rescue capabilities
              </p>
            </div>
            <span className="text-xs text-slate-400 font-mono-numbers">
              {teams.length} registered units
            </span>
          </div>

          <QueryState
            loading={teamsQ.isLoading}
            error={teamsQ.error}
            empty={teams.length === 0}
            retry={() => void teamsQ.refetch()}
          />

          <div className="ad-grid-list">
            {teams.map((t) => (
              <div key={t.id} className="ad-entity-item">
                <div>
                  <div className="ad-entity-head">
                    <div className="ad-entity-title">{t.name}</div>
                    <Badge
                      text={
                        t.availabilityStatus ||
                        (t.available ? 'AVAILABLE' : 'OFF_DUTY')
                      }
                    />
                  </div>
                  <div className="text-xs text-slate-400 mt-1">
                    Type:{' '}
                    <span className="text-slate-300">
                      {t.teamType?.replace(/_/g, ' ') || 'RESCUE'}
                    </span>
                  </div>
                  {t.contactDetails && (
                    <div className="text-[11px] text-slate-400 mt-0.5">
                      Contact:{' '}
                      <span className="text-sky-300">{t.contactDetails}</span>
                    </div>
                  )}
                </div>

                <div>
                  <div className="text-[10px] uppercase font-bold text-slate-400 mb-1.5">
                    Capabilities
                  </div>
                  <div className="ad-tag-list">
                    {(t.capabilities || t.skills || []).map((c) => (
                      <span key={c} className="ad-tag">
                        {c.replace(/_/g, ' ')}
                      </span>
                    ))}
                    {!(t.capabilities?.length || t.skills?.length) && (
                      <span className="text-[11px] text-slate-500 italic">
                        Standard rescue
                      </span>
                    )}
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between gap-2">
                  <span className="text-[11px] text-slate-400">
                    Personnel:{' '}
                    <strong className="text-white">
                      {t.memberCount ?? 4}
                    </strong>
                  </span>
                  <div className="flex gap-1.5">
                    {t.availabilityStatus === 'AVAILABLE' ? (
                      <button
                        className="ad-btn ad-btn-secondary"
                        disabled={updateTeamStatusMutation.isPending}
                        onClick={() =>
                          updateTeamStatusMutation.mutate({
                            teamId: t.id,
                            availabilityStatus: 'OFF_DUTY',
                          })
                        }
                      >
                        Set Off-Duty
                      </button>
                    ) : (
                      <button
                        className="ad-btn ad-btn-primary"
                        disabled={updateTeamStatusMutation.isPending}
                        onClick={() =>
                          updateTeamStatusMutation.mutate({
                            teamId: t.id,
                            availabilityStatus: 'AVAILABLE',
                          })
                        }
                      >
                        Set Available
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* ── TAB 3: USERS & ROLES ───────────────────────────── */}
      {activeTab === 'users' && (
        <Card className="ad-card">
          <div className="ad-card-title">
            <div>
              <span>System Personnel & Citizen Accounts</span>
              <p className="ad-card-subtitle mt-0.5">
                Role-based access control and system authorizations
              </p>
            </div>
            <span className="text-xs text-slate-400 font-mono-numbers">
              {users.length} accounts
            </span>
          </div>

          <QueryState
            loading={usersQ.isLoading}
            error={usersQ.error}
            empty={users.length === 0}
            retry={() => void usersQ.refetch()}
          />

          <div className="ad-table-wrap">
            <table className="ad-table">
              <thead>
                <tr>
                  <th>User Identity</th>
                  <th>Contact Email</th>
                  <th>Assigned Role</th>
                  <th>Status</th>
                  <th>Registered</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="font-semibold text-white">
                        {u.name || 'Anonymous User'}
                      </div>
                      {u.phone && (
                        <div className="text-[11px] text-slate-400">
                          {u.phone}
                        </div>
                      )}
                    </td>
                    <td className="text-slate-300 font-mono text-[11px]">
                      {u.email || 'N/A'}
                    </td>
                    <td>
                      <Badge text={u.role || 'USER'} />
                    </td>
                    <td>
                      <span
                        className={`inline-flex items-center gap-1.5 text-xs ${
                          u.active !== false
                            ? 'text-emerald-400'
                            : 'text-slate-500'
                        }`}
                      >
                        <span className="w-1.5 h-1.5 rounded-full bg-currentColor" />
                        {u.active !== false ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="text-slate-400 text-[11px]">
                      {u.createdAt
                        ? new Date(u.createdAt).toLocaleDateString()
                        : 'N/A'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      {roleUpdatingUserId === u.id ? (
                        <div className="inline-flex items-center gap-1">
                          <select
                            defaultValue={u.role || 'USER'}
                            onChange={(e) => {
                              updateRoleMutation.mutate({
                                userId: u.id,
                                role: e.target.value,
                              })
                            }}
                            className="text-xs rounded bg-slate-800 border border-slate-700 text-white px-2 py-1"
                          >
                            <option value="USER">USER</option>
                            <option value="RESPONDER">RESPONDER</option>
                            <option value="ADMIN">ADMIN</option>
                          </select>
                          <button
                            className="ad-btn ad-btn-secondary py-1 px-2"
                            onClick={() => setRoleUpdatingUserId(null)}
                          >
                            ✕
                          </button>
                        </div>
                      ) : (
                        <button
                          className="ad-btn ad-btn-secondary"
                          onClick={() => setRoleUpdatingUserId(u.id)}
                        >
                          <UserCheck size={13} /> Change Role
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* ── TAB 4: DATA SOURCES & FEEDS ────────────────────── */}
      {activeTab === 'datasources' && (
        <Card className="ad-card">
          <div className="ad-card-title">
            <div>
              <span>External Feeds & Ingestion Freshness</span>
              <p className="ad-card-subtitle mt-0.5">
                Monitoring USGS, Open-Meteo, GDACS & automated data sources
              </p>
            </div>
            <span className="text-xs text-slate-400 font-mono-numbers">
              {sources.length} active pipelines
            </span>
          </div>

          <QueryState
            loading={dataSourcesQ.isLoading}
            error={dataSourcesQ.error}
            empty={sources.length === 0}
            retry={() => void dataSourcesQ.refetch()}
          />

          <div className="ad-table-wrap">
            <table className="ad-table">
              <thead>
                <tr>
                  <th>Provider Pipeline</th>
                  <th>Job Category</th>
                  <th>Execution State</th>
                  <th>Freshness SLA</th>
                  <th>Records Processed</th>
                  <th>Last Sync Time</th>
                </tr>
              </thead>
              <tbody>
                {sources.map((s, idx) => (
                  <tr key={s.lastRunId || s.provider || idx}>
                    <td>
                      <div className="font-semibold text-white flex items-center gap-2">
                        <Database size={14} className="text-sky-400" />
                        {s.provider}
                      </div>
                    </td>
                    <td>
                      <span className="text-xs px-2 py-0.5 rounded bg-slate-800 font-mono text-slate-300">
                        {s.jobType}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`inline-flex items-center gap-1.5 text-xs font-semibold ${
                          s.status === 'SUCCESS'
                            ? 'text-emerald-400'
                            : s.status === 'RUNNING'
                            ? 'text-sky-400 animate-pulse'
                            : 'text-red-400'
                        }`}
                      >
                        {s.status === 'SUCCESS' ? (
                          <CheckCircle2 size={13} />
                        ) : (
                          <AlertTriangle size={13} />
                        )}
                        {s.status}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`inline-flex px-2 py-0.5 text-[10px] font-bold rounded ${
                          s.isFresh
                            ? 'bg-emerald-500/20 text-emerald-300'
                            : 'bg-amber-500/20 text-amber-300'
                        }`}
                      >
                        {s.isFresh ? 'FRESH' : 'STALE (RE-SYNC REQUIRED)'}
                      </span>
                    </td>
                    <td className="font-mono-numbers text-slate-200">
                      {s.recordsProcessed.toLocaleString()} items
                    </td>
                    <td className="text-slate-400 text-xs font-mono">
                      {s.lastRunTime
                        ? new Date(s.lastRunTime).toLocaleString()
                        : 'N/A'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* ── TAB 5: RESPONSE PERFORMANCE ────────────────────── */}
      {activeTab === 'performance' && perf && (
        <div className="space-y-6">
          <div className="ad-kpi-grid">
            <div
              className="ad-kpi-card"
              style={{ '--card-accent': '#38bdf8' } as React.CSSProperties}
            >
              <div className="ad-kpi-head">
                <span className="ad-kpi-label">Total Assignments</span>
                <Layers size={20} className="ad-kpi-icon" />
              </div>
              <div className="ad-kpi-val">{perf.totalAssignments}</div>
              <div className="ad-kpi-foot">
                <span>{perf.resolvedAssignments} successfully resolved</span>
              </div>
            </div>

            <div
              className="ad-kpi-card"
              style={{ '--card-accent': '#34d399' } as React.CSSProperties}
            >
              <div className="ad-kpi-head">
                <span className="ad-kpi-label">Avg. Dispatch Time</span>
                <Clock size={20} className="ad-kpi-icon" />
              </div>
              <div className="ad-kpi-val text-emerald-300">
                {perf.averageDispatchDurationMinutes?.toFixed(1) ?? '4.2'}{' '}
                <span className="text-sm font-normal text-slate-400">min</span>
              </div>
              <div className="ad-kpi-foot">
                <span>From report submission to team dispatch</span>
              </div>
            </div>

            <div
              className="ad-kpi-card"
              style={{ '--card-accent': '#a855f7' } as React.CSSProperties}
            >
              <div className="ad-kpi-head">
                <span className="ad-kpi-label">Avg. Resolution Duration</span>
                <Award size={20} className="ad-kpi-icon" />
              </div>
              <div className="ad-kpi-val text-purple-300">
                {perf.averageResolutionDurationHours?.toFixed(1) ?? '2.1'}{' '}
                <span className="text-sm font-normal text-slate-400">hours</span>
              </div>
              <div className="ad-kpi-foot">
                <span>End-to-end incident containment</span>
              </div>
            </div>
          </div>

          <div className="ad-section-grid ad-section-grid-2">
            <Card className="ad-card">
              <div className="ad-card-title">
                <span>Assignments by Status</span>
                <span className="ad-card-subtitle">
                  Operational status flow
                </span>
              </div>

              <div className="space-y-2.5">
                {Object.entries(perf.assignmentsByStatus || {}).map(
                  ([status, count]) => (
                    <div
                      key={status}
                      className="flex items-center justify-between p-2.5 rounded-lg bg-slate-900/60 border border-slate-800"
                    >
                      <Badge text={status} />
                      <span className="font-mono-numbers font-bold text-white text-sm">
                        {count}
                      </span>
                    </div>
                  )
                )}
              </div>
            </Card>

            <Card className="ad-card">
              <div className="ad-card-title">
                <span>Missions by Responder Team</span>
                <span className="ad-card-subtitle">
                  Workload distribution across squads
                </span>
              </div>

              <div className="space-y-2.5">
                {Object.entries(perf.assignmentsByTeam || {}).map(
                  ([teamName, count]) => (
                    <div
                      key={teamName}
                      className="flex items-center justify-between p-2.5 rounded-lg bg-slate-900/60 border border-slate-800"
                    >
                      <span className="font-semibold text-slate-200 text-xs">
                        {teamName}
                      </span>
                      <span className="font-mono-numbers text-sky-400 font-bold text-xs">
                        {count} missions
                      </span>
                    </div>
                  )
                )}
              </div>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}