export type Role = 'USER' | 'RESPONDER' | 'ADMIN'
export interface User { id: string; name: string; role: Role; teamId?: string }
export type IncidentStatus = 'PENDING' | 'VERIFIED' | 'REJECTED' | 'ASSIGNED' | 'RESOLVED'
export interface Incident {
  id: string; type: string; description: string; lat: number; lon: number
  status: IncidentStatus; createdAt: string; rejectionReason?: string
}
export interface IncidentReportRequest { type: string; description: string; lat: number; lon: number }
export interface Team { id: string; name: string; skills: string[]; available: boolean }
export interface Recommendation { id: string; incidentId: string; teamId: string; teamName: string; skills: string[]; available: boolean }
export type AssignmentStatus = 'APPROVED' | 'EN_ROUTE' | 'ON_SCENE' | 'RESOLVED'
export interface Assignment { id: string; incidentId: string; teamId: string; status: AssignmentStatus; updates: { at: string; note: string }[] }
export interface AssignmentView extends Assignment { incident: Incident }
export interface Alert { id: string; title: string; severity: 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE'; area: string; issuedAt: string; expiresAt: string }
export interface AuditLog { id: string; at: string; actor: string; action: string }
export interface ApiError { status: number; message: string }
