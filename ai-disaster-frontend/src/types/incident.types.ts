import type { GeoPointDto } from './api.types'

export type DisasterType =
  | 'FLOOD'
  | 'EARTHQUAKE'
  | 'CYCLONE'
  | 'LANDSLIDE'
  | 'WILDFIRE'
  | 'GENERAL_EMERGENCY'

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type Severity = 'LOW' | 'MEDIUM' | 'MODERATE' | 'HIGH' | 'SEVERE' | 'CRITICAL'

export type IncidentStatus =
  | 'PENDING'
  | 'VERIFIED'
  | 'ASSIGNED'
  | 'RESOLVED'
  | 'REJECTED'

export interface IncidentReportRequest {
  title: string
  description: string
  disasterType: DisasterType
  severity?: IncidentSeverity | string
  location: GeoPointDto
}

export interface Incident {
  id: string
  reporterId?: string
  reporterName?: string
  title?: string
  description: string
  /** Flat disaster type used by UI and mappers */
  type: string
  /** Canonical field from API (alias of type) */
  disasterType?: DisasterType
  severity?: Severity
  /** Flat lat/lon used by UI and mappers */
  lat: number
  lon: number
  /** Nested location from API */
  location?: GeoPointDto
  reportedTime?: string
  createdAt?: string
  /** Flat status used by UI and mappers (aliases verificationStatus) */
  status: IncidentStatus
  verificationStatus?: IncidentStatus
  verifierId?: string
  verifierName?: string
  verificationTime?: string
  rejectionReason?: string
  assignedTeamId?: string
  assignedTeamName?: string
}

export interface VerifyIncidentRequest {
  severityOverride?: Severity
  comments?: string
}

export interface RejectIncidentRequest {
  rejectionReason: string
}

export interface ReassignIncidentRequest {
  reason: string
}
