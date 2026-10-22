import type { Alert, Assignment, AuditLog, Incident, Recommendation, Team, User } from '../types'

export interface MockUser extends User { username: string; password: string }
interface Db {
  session: string | null; seq: number
  users: MockUser[]; teams: Team[]; incidents: Incident[]; alerts: Alert[]
  recs: Recommendation[]; assignments: Assignment[]; audit: AuditLog[]
}
const now = () => new Date().toISOString()

// SIMULATED DATA ONLY.
const seed = (): Db => ({
  session: null, seq: 100,
  users: [
    { id: 'u1', username: 'user', password: 'user123', name: 'Demo Citizen', role: 'USER' },
    { id: 'u2', username: 'responder', password: 'resp123', name: 'Demo Responder', role: 'RESPONDER', teamId: 't1' },
    { id: 'u3', username: 'admin', password: 'admin123', name: 'Demo Admin', role: 'ADMIN' },
  ],
  teams: [
    { id: 't1', name: 'Alpha Fire & Rescue (demo)', skills: ['fire', 'rescue'], available: true },
    { id: 't2', name: 'Bravo Medical (demo)', skills: ['medical'], available: true },
  ],
  incidents: [
    { id: 'i1', type: 'FLOOD', description: 'Simulated: street flooding reported.', lat: 13.08, lon: 80.27, status: 'PENDING', createdAt: now() },
    { id: 'i2', type: 'FIRE', description: 'Simulated: warehouse fire.', lat: 13.05, lon: 80.22, status: 'VERIFIED', createdAt: now() },
  ],
  alerts: [
    { id: 'a1', title: 'Simulated heavy rain advisory', severity: 'MODERATE', area: 'Demo Region', issuedAt: now(), expiresAt: new Date(Date.now() + 864e5).toISOString() },
    { id: 'a2', title: 'Simulated cyclone watch', severity: 'SEVERE', area: 'Demo Coast', issuedAt: now(), expiresAt: new Date(Date.now() + 2 * 864e5).toISOString() },
  ],
  recs: [], assignments: [], audit: [],
})

export let db: Db = seed()
export const resetMockState = () => { db = seed() }
export const nextId = (p: string) => `${p}${++db.seq}`
export const audit = (actor: string, action: string) =>
  db.audit.unshift({ id: nextId('log'), at: now(), actor, action })
