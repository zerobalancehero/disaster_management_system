import { api } from './client'
import type { Incident, Recommendation, Assignment, AuditLog } from '../types'

export const getIncidents = () => api.get<Incident[]>('/admin/incidents').then((r) => r.data)
export const verifyIncident = (id: string) => api.patch<Incident>(`/admin/incidents/${id}/verify`).then((r) => r.data)
export const rejectIncident = (id: string, reason: string) =>
  api.patch<Incident>(`/admin/incidents/${id}/reject`, { reason }).then((r) => r.data)
export const requestRecommendations = (id: string) =>
  api.post<Recommendation[]>(`/admin/incidents/${id}/recommendations`).then((r) => r.data)
export const getRecommendations = (id: string) =>
  api.get<Recommendation[]>(`/admin/incidents/${id}/recommendations`).then((r) => r.data)
// ASSUMPTION: {id} is the recommendation id. Confirm with backend.
export const approveRecommendation = (recId: string) =>
  api.post<Assignment>(`/admin/assignments/${recId}/approve`).then((r) => r.data)
export const rejectRecommendation = (recId: string) =>
  api.post(`/admin/assignments/${recId}/reject`).then(() => undefined)
export const getAuditLogs = () => api.get<AuditLog[]>('/admin/audit-logs').then((r) => r.data)
