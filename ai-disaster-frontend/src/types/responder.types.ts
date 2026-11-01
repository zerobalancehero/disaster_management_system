import type { GeoPointDto } from './api.types'
import type { Incident } from './incident.types'

export type AssignmentStatus =
  | 'APPROVED'
  | 'ACCEPTED'
  | 'EN_ROUTE'
  | 'ON_SCENE'
  | 'RESOLVED'

export type AvailabilityStatus = 'AVAILABLE' | 'BUSY' | 'OFF_DUTY'

export interface ResponseUpdateDto {
  at?: string
  updateTime?: string
  note?: string
  message?: string
  location?: GeoPointDto
}

export interface AssignmentResponse {
  id: string
  incidentId: string
  incidentTitle?: string
  incidentLocation?: GeoPointDto
  teamId: string
  teamName?: string
  approvingAdminId?: string
  approvingAdminName?: string
  approvalTime?: string
  /** Canonical assignment status field */
  assignmentStatus?: AssignmentStatus
  /** Flat status alias used by UI and mappers */
  status?: AssignmentStatus
  assignmentTime?: string
  completionTime?: string
  rejectionOrCancellationReason?: string
  version?: number
  updates: ResponseUpdateDto[]
  incident?: Incident
}

export interface UpdateAssignmentStatusRequest {
  status: AssignmentStatus
  message?: string
  location?: GeoPointDto
}

export interface ResponseUpdateRequest {
  notes?: string
  message?: string
  location?: GeoPointDto
}

export interface UpdateTeamStatusRequest {
  availabilityStatus: AvailabilityStatus
}

export type NotificationType =
  | 'NEW_DISPATCH'
  | 'ASSIGNMENT_CANCELLED'
  | 'STATUS_CHANGE'
  | 'CRITICAL_ALERT'
  | 'SYSTEM_BROADCAST'

export interface NotificationResponse {
  id: string
  type: NotificationType
  title: string
  message: string
  relatedEntityType?: string
  relatedEntityId?: string
  createdAt: string
  readAt?: string
  deliveryStatus?: string
}
