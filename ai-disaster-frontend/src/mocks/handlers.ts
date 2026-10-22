import { http, HttpResponse } from 'msw'
import { audit, db, nextId } from './state'
import { canTransition } from '../lib/transitions'
import type { AssignmentStatus, IncidentReportRequest, Role, User } from '../types'

const B = '/api/v1'
const err = (status: number, message: string) => HttpResponse.json({ status, message }, { status })
type Info = { request: Request; params: unknown }

// Auth + role check + deterministic error injection via `x-mock-error: <status>` header.
const guard = (roles: Role[], fn: (u: User, req: Request, p: Record<string, string>) => Response | Promise<Response>) =>
  async ({ request, params }: Info) => {
    const forced = request.headers.get('x-mock-error')
    if (forced) return err(Number(forced), 'Simulated error')
    const u = db.users.find((x) => x.id === db.session)
    if (!u) return err(401, 'Not authenticated')
    if (!roles.includes(u.role)) return err(403, 'Forbidden')
    return fn(u, request, params as Record<string, string>)
  }
const ANY: Role[] = ['USER', 'RESPONDER', 'ADMIN']
const strip = ({ username: _a, password: _b, ...u }: (typeof db.users)[number]) => u

export const handlers = [
  http.post(`${B}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { username?: string; email?: string; password: string }
    const identifier = (body.email ?? body.username ?? '').toLowerCase()
    const u = db.users.find(
      (x) =>
        (x.username.toLowerCase() === identifier ||
          (x.email && x.email.toLowerCase() === identifier) ||
          identifier.startsWith(x.username.toLowerCase())) &&
        x.password === body.password
    )
    if (!u) return err(401, 'Invalid credentials')
    db.session = u.id
    return HttpResponse.json(strip(u))
  }),
  http.get(`${B}/users/me`, guard(ANY, (u) => HttpResponse.json(u))),
  http.post(`${B}/auth/logout`, () => { db.session = null; return new HttpResponse(null, { status: 204 }) }),

  http.get(`${B}/alerts/active`, guard(ANY, () =>
    HttpResponse.json(db.alerts.filter((a) => new Date(a.expiresAt) > new Date())))),
  http.post(`${B}/incidents/reports`, guard(['USER'], async (u, req) => {
    const b = (await req.json()) as IncidentReportRequest
    if (!b.description || b.description.length < 10) return err(400, 'Description too short')
    const inc = { ...b, id: nextId('i'), status: 'PENDING' as const, createdAt: new Date().toISOString() }
    db.incidents.push(inc)
    audit(u.name, `Submitted incident ${inc.id}`)
    return HttpResponse.json(inc, { status: 201 })
  })),

  http.get(`${B}/admin/incidents`, guard(['ADMIN'], () => HttpResponse.json(db.incidents))),
  http.patch(`${B}/admin/incidents/:id/verify`, guard(['ADMIN'], (u, _r, p) => {
    const i = db.incidents.find((x) => x.id === p.id)
    if (!i) return err(404, 'Incident not found')
    if (i.status !== 'PENDING') return err(409, `Cannot verify an incident in status ${i.status}`)
    i.status = 'VERIFIED'; audit(u.name, `Verified ${i.id}`)
    return HttpResponse.json(i)
  })),
  http.patch(`${B}/admin/incidents/:id/reject`, guard(['ADMIN'], async (u, req, p) => {
    const { reason } = (await req.json()) as { reason?: string }
    if (!reason?.trim()) return err(400, 'Rejection reason is required')
    const i = db.incidents.find((x) => x.id === p.id)
    if (!i) return err(404, 'Incident not found')
    if (i.status !== 'PENDING') return err(409, `Cannot reject an incident in status ${i.status}`)
    i.status = 'REJECTED'; i.rejectionReason = reason; audit(u.name, `Rejected ${i.id}: ${reason}`)
    return HttpResponse.json(i)
  })),
  http.post(`${B}/admin/incidents/:id/recommendations`, guard(['ADMIN'], (u, _r, p) => {
    const i = db.incidents.find((x) => x.id === p.id)
    if (!i) return err(404, 'Incident not found')
    if (i.status !== 'VERIFIED') return err(409, 'Only verified incidents can receive recommendations')
    db.recs = db.recs.filter((r) => r.incidentId !== i.id)
    const recs = db.teams.filter((t) => t.available).map((t) => ({
      id: nextId('rec'), incidentId: i.id, teamId: t.id, teamName: t.name, skills: t.skills, available: t.available,
    }))
    db.recs.push(...recs); audit(u.name, `Requested recommendations for ${i.id}`)
    return HttpResponse.json(recs)
  })),
  http.get(`${B}/admin/incidents/:id/recommendations`, guard(['ADMIN'], (_u, _r, p) =>
    HttpResponse.json(db.recs.filter((r) => r.incidentId === p.id)))),
  http.post(`${B}/admin/assignments/:id/approve`, guard(['ADMIN'], (u, _r, p) => {
    const rec = db.recs.find((r) => r.id === p.id)
    if (!rec) return err(404, 'Recommendation not found')
    const inc = db.incidents.find((i) => i.id === rec.incidentId)
    if (!inc || inc.status !== 'VERIFIED') return err(409, 'Incident is not awaiting assignment')
    const a = { id: nextId('as'), incidentId: inc.id, teamId: rec.teamId, status: 'APPROVED' as const, updates: [] }
    db.assignments.push(a); inc.status = 'ASSIGNED'
    db.recs = db.recs.filter((r) => r.incidentId !== inc.id)
    audit(u.name, `Approved ${rec.teamName} for ${inc.id}`)
    return HttpResponse.json(a, { status: 201 })
  })),
  http.post(`${B}/admin/assignments/:id/reject`, guard(['ADMIN'], (u, _r, p) => {
    if (!db.recs.some((r) => r.id === p.id)) return err(404, 'Recommendation not found')
    db.recs = db.recs.filter((r) => r.id !== p.id) // no assignment created
    audit(u.name, `Rejected recommendation ${p.id}`)
    return new HttpResponse(null, { status: 204 })
  })),
  http.get(`${B}/admin/audit-logs`, guard(['ADMIN'], () => HttpResponse.json(db.audit))),

  http.get(`${B}/responder/my-assignments`, guard(['RESPONDER'], (u) =>
    HttpResponse.json(db.assignments.filter((a) => a.teamId === u.teamId)
      .map((a) => ({ ...a, incident: db.incidents.find((i) => i.id === a.incidentId) }))))),
  http.patch(`${B}/responder/my-assignments/:id/status`, guard(['RESPONDER'], async (u, req, p) => {
    const { status } = (await req.json()) as { status: AssignmentStatus }
    const a = db.assignments.find((x) => x.id === p.id)
    if (!a) return err(404, 'Assignment not found')
    if (a.teamId !== u.teamId) return err(403, 'Assignment belongs to another team')
    if (!canTransition(a.status, status)) return err(409, `Invalid transition ${a.status} -> ${status}`)
    a.status = status
    const inc = db.incidents.find((i) => i.id === a.incidentId)
    if (inc && status === 'RESOLVED') inc.status = 'RESOLVED'
    audit(u.name, `Assignment ${a.id} -> ${status}`)
    return HttpResponse.json({ ...a, incident: inc })
  })),
  http.patch(`${B}/responder/team/status`, guard(['RESPONDER'], async (u, req) => {
    const { available } = (await req.json()) as { available: boolean }
    const t = db.teams.find((x) => x.id === u.teamId)
    if (!t) return err(404, 'Team not found')
    t.available = available; audit(u.name, `Team ${t.id} availability=${available}`)
    return HttpResponse.json(t)
  })),
]
