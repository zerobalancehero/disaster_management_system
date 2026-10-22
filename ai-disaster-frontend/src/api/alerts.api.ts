import { api } from './client'
import type { Alert, Incident, IncidentReportRequest } from '../types'

export const getActiveAlerts = () => api.get<Alert[]>('/alerts/active').then((r) => r.data)
export const reportIncident = (b: IncidentReportRequest) =>
  api.post<Incident>('/incidents/reports', b).then((r) => r.data)
