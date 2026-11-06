import { api } from './client'
export type Json = Record<string, unknown>
export type Loc = { latitude: number; longitude: number }
export type DisasterType = 'FLOOD' | 'EARTHQUAKE' | 'CYCLONE' | 'LANDSLIDE' | 'WILDFIRE' | 'GENERAL_EMERGENCY'
export type Horizon = 'NEXT_6H' | 'NEXT_24H' | 'NEXT_48H'
export type AssignmentStatus = 'APPROVED' | 'ACCEPTED' | 'EN_ROUTE' | 'ON_SCENE' | 'RESOLVED'

const one = <T = Json>(p: Promise<{ data: T }>) => p.then((r) => r.data as T)
const many = <T = Json>(p: Promise<{ data: unknown }>) =>
  p.then((r) => (Array.isArray(r.data) ? r.data : ((r.data as { content?: unknown[] })?.content ?? [])) as T[])
const q = (params?: Json) => ({ params })

// UNRESOLVED DTO = body typed as Json; confirm against the Spring records before relying on field names.
export const authApi = {
  login: (b: { email: string; password: string }) => one(api.post('/auth/login', b)),
  logout: () => api.post('/auth/logout'),
  me: () => one(api.get('/users/me')),
}
export const weatherApi = {
  current: (lat: number, lon: number) => one(api.get('/weather/current', q({ lat, lon }))),
  forecast: (lat: number, lon: number) => one(api.get('/weather/forecast', q({ lat, lon }))),
}
export const alertsApi = {
  active: () => many(api.get('/alerts/active')),
  history: () => many(api.get('/alerts/history')),
  byId: (id: string) => one(api.get(`/alerts/${id}`)),
  safety: (params?: Json) => many(api.get('/alerts/safety-instructions', q(params))),
  safetyFor: (id: string) => many(api.get(`/alerts/${id}/safety-instructions`)),
}
export const incidentsApi = {
  report: (b: { title: string; disasterType: DisasterType; description: string; location: Loc; severity?: string }) =>
    one(api.post('/incidents/reports', b)),
  mine: () =>
    many(api.get('/incidents/my-reports')).then((list) =>
      list.map((r: any) => {
        const lat = r.location?.latitude ?? r.location?.lat ?? r.latitude ?? r.lat ?? 0
        const lon = r.location?.longitude ?? r.location?.lon ?? r.longitude ?? r.lon ?? 0
        return {
          ...r,
          lat,
          lon,
          location: r.location ?? { latitude: lat, longitude: lon },
          type: r.disasterType ?? r.type ?? 'OTHER',
          status: r.verificationStatus ?? r.status ?? 'PENDING',
        }
      })
    ),
  mineById: (id: string) =>
    one(api.get(`/incidents/my-reports/${id}`)).then((r: any) => {
      const lat = r.location?.latitude ?? r.location?.lat ?? r.latitude ?? r.lat ?? 0
      const lon = r.location?.longitude ?? r.location?.lon ?? r.longitude ?? r.lon ?? 0
      return {
        ...r,
        lat,
        lon,
        location: r.location ?? { latitude: lat, longitude: lon },
        type: r.disasterType ?? r.type ?? 'OTHER',
        status: r.verificationStatus ?? r.status ?? 'PENDING',
      }
    }),
}
export const risksApi = {
  regions: (params?: Json) => many(api.get('/risks/regions', q(params))),
  region: (id: string) => one(api.get(`/risks/regions/${id}`)),
  history: (params?: Json) => many(api.get('/risks/history', q(params))),
  modelInfo: () => one(api.get('/risks/model-info')),
  calculate: (regionId: string, disasterType = 'FLOOD', horizon = 'NEXT_24H') =>
    one(api.post('/risks/calculate', null, { params: { regionId, disasterType, horizon } })),
  calculateAll: (disasterType = 'FLOOD', horizon = 'NEXT_24H') =>
    many(api.post('/risks/calculate-all', null, { params: { disasterType, horizon } })),
}
export const responderApi = {
  assignments: () => many(api.get('/responder/my-assignments')),
  assignment: (id: string) => one(api.get(`/responder/my-assignments/${id}`)),
  // Contract per spec: { status, message, location } — NOT newStatus/notes.
  setStatus: (id: string, b: { status: AssignmentStatus; message: string; location: Loc | null }) =>
    one(api.patch(`/responder/my-assignments/${id}/status`, b)),
  addUpdate: (id: string, b: { status?: string; message: string; location?: Loc | null; latitude?: number; longitude?: number }) => {
    const payload = {
      status: b.status || 'IN_PROGRESS',
      message: b.message,
      location: b.location ?? (b.latitude != null && b.longitude != null ? { latitude: b.latitude, longitude: b.longitude } : null)
    }
    return one(api.post(`/responder/my-assignments/${id}/updates`, payload))
  },
  setTeamStatus: (availabilityStatus: 'AVAILABLE' | 'OFF_DUTY') =>
    one(api.patch('/responder/team/status', { availabilityStatus })),
  notifications: () => many(api.get('/responder/notifications')),
  markRead: (id: string) => one(api.patch(`/responder/notifications/${id}/read`)),
  userNotifications: () => many(api.get('/users/notifications')),
  markUserRead: (id: string) => one(api.patch(`/users/notifications/${id}/read`)),
}
export const adminApi = {
  dashboard: () => one<Json>(api.get('/admin/dashboard')),
  incidents: (params?: Json) => many<Json>(api.get('/admin/incidents', q(params))),
  pending: () => many<Json>(api.get('/admin/incidents/pending')),
  incident: (id: string) => one<Json>(api.get(`/admin/incidents/${id}`)),
  verify: (id: string) => one<Json>(api.patch(`/admin/incidents/${id}/verify`)),
  reject: (id: string, rejectionReason: string) => one<Json>(api.patch(`/admin/incidents/${id}/reject`, { rejectionReason })),
  recommend: (id: string) => many<Json>(api.post(`/admin/incidents/${id}/recommendations`)),
  recommendations: (id: string) => many<Json>(api.get(`/admin/incidents/${id}/recommendations`)),
  approve: (recId: string) => one<Json>(api.post(`/admin/assignments/${recId}/approve`)),
  declineRec: (recId: string, rejectionReason: string) => one<Json>(api.post(`/admin/assignments/${recId}/reject`, { rejectionReason })),
  reassign: (id: string, b: Json) => one<Json>(api.post(`/admin/incidents/${id}/reassign`, b)), // UNRESOLVED: body
  teams: () => many<Json>(api.get('/admin/teams')),
  createTeam: (b: Json) => one<Json>(api.post('/admin/teams', b)),
  updateTeam: (id: string, b: Json) => one<Json>(api.patch(`/admin/teams/${id}`, b)),
  setMembers: (id: string, b: Json) => one<Json>(api.put(`/admin/teams/${id}/members`, b)),
  users: () => many<Json>(api.get('/admin/users')),
  updateUser: (id: string, b: Json) => one<Json>(api.patch(`/admin/users/${id}`, b)),
  setRole: (id: string, b: Json) => one<Json>(api.patch(`/admin/users/${id}/role`, b)),
  riskRegions: () => many<Json>(api.get('/admin/risks/regions')),
  modelInfo: () => one<Json>(api.get('/admin/risks/model-info')),
  createSafety: (b: Json) => one<Json>(api.post('/admin/safety-instructions', b)),
  updateSafety: (id: string, b: Json) => one<Json>(api.patch(`/admin/safety-instructions/${id}`, b)),
  performance: () => one<Json>(api.get('/admin/reports/response-performance')),
  auditLogs: (params?: Json) => many<Json>(api.get('/admin/audit-logs', q(params))),
  dataSources: () => one<Json>(api.get('/admin/data-sources/status')),
}
