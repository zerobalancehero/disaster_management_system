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
  teamId?: string
  team?: { id?: string }
  status: string
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
  return {
    id: raw.id,
    incidentId: raw.incidentId ?? inc?.id ?? '',
    teamId: raw.team?.id ?? raw.teamId ?? '',
    status: raw.status as AssignmentStatus,
    updates: (raw.updates ?? []).map((u) => ({
      at: u.updateTime ?? u.at ?? new Date().toISOString(),
      note: u.notes ?? u.note ?? '',
    })),
    incident: {
      id: inc?.id ?? raw.incidentId ?? '',
      title: inc?.title,
      type: inc?.disasterType ?? inc?.type ?? 'OTHER',
      description: inc?.description ?? '',
      lat: inc?.latitude ?? inc?.lat ?? 0,
      lon: inc?.longitude ?? inc?.lon ?? 0,
      status: (inc?.verificationStatus ?? inc?.status ?? 'ASSIGNED') as AssignmentView['incident']['status'],
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
    status, // fallback for mock handlers
    notes: `Transitioned status to ${status}`,
  })
  return mapAssignment(r.data as RawAssignment)
}

export const setTeamAvailability = async (available: boolean): Promise<unknown> => {
  const r = await api.patch('/responder/team/status', {
    availabilityStatus: available ? 'AVAILABLE' : 'OFF_DUTY',
    available, // fallback for mock handlers
  })
  return r.data
}

