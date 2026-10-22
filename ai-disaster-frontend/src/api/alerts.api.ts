import { api } from './client'
import type { Alert, Incident, IncidentReportRequest } from '../types'

interface RawAlert {
  id: string
  title?: string
  headline?: string
  severity: Alert['severity']
  area?: string
  description?: string
  disasterType?: string
  issuedAt?: string
  issueTime?: string
  expiresAt?: string
  expiryTime?: string
}

function mapAlert(raw: RawAlert): Alert {
  return {
    id: raw.id,
    title: raw.headline ?? raw.title ?? `${raw.disasterType ?? 'Disaster'} Alert`,
    severity: raw.severity,
    area: raw.area ?? 'Chennai Metropolitan Region',
    description: raw.description,
    issuedAt: raw.issueTime ?? raw.issuedAt ?? new Date().toISOString(),
    expiresAt: raw.expiryTime ?? raw.expiresAt ?? new Date(Date.now() + 86400000).toISOString(),
  }
}

interface RawIncident {
  id: string
  title?: string
  description: string
  type?: string
  disasterType?: string
  lat?: number
  latitude?: number
  lon?: number
  longitude?: number
  status?: string
  verificationStatus?: string
  createdAt?: string
  reportedTime?: string
  rejectionReason?: string
}

function mapIncident(raw: RawIncident): Incident {
  return {
    id: raw.id,
    title: raw.title,
    description: raw.description,
    type: raw.disasterType ?? raw.type ?? 'OTHER',
    lat: raw.latitude ?? raw.lat ?? 0,
    lon: raw.longitude ?? raw.lon ?? 0,
    status: (raw.verificationStatus ?? raw.status ?? 'PENDING') as Incident['status'],
    createdAt: raw.reportedTime ?? raw.createdAt ?? new Date().toISOString(),
    rejectionReason: raw.rejectionReason,
  }
}

export const getActiveAlerts = async (): Promise<Alert[]> => {
  const r = await api.get('/alerts/active')
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawAlert[]).map(mapAlert)
}

export const reportIncident = async (b: IncidentReportRequest): Promise<Incident> => {
  const payload = {
    title: b.title || `${b.type.charAt(0) + b.type.slice(1).toLowerCase()} Incident Report`,
    description: b.description,
    disasterType: b.type,
    severity: b.severity || 'MEDIUM',
    location: {
      latitude: b.lat,
      longitude: b.lon,
    },
  }
  const r = await api.post('/incidents/reports', payload)
  return mapIncident(r.data as RawIncident)
}

