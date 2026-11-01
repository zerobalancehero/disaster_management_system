import { api } from './client'
import type { Incident, IncidentReportRequest, PagedResponse } from '../types'

export const reportIncident = async (data: IncidentReportRequest): Promise<Incident> => {
  const r = await api.post<Incident>('/incidents/reports', data)
  return r.data
}

export const getMyReports = async (page = 0, size = 20): Promise<PagedResponse<Incident>> => {
  const r = await api.get('/incidents/my-reports', { params: { page, size } })
  if (Array.isArray(r.data)) {
    return {
      content: r.data,
      page,
      size,
      totalElements: r.data.length,
      totalPages: 1,
      last: true,
    }
  }
  return r.data
}

export const getMyReportById = async (id: string): Promise<Incident> => {
  const r = await api.get<Incident>(`/incidents/my-reports/${id}`)
  return r.data
}
