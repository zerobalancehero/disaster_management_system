import { api } from './client'
import type { AssignmentStatus, AssignmentView } from '../types'

interface RawUpdate {
  at?: string
  updateTime?: string
  note?: string
  notes?: string
}

interface RawAssignment {
  id: string
  incidentId?: string
  incidentTitle?: string
  incidentLocation?: { latitude?: number; longitude?: number }
  teamId?: string
  team?: { id?: string }
  status?: string
  assignmentStatus?: string
  updates?: RawUpdate[]
  incident?: {
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
  }
}

function mapAssignment(raw: RawAssignment): AssignmentView {
  const inc = raw.incident
  const incidentStatus = (inc?.verificationStatus ?? inc?.status ?? 'ASSIGNED') as import('../types').IncidentStatus
  const status = (raw.assignmentStatus ?? raw.status ?? 'APPROVED') as AssignmentStatus
  const lat = raw.incidentLocation?.latitude ?? inc?.latitude ?? inc?.lat ?? 0
  const lon = raw.incidentLocation?.longitude ?? inc?.longitude ?? inc?.lon ?? 0
  const title = raw.incidentTitle ?? inc?.title
  const desc = inc?.description ?? ''
  const type = inc?.disasterType ?? inc?.type ?? 'OTHER'

  return {
    id: raw.id,
    incidentId: raw.incidentId ?? inc?.id ?? '',
    teamId: raw.team?.id ?? raw.teamId ?? '',
    status,
    assignmentStatus: status,
    updates: (raw.updates ?? []).map((u) => ({
      at: u.updateTime ?? u.at ?? new Date().toISOString(),
      note: u.notes ?? u.note ?? '',
    })),
    incident: {
      id: inc?.id ?? raw.incidentId ?? '',
      title,
      type,
      description: desc,
      lat,
      lon,
      status: incidentStatus,
      createdAt: inc?.reportedTime ?? inc?.createdAt ?? new Date().toISOString(),
    },
  }
}

export const getMyAssignments = async (): Promise<AssignmentView[]> => {
  const r = await api.get('/responder/my-assignments')
  const list = Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
  return (list as RawAssignment[]).map(mapAssignment)
}

export const updateAssignmentStatus = async (
  id: string,
  status: AssignmentStatus
): Promise<AssignmentView> => {
  const r = await api.patch(`/responder/my-assignments/${id}/status`, {
    newStatus: status,
    notes: `Transitioned status to ${status}`,
  })
  return mapAssignment(r.data as RawAssignment)
}

export const setTeamAvailability = async (available: boolean): Promise<unknown> => {
  const r = await api.patch('/responder/team/status', {
    availabilityStatus: available ? 'AVAILABLE' : 'OFF_DUTY',
  })
  return r.data
}

