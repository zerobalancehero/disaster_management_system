import { api } from './client'
export { reportIncident } from './incidents.api'
import type {
  DisasterAlertResponse,
  DisasterType,
  PagedResponse,
  SafetyInstructionResponse,
  Severity,
} from '../types'

export const getActiveAlerts = async (): Promise<DisasterAlertResponse[]> => {
  const r = await api.get('/alerts/active')
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
}

export const getAlertHistory = async (page = 0, size = 20): Promise<PagedResponse<DisasterAlertResponse>> => {
  const r = await api.get('/alerts/history', { params: { page, size } })
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

export const getAlertById = async (id: string): Promise<DisasterAlertResponse> => {
  const r = await api.get<DisasterAlertResponse>(`/alerts/${id}`)
  return r.data
}

export const getSafetyInstructions = async (
  disasterType?: DisasterType,
  severity?: Severity,
  lang = 'en'
): Promise<SafetyInstructionResponse[]> => {
  const r = await api.get('/alerts/safety-instructions', {
    params: { disasterType, severity, lang },
  })
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
}

export const getAlertSafetyInstructions = async (
  alertId: string,
  lang = 'en'
): Promise<SafetyInstructionResponse[]> => {
  const r = await api.get(`/alerts/${alertId}/safety-instructions`, {
    params: { lang },
  })
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? [])
}
