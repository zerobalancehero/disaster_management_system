import type { DisasterType, Severity } from './incident.types'

export type TargetAudience = 'GENERAL_PUBLIC' | 'FIRST_RESPONDERS' | 'VULNERABLE_POPULATIONS' | 'ADMINISTRATORS'

export interface DisasterAlertResponse {
  id: string
  source?: string
  externalSourceId?: string
  disasterType?: DisasterType
  severity: Severity
  title: string
  description?: string
  issueTime?: string
  issuedAt?: string
  expiryTime?: string
  /** Flat field used by UI */
  expiresAt?: string
  status?: 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
  affectedAreaWkt?: string
  impactedAreaName?: string
  /** Flat area name used by UI */
  area?: string
}

export interface SafetyInstructionResponse {
  id: string
  title: string
  disasterType: DisasterType
  minimumSeverity: Severity
  instructionText: string
  targetAudience: TargetAudience
  language: string
  active: boolean
  updatedAt: string
}

export interface SafetyInstructionRequest {
  title: string
  disasterType: DisasterType
  minimumSeverity: Severity
  instructionText: string
  targetAudience: TargetAudience
  language?: string
  active?: boolean
}
