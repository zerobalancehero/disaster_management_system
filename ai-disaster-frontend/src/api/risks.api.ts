import { api } from './client'
import type {
  DisasterType,
  PagedResponse,
  PredictionHorizon,
  RegionRiskResponse,
  RiskAssessmentResponse,
} from '../types'

export const getRegionalRisks = async (): Promise<RegionRiskResponse[]> => {
  const r = await api.get('/risks/regions')
  return Array.isArray(r.data) ? r.data : []
}

export const getRegionRisk = async (regionId: string): Promise<RegionRiskResponse> => {
  const r = await api.get<RegionRiskResponse>(`/risks/regions/${regionId}`)
  return r.data
}

export const getRiskHistory = async (
  regionId: string,
  page = 0,
  size = 20
): Promise<PagedResponse<RiskAssessmentResponse>> => {
  const r = await api.get('/risks/history', { params: { regionId, page, size } })
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

export const calculateRisk = async (
  regionId: string,
  disasterType: DisasterType = 'FLOOD',
  horizon: PredictionHorizon = 'NEXT_24H'
): Promise<RiskAssessmentResponse> => {
  const r = await api.post<RiskAssessmentResponse>('/risks/calculate', null, {
    params: { regionId, disasterType, horizon },
  })
  return r.data
}

export const calculateAllRegionsRisk = async (
  disasterType: DisasterType = 'FLOOD',
  horizon: PredictionHorizon = 'NEXT_24H'
): Promise<RiskAssessmentResponse[]> => {
  const r = await api.post<RiskAssessmentResponse[]>('/risks/calculate-all', null, {
    params: { disasterType, horizon },
  })
  return Array.isArray(r.data) ? r.data : []
}

export const getRiskModelInfo = async (): Promise<any> => {
  const r = await api.get('/risks/model-info')
  return r.data
}
