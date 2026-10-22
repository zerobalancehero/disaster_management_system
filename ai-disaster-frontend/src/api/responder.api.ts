import { api } from './client'
import type { AssignmentStatus, AssignmentView } from '../types'

export const getMyAssignments = () => api.get<AssignmentView[]>('/responder/my-assignments').then((r) => r.data)
export const updateAssignmentStatus = (id: string, status: AssignmentStatus) =>
  api.patch<AssignmentView>(`/responder/my-assignments/${id}/status`, { status }).then((r) => r.data)
export const setTeamAvailability = (available: boolean) =>
  api.patch('/responder/team/status', { available }).then((r) => r.data)
