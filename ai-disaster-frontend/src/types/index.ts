export * from './api.types'
export * from './auth.types'
export * from './incident.types'
export * from './weather.types'
export * from './alert.types'
export * from './risk.types'
export * from './responder.types'
export * from './admin.types'

// Aliases for compatibility
export type Role = import('./auth.types').UserRole
export type Alert = import('./alert.types').DisasterAlertResponse
export type Assignment = import('./responder.types').AssignmentResponse
export type AssignmentView = import('./responder.types').AssignmentResponse
export type AuditLog = import('./admin.types').AuditLogResponse
export type Recommendation = import('./admin.types').RecommendationResponse
export type Team = import('./admin.types').TeamResponse
export type ApiError = { status: number; message: string }
