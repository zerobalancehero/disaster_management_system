import type { GeoPointDto } from './api.types'
import type { AvailabilityStatus } from './responder.types'

export type TeamType = 'FIRE_AND_RESCUE' | 'MEDICAL_EMERGENCY' | 'HAZMAT' | 'WATER_RESCUE' | 'EVACUATION'
export type CapabilityType = 'WATER_RESCUE' | 'MEDICAL' | 'HAZMAT' | 'EVACUATION' | 'SEARCH_AND_RESCUE' | 'FIREFIGHTING'

export interface AdminDashboardResponse {
  totalIncidents: number
  pendingVerificationIncidents: number
  verifiedIncidents: number
  rejectedIncidents: number
  incidentsBySeverity: Record<string, number>
  activeDisasterAlerts: number
  totalResponderTeams: number
  availableTeams: number
  deployedTeams: number
  activeAssignments: number
  totalUsers: number
  modelStatus: string
  dataFreshnessStatus: string
}

export interface ResponsePerformanceReport {
  totalAssignments: number
  resolvedAssignments: number
  cancelledAssignments: number
  inProgressAssignments: number
  averageDispatchDurationMinutes: number
  averageResolutionDurationHours: number
  assignmentsByTeam: Record<string, number>
  assignmentsByStatus: Record<string, number>
}

export interface ProviderStatusDto {
  lastRunId?: string
  provider: string
  jobType: string
  lastRunTime: string
  status: 'SUCCESS' | 'FAILED' | 'RUNNING'
  recordsProcessed: number
  errorSummary?: string
  isFresh: boolean
}

export interface DataSourceStatusResponse {
  providers: ProviderStatusDto[]
}

export interface AuditLogResponse {
  id: string
  timestamp?: string
  /** Flat alias used by mocks/UI */
  at?: string
  actorId?: string
  actorName?: string
  /** Flat alias used by mocks/UI */
  actor?: string
  action: string
  details?: string
  entityType?: string
  entityId?: string
}

export interface TeamResponse {
  id: string
  name: string
  teamType?: TeamType
  availabilityStatus?: AvailabilityStatus
  baseLocation?: GeoPointDto
  contactDetails?: string
  memberCount?: number
  capabilities?: CapabilityType[]
  /** Flat field used by mocks/UI */
  skills?: string[]
  /** Flat field used by mocks/UI */
  available?: boolean
}

export interface CreateTeamRequest {
  name: string
  teamType: TeamType
  baseLocation?: GeoPointDto
  contactDetails?: string
  capabilities: CapabilityType[]
}

export interface UpdateTeamRequest {
  name?: string
  teamType?: TeamType
  baseLocation?: GeoPointDto
  contactDetails?: string
  capabilities?: CapabilityType[]
  availabilityStatus?: AvailabilityStatus
}

export interface TeamMembersRequest {
  userIds: string[]
}

export interface RecommendationResponse {
  id: string
  incidentId: string
  teamId: string
  teamName: string
  rank?: number
  estimatedTravelTimeMinutes?: number
  eligibility?: boolean
  availabilitySnapshot?: AvailabilityStatus
  recommendationReason?: string
  generationTime?: string
  recommendationStatus?: 'PROPOSED' | 'APPROVED' | 'REJECTED'
  score?: number
  skills?: string[]
  /** Flat availability flag used by mocks/UI */
  available?: boolean
  matchReason?: string
}

export interface RecommendationRejectRequest {
  rejectionReason: string
}
