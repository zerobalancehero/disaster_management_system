import { type ReactNode, useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
    Phone,
    Droplets,
    Wind as WindIcon,
    Gauge,
    Eye,
    Cloud,
    Sun,
    Sunrise,
    Sunset,
    MapPin,
    Clock,
    Thermometer,
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
import { alertsApi, weatherApi, type Json } from '../../api/endpoints'
import { Badge, Card, QueryState } from '../../components/ui'
import { KV } from '../../components/KV'
import type { ApiError } from '../../types'
import '../../styles/UserOverview.css'

const LAT = 13.0827
const LON = 80.2707
const SOS: [string, string][] = [
    ['112', 'National emergency'],
    ['108', 'Ambulance'],
    ['101', 'Fire & rescue'],
]

/* ---------- Json helpers ---------- */
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

const coordsFor = (alert: Json, index: number): [number, number] => {
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
    return [LAT + offsetLat, LON + offsetLon]
}

/* ---------- Weather extraction ---------- */
type WeatherInfo = {
    temp: number | null
    feels: number | null
    humidity: number | null
    pressure: number | null
    windSpeed: number | null
    windDeg: number | null
    visibility: number | null
    clouds: number | null
    uv: number | null
    sunrise: Date | null
    sunset: Date | null
    dt: Date | null
    condition: string | null
    description: string | null
    name: string | null
}

const toDate = (v: unknown): Date | null => {
    if (v == null) return null
    if (typeof v === 'number') {
        if (v > 1e12) return new Date(v)
        if (v > 1e9) return new Date(v * 1000)
        return null
    }
    if (typeof v === 'string') {
        const d = new Date(v)
        return isNaN(d.getTime()) ? null : d
    }
    return null
}

function extractWeather(data: Json): WeatherInfo {
    const get = (...paths: string[]): unknown => {
        for (const path of paths) {
            const parts = path.split('.')
            let cur: any = data
            let ok = true
            for (const p of parts) {
                if (cur == null) {
                    ok = false
                    break
                }
                cur = cur[p]
            }
            if (ok && cur != null) return cur
        }
        return undefined
    }

    const toC = (v: number | null): number | null => {
        if (v == null) return null
        if (v > 150) return v - 273.15
        return v
    }

    return {
        temp: toC(num(get('main.temp', 'temp', 'temperature'))),
        feels: toC(num(get('main.feels_like', 'feels_like', 'feelsLike'))),
        humidity: num(get('main.humidity', 'humidity')),
        pressure: num(get('main.pressure', 'pressure')),
        windSpeed: num(get('wind.speed', 'wind_speed', 'windSpeed')),
        windDeg: num(get('wind.deg', 'wind_deg', 'windDirection')),
        visibility: num(get('visibility')),
        clouds: num(get('clouds.all', 'clouds', 'cloudiness')),
        uv: num(get('uvi', 'uv_index', 'uvIndex')),
        sunrise: toDate(get('sys.sunrise', 'sunrise')),
        sunset: toDate(get('sys.sunset', 'sunset')),
        dt: toDate(get('dt', 'timestamp', 'lastUpdated')),
        condition: str(get('weather.0.main', 'condition', 'weather')),
        description: str(get('weather.0.description', 'description')),
        name: str(get('name', 'city', 'location')),
    }
}

/* ---------- Weather theme ---------- */
type WeatherTheme = { key: string; emoji: string }

function weatherTheme(cond: string | null): WeatherTheme {
    const c = (cond ?? '').toLowerCase()
    if (c.includes('thunder')) return { key: 'storm', emoji: '⛈️' }
    if (c.includes('drizzle') || c.includes('rain')) return { key: 'rain', emoji: '🌧️' }
    if (c.includes('snow')) return { key: 'snow', emoji: '❄️' }
    if (c.includes('cloud')) return { key: 'clouds', emoji: '☁️' }
    if (c.includes('clear') || c.includes('sun')) return { key: 'clear', emoji: '☀️' }
    if (c.includes('mist') || c.includes('fog') || c.includes('haze') || c.includes('smoke'))
        return { key: 'mist', emoji: '🌫️' }
    if (c.includes('dust') || c.includes('sand')) return { key: 'dust', emoji: '🌪️' }
    return { key: 'default', emoji: '🌤️' }
}

const compass = (deg: number | null): string => {
    if (deg == null) return ''
    const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']
    return dirs[Math.round((deg % 360) / 45) % 8]
}

const uvLabel = (uv: number): string => {
    if (uv < 3) return 'Low'
    if (uv < 6) return 'Moderate'
    if (uv < 8) return 'High'
    if (uv < 11) return 'Very High'
    return 'Extreme'
}

const fmtTime = (d: Date | null): string =>
    d ? d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'

/* ---------- Weather subcomponents ---------- */
function WxStat({
    icon,
    label,
    value,
}: {
    icon: ReactNode
    label: string
    value: string
}) {
    return (
        <div className="uo-wx-stat">
            <div className="uo-wx-stat-icon">{icon}</div>
            <div className="uo-wx-stat-body">
                <div className="uo-wx-stat-label">{label}</div>
                <div className="uo-wx-stat-value">{value}</div>
            </div>
        </div>
    )
}

function WeatherPanel({ data }: { data: Json }) {
    const w = extractWeather(data)

    // If nothing meaningful was extracted, fall back to the original KV view.
    if (w.temp == null && w.humidity == null && w.windSpeed == null) {
        return <KV data={data} />
    }

    const theme = weatherTheme(w.condition)
    const sunProgress =
        w.sunrise && w.sunset && w.dt
            ? Math.min(
                1,
                Math.max(
                    0,
                    (w.dt.getTime() - w.sunrise.getTime()) /
                    (w.sunset.getTime() - w.sunrise.getTime())
                )
            )
            : null

    return (
        <div className={`uo-wx-content uo-wx-${theme.key}`}>
            {/* Hero row */}
            <div className="uo-wx-hero">
                <div className="uo-wx-icon-wrap" aria-hidden>
                    {theme.emoji}
                </div>

                <div className="uo-wx-hero-body">
                    <div className="uo-wx-temp-block">
                        <span className="uo-wx-temp-value">
                            {w.temp != null ? Math.round(w.temp) : '—'}
                        </span>
                        <span className="uo-wx-temp-unit">°C</span>
                    </div>

                    <div className="uo-wx-condition">
                        {w.condition ?? 'Weather'}
                    </div>
                    {w.description && (
                        <div className="uo-wx-desc">{w.description}</div>
                    )}
                    {w.name && (
                        <div className="uo-wx-loc">
                            <MapPin size={11} aria-hidden /> {w.name}
                        </div>
                    )}
                </div>
            </div>

            {/* Stats grid */}
            <div className="uo-wx-stats">
                {w.feels != null && (
                    <WxStat
                        icon={<Thermometer size={15} aria-hidden />}
                        label="Feels like"
                        value={`${Math.round(w.feels)}°C`}
                    />
                )}
                {w.humidity != null && (
                    <WxStat
                        icon={<Droplets size={15} aria-hidden />}
                        label="Humidity"
                        value={`${Math.round(w.humidity)}%`}
                    />
                )}
                {w.windSpeed != null && (
                    <WxStat
                        icon={<WindIcon size={15} aria-hidden />}
                        label="Wind"
                        value={`${w.windSpeed.toFixed(1)} m/s${w.windDeg != null ? ` ${compass(w.windDeg)}` : ''
                            }`}
                    />
                )}
                {w.pressure != null && (
                    <WxStat
                        icon={<Gauge size={15} aria-hidden />}
                        label="Pressure"
                        value={`${Math.round(w.pressure)} hPa`}
                    />
                )}
                {w.visibility != null && (
                    <WxStat
                        icon={<Eye size={15} aria-hidden />}
                        label="Visibility"
                        value={`${(w.visibility / 1000).toFixed(1)} km`}
                    />
                )}
                {w.clouds != null && (
                    <WxStat
                        icon={<Cloud size={15} aria-hidden />}
                        label="Cloud cover"
                        value={`${Math.round(w.clouds)}%`}
                    />
                )}
                {w.uv != null && (
                    <WxStat
                        icon={<Sun size={15} aria-hidden />}
                        label={`UV · ${uvLabel(w.uv)}`}
                        value={w.uv.toFixed(1)}
                    />
                )}
            </div>

            {/* Sun cycle bar */}
            {(w.sunrise || w.sunset) && (
                <div className="uo-wx-sunbar">
                    <div className="uo-wx-sun-side">
                        <Sunrise size={13} aria-hidden />
                        <span>{fmtTime(w.sunrise)}</span>
                    </div>
                    <div className="uo-wx-sun-track">
                        <div
                            className="uo-wx-sun-fill"
                            style={{ width: `${(sunProgress ?? 0.5) * 100}%` }}
                        />
                        {sunProgress != null && (
                            <div
                                className="uo-wx-sun-dot"
                                style={{ left: `${sunProgress * 100}%` }}
                            />
                        )}
                    </div>
                    <div className="uo-wx-sun-side">
                        <Sunset size={13} aria-hidden />
                        <span>{fmtTime(w.sunset)}</span>
                    </div>
                </div>
            )}

            {w.dt && (
                <div className="uo-wx-footer">
                    <Clock size={11} aria-hidden /> Updated {w.dt.toLocaleTimeString()}
                </div>
            )}
        </div>
    )
}

/* ---------- Map markers ---------- */
const markerIcon = (severity: string) =>
    divIcon({
        className: `uo-marker-wrap uo-marker-${severity.toLowerCase()}`,
        html: '<span class="uo-marker-pulse"></span><span class="uo-marker-dot"></span>',
        iconSize: [26, 26],
        iconAnchor: [13, 13],
        popupAnchor: [0, -16],
        tooltipAnchor: [0, -16],
    })

function FitBounds({ points }: { points: [number, number][] }) {
    const map = useMap()
    const hasFit = useRef(false)
    useEffect(() => {
        if (!points.length || hasFit.current) return
        hasFit.current = true
        if (points.length === 1) {
            map.setView(points[0], 6)
        } else {
            map.fitBounds(points, { padding: [60, 60], maxZoom: 8 })
        }
    }, [points, map])
    return null
}

function FlyTo({ point }: { point: [number, number] | null }) {
    const map = useMap()
    useEffect(() => {
        if (point) map.flyTo(point, 6, { duration: 0.9 })
    }, [point, map])
    return null
}

function AlertTooltipContent({ alert }: { alert: Json }) {
    const sev = (str(alert.severity) ?? 'LOW').toUpperCase()
    const title = str(alert.headline) ?? str(alert.title) ?? 'Alert'
    return (
        <span className="uo-tooltip">
            <span
                className="uo-tooltip-dot"
                style={{ color: `var(--sev-${sev.toLowerCase()})` }}
            />
            <span>{title}</span>
            <span className="uo-tooltip-sev">{sev}</span>
        </span>
    )
}

function AlertPopupContent({
    alert,
    pos,
}: {
    alert: Json
    pos: [number, number]
}) {
    const sev = (str(alert.severity) ?? 'LOW').toUpperCase()
    const title = str(alert.headline) ?? str(alert.title) ?? 'Alert'
    const desc = str(alert.description)
    const area = str(alert.area)
    const until = str(alert.expiresAt) ?? str(alert.expiryTime)
    const untilLabel = until ? new Date(until).toLocaleString() : null

    return (
        <div className={`uo-popup uo-sev-${sev.toLowerCase()}`}>
            <div className="uo-popup-head">
                <span className="uo-popup-sev">{sev}</span>
                {area && <span className="uo-popup-area">📍 {area}</span>}
            </div>
            <h3 className="uo-popup-title">{title}</h3>
            {desc && <p className="uo-popup-desc">{desc}</p>}
            <div className="uo-popup-meta">
                <span>
                    📌 {pos[0].toFixed(4)}, {pos[1].toFixed(4)}
                </span>
                {untilLabel && <span>⏱ Valid until {untilLabel}</span>}
            </div>
            <div className="uo-popup-actions">
                <a
                    className="uo-popup-action"
                    href={`https://www.google.com/maps/dir/?api=1&destination=${pos[0]},${pos[1]}`}
                    target="_blank"
                    rel="noreferrer"
                >
                    Get directions →
                </a>
            </div>
        </div>
    )
}

/* ---------- Page ---------- */
export default function UserOverview() {
    const alerts = useQuery<Json[], ApiError>({
        queryKey: ['alerts'],
        queryFn: alertsApi.active,
        refetchInterval: 60_000,
    })
    const wx = useQuery<Json, ApiError>({
        queryKey: ['wx', LAT, LON],
        queryFn: () => weatherApi.current(LAT, LON),
        refetchInterval: 300_000,
    })

    const top = alerts.data?.find((a) => str(a.severity) === 'SEVERE')

    const plotted = useMemo(
        () =>
            (alerts.data ?? []).map((alert, i) => ({
                id: String(alert.id ?? i),
                alert,
                pos: coordsFor(alert, i),
            })),
        [alerts.data]
    )
    const points = useMemo(() => plotted.map((p) => p.pos), [plotted])

    const [focusPoint, setFocusPoint] = useState<[number, number] | null>(null)
    const [focusId, setFocusId] = useState<string | null>(null)

    const center: [number, number] =
        points.length === 1 ? points[0] : [LAT, LON]

    return (
        <div className="user-overview">
            {/* ── Live incident map ───────────────────── */}
            <div className="uo-map-hero">
                <MapContainer
                    center={center}
                    zoom={points.length === 1 ? 6 : 2}
                    zoomControl={false}
                    scrollWheelZoom={false}
                    className="uo-map"
                >
                    <ZoomControl position="topright" />
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <FitBounds points={points} />
                    <FlyTo point={focusPoint} />
                    {plotted.map(({ id, alert, pos }) => {
                        const sev = (str(alert.severity) ?? 'LOW').toLowerCase()
                        return (
                            <Marker
                                key={id}
                                position={pos}
                                icon={markerIcon(sev)}
                                eventHandlers={{ popupopen: () => setFocusId(id) }}
                            >
                                <Tooltip direction="top" offset={[0, -14]} opacity={1}>
                                    <AlertTooltipContent alert={alert} />
                                </Tooltip>
                                <Popup maxWidth={320} minWidth={240}>
                                    <AlertPopupContent alert={alert} pos={pos} />
                                </Popup>
                            </Marker>
                        )
                    })}
                </MapContainer>

                <div className="uo-map-badge">
                    <span className="uo-map-badge-dot" />
                    Live Incident Map
                    {plotted.length > 0 && (
                        <span className="uo-map-badge-count">{plotted.length}</span>
                    )}
                </div>

                <div className="uo-map-legend">
                    {(['SEVERE', 'MODERATE', 'MINOR', 'LOW'] as const).map((s) => (
                        <span key={s} className="uo-legend-item">
                            <span
                                className="uo-legend-dot"
                                style={{ color: `var(--sev-${s.toLowerCase()})` }}
                            />
                            {s}
                        </span>
                    ))}
                </div>

                {plotted.length === 0 && !alerts.isLoading && (
                    <div className="uo-map-empty">No active alerts to plot</div>
                )}
            </div>

            {/* ── Severe banner ──────────────────────── */}
            {top && (
                <div role="alert" className="uo-severe-banner">
                    <div className="uo-severe-head">
                        <strong>
                            {str(top.headline) ?? str(top.title) ?? 'Severe alert in effect'}
                        </strong>
                        <Badge text="SEVERE" />
                    </div>
                    {str(top.description) && (
                        <p className="uo-severe-desc">{str(top.description)}</p>
                    )}
                </div>
            )}

            {/* ── Active alerts list ─────────────────── */}
            <Card className="uo-card">
                <h2 className="uo-h2">Active alerts</h2>
                <QueryState
                    loading={alerts.isLoading}
                    error={alerts.error}
                    empty={alerts.data?.length === 0}
                    retry={() => void alerts.refetch()}
                />
                <ul className="uo-alert-list">
                    {plotted.map(({ id, alert: a, pos }) => {
                        const sev = (str(a.severity) ?? 'LOW').toLowerCase()
                        const title = str(a.headline) ?? str(a.title) ?? 'Alert'
                        const desc = str(a.description)
                        const area = str(a.area)
                        const until = str(a.expiresAt) ?? str(a.expiryTime)
                        return (
                            <li
                                key={id}
                                className={`uo-alert-row uo-sev-${sev}${focusId === id ? ' is-focused' : ''
                                    }`}
                                onClick={() => {
                                    setFocusId(id)
                                    setFocusPoint(pos)
                                }}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' || e.key === ' ') {
                                        e.preventDefault()
                                        setFocusId(id)
                                        setFocusPoint(pos)
                                    }
                                }}
                                role="button"
                                tabIndex={0}
                                aria-label={`Focus map on ${title}`}
                            >
                                <span className="uo-alert-strip" />
                                <div className="uo-alert-body">
                                    <div className="uo-alert-head">
                                        <span className="uo-alert-title">{title}</span>
                                        <Badge text={String(a.severity ?? 'LOW')} />
                                    </div>
                                    {desc && <p className="uo-alert-desc">{desc}</p>}
                                    <div className="uo-alert-meta">
                                        <span>
                                            📌 {pos[0].toFixed(4)}, {pos[1].toFixed(4)}
                                        </span>
                                        {area && <span>📍 {area}</span>}
                                        {until && (
                                            <span>
                                                Valid until {new Date(until).toLocaleString()}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </li>
                        )
                    })}
                </ul>
            </Card>

            {/* ── Weather (premium design) ───────────── */}
            <Card className="uo-card uo-weather-card">
                <h2 className="uo-h2">Current weather</h2>
                <QueryState
                    loading={wx.isLoading}
                    error={wx.error}
                    retry={() => void wx.refetch()}
                />
                {wx.data && <WeatherPanel data={wx.data} />}
            </Card>

            {/* ── SOS nav ────────────────────────────── */}
            <nav aria-label="Emergency numbers" className="uo-sos-nav">
                {SOS.map(([n, l]) => (
                    <a key={n} href={`tel:${n}`} className="uo-sos-pill">
                        <Phone size={16} aria-hidden />
                        <span className="uo-sos-num">{n}</span>
                        <span className="uo-sos-label">{l}</span>
                    </a>
                ))}
            </nav>
        </div>
    )
}