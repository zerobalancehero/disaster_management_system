import type { GeoPointDto } from './api.types'
import type { DisasterType, Severity } from './incident.types'

export type PredictionHorizon = 'NEXT_6H' | 'NEXT_24H' | 'NEXT_48H'
export type RegionType = 'COASTAL' | 'URBAN' | 'RIVER_BASIN' | 'MOUNTAIN' | 'INDUSTRIAL'

export interface RiskAssessmentResponse {
  id: string
  regionId: string
  regionName?: string
  disasterType: DisasterType
  riskProbability: number // 0.0 to 1.0
  predictedSeverity: Severity
  predictionHorizon: PredictionHorizon
  predictionTimestamp: string
  explanation?: string
  confidenceScore?: number
}

export interface RegionRiskResponse {
  id: string
  name: string
  regionType: RegionType
  centerPoint: GeoPointDto
  activeRisks: RiskAssessmentResponse[]
}

export interface RiskModelInfoResponse {
  modelName: string
  modelVersion: string
  lastTrainedTime: string
  accuracyRocAuc: number
  inferenceLatencyMs: number
  status: 'HEALTHY' | 'DEGRADED' | 'OFFLINE'
  supportedHazards: DisasterType[]
}
