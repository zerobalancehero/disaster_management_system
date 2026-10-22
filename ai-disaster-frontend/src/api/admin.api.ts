import { api } from './client'
import type { Incident, Recommendation, Assignment, AuditLog } from '../types'

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
  reporter?: { name?: string }
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
    reporterName: raw.reporter?.name,
  }
}

interface RawRecommendation {
  id: string
  incidentId: string
  teamId?: string
  teamName?: string
  skills?: string[]
  available?: boolean
  score?: number
  matchReason?: string
  team?: {
    id: string
    name: string
    status?: string
    capabilities?: { capabilityType?: string }[]
  }
}

function mapRecommendation(raw: RawRecommendation): Recommendation {
  const teamSkills = raw.team?.capabilities
    ? raw.team.capabilities.map((c) => c.capabilityType ?? '').filter(Boolean)
    : raw.skills ?? []

  return {
    id: raw.id,
    incidentId: raw.incidentId,
    teamId: raw.team?.id ?? raw.teamId ?? '',
    teamName: raw.team?.name ?? raw.teamName ?? 'Responder Team',
    skills: teamSkills,
    available: raw.team ? raw.team.status === 'AVAILABLE' : (raw.available ?? true),
    score: raw.score,
    matchReason: raw.matchReason,
  }
}

export const getIncidents = async (): Promise<Incident[]> => {
  const r = await api.get('/admin/incidents')
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawIncident[]).map(mapIncident)
}

export const verifyIncident = async (id: string): Promise<Incident> => {
  const r = await api.patch(`/admin/incidents/${id}/verify`)
  return mapIncident(r.data as RawIncident)
}

export const rejectIncident = async (id: string, reason: string): Promise<Incident> => {
  const r = await api.patch(`/admin/incidents/${id}/reject`, {
    rejectionReason: reason,
  })
  return mapIncident(r.data as RawIncident)
}

export const requestRecommendations = async (id: string): Promise<Recommendation[]> => {
  const r = await api.post(`/admin/incidents/${id}/recommendations`)
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawRecommendation[]).map(mapRecommendation)
}

export const getRecommendations = async (id: string): Promise<Recommendation[]> => {
  const r = await api.get(`/admin/incidents/${id}/recommendations`)
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawRecommendation[]).map(mapRecommendation)
}

export const approveRecommendation = async (recId: string): Promise<Assignment> => {
  const r = await api.post<Assignment>(`/admin/assignments/${recId}/approve`)
  return r.data
}

export const rejectRecommendation = async (recId: string, reason?: string): Promise<void> => {
  await api.post(`/admin/assignments/${recId}/reject`, {
    rejectionReason: reason || 'Declined by administrator',
  })
}

interface RawAuditLog {
  id: string
  at?: string
  timestamp?: string
  actor?: string
  actorName?: string
  action: string
  details?: string
}

export const getAuditLogs = async (): Promise<AuditLog[]> => {
  const r = await api.get('/admin/audit-logs')
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawAuditLog[]).map((l) => ({
    id: l.id,
    at: l.timestamp ?? l.at ?? new Date().toISOString(),
    actor: l.actorName ?? l.actor ?? 'SYSTEM',
    action: l.action,
  }))
}

