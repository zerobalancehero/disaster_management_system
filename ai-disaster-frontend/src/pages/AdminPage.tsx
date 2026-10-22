import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as admin from '../api/admin.api'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import type { ApiError, Incident, Recommendation } from '../types'

function Row({ i }: { i: Incident }) {
  const qc = useQueryClient()
  const [reason, setReason] = useState('')
  const [recs, setRecs] = useState<Recommendation[] | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['admin-incidents'] })
    void qc.invalidateQueries({ queryKey: ['audit'] })
  }
  const opts = { onSuccess: refresh, onError: (e: ApiError) => { setErr(e.message); refresh() } }
  const verify = useMutation({ mutationFn: () => admin.verifyIncident(i.id), ...opts })
  const reject = useMutation({ mutationFn: () => admin.rejectIncident(i.id, reason), ...opts })
  const rec = useMutation({
    mutationFn: () => admin.requestRecommendations(i.id),
    onSuccess: setRecs,
    onError: (e: ApiError) => setErr(e.message),
  })
  const approve = useMutation({
    mutationFn: admin.approveRecommendation,
    onSuccess: () => { setRecs(null); refresh() },
    onError: opts.onError,
  })
  const decline = useMutation({
    mutationFn: (id: string) => admin.rejectRecommendation(id),
    onSuccess: (_d, id) => setRecs((r) => r?.filter((x) => x.id !== id) ?? null),
  })

  return (
    <li className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 transition-all hover:border-slate-700">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold text-white">{i.title || `${i.type} Incident`}</span>
          <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
            {i.type}
          </span>
        </div>
        <Badge text={i.status} />
      </div>

      <p className="text-xs text-slate-300 mb-2 leading-relaxed">{i.description}</p>

      <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-400 gap-2 border-t border-slate-800/80 pt-2">
        <span>📍 Coordinates: {i.lat.toFixed(4)}, {i.lon.toFixed(4)}</span>
        <span>🕒 Reported: {new Date(i.createdAt).toLocaleString()}</span>
        {i.reporterName && <span>👤 Reported by: {i.reporterName}</span>}
      </div>

      {i.rejectionReason && (
        <div className="mt-2 rounded-lg border border-red-500/20 bg-red-950/20 p-2 text-xs text-red-300">
          <strong>Rejection Justification:</strong> {i.rejectionReason}
        </div>
      )}

      {i.status === 'PENDING' && (
        <div className="mt-3 flex flex-wrap items-center gap-2 pt-2 border-t border-slate-800/60">
          <Btn onClick={() => verify.mutate()} className="bg-emerald-600 hover:bg-emerald-500">
            ✓ Verify Report
          </Btn>
          <div className="flex flex-1 items-center gap-2 min-w-[240px]">
            <input
              aria-label="Rejection reason"
              placeholder="Provide reason to reject..."
              className="w-full rounded-lg border border-slate-700 bg-slate-800/80 px-2.5 py-1 text-xs text-white focus:outline-none focus:border-red-500"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <Btn
              className="bg-red-700 hover:bg-red-600"
              disabled={!reason.trim()}
              onClick={() => reject.mutate()}
            >
              Reject
            </Btn>
          </div>
        </div>
      )}

      {i.status === 'VERIFIED' && (
        <div className="mt-3 pt-2 border-t border-slate-800/60">
          <Btn onClick={() => rec.mutate()} className="bg-indigo-600 hover:bg-indigo-500">
            🤖 Query AI Dispatch Recommendation Engine
          </Btn>
        </div>
      )}

      {recs && (
        <div className="mt-3 rounded-xl border border-indigo-500/20 bg-indigo-950/20 p-3 space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold text-indigo-300">
            <span>Ranked Dispatch Options</span>
            <span className="text-[10px] text-slate-400">Haversine + Capability Filtered</span>
          </div>
          {recs.length === 0 && (
            <p className="text-xs text-slate-400">No active responder squads currently available.</p>
          )}
          <ul className="space-y-2">
            {recs.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-indigo-500/30 bg-slate-900/80 p-2.5 text-xs">
                <div>
                  <div className="font-semibold text-white flex items-center gap-2">
                    {r.teamName}
                    {r.score && (
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300">
                        Match Score: {r.score}%
                      </span>
                    )}
                  </div>
                  <div className="text-[11px] text-slate-400 mt-0.5">
                    Skills: {r.skills.length > 0 ? r.skills.join(', ') : 'General rescue'}
                  </div>
                </div>
                <div className="flex gap-2">
                  <Btn
                    className="bg-emerald-600 hover:bg-emerald-500 text-xs"
                    onClick={() => {
                      if (window.confirm(`Confirm dispatch of ${r.teamName} to incident?`)) {
                        approve.mutate(r.id)
                      }
                    }}
                  >
                    Confirm Dispatch
                  </Btn>
                  <Btn className="bg-slate-700 hover:bg-slate-600 text-xs" onClick={() => decline.mutate(r.id)}>
                    Decline
                  </Btn>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {err && <p role="alert" className="mt-2 text-xs text-red-400">⚠️ {err}</p>}
    </li>
  )
}

export default function AdminPage() {
  const [filter, setFilter] = useState<string>('ALL')
  const inc = useQuery<Incident[], ApiError>({ queryKey: ['admin-incidents'], queryFn: admin.getIncidents })
  const logs = useQuery({ queryKey: ['audit'], queryFn: admin.getAuditLogs })

  const allIncidents = inc.data ?? []
  const filtered = allIncidents.filter((i) => {
    if (filter === 'ALL') return true
    return i.status === filter
  })

  const stats = {
    total: allIncidents.length,
    pending: allIncidents.filter((i) => i.status === 'PENDING').length,
    verified: allIncidents.filter((i) => i.status === 'VERIFIED').length,
    assigned: allIncidents.filter((i) => i.status === 'ASSIGNED').length,
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-3 text-center">
          <span className="text-[11px] uppercase tracking-wider text-slate-400">Total Reports</span>
          <div className="text-2xl font-bold text-white mt-1">{stats.total}</div>
        </div>
        <div className="rounded-xl border border-amber-500/20 bg-amber-950/20 p-3 text-center">
          <span className="text-[11px] uppercase tracking-wider text-amber-400">Pending Review</span>
          <div className="text-2xl font-bold text-amber-300 mt-1">{stats.pending}</div>
        </div>
        <div className="rounded-xl border border-blue-500/20 bg-blue-950/20 p-3 text-center">
          <span className="text-[11px] uppercase tracking-wider text-blue-400">Verified</span>
          <div className="text-2xl font-bold text-blue-300 mt-1">{stats.verified}</div>
        </div>
        <div className="rounded-xl border border-purple-500/20 bg-purple-950/20 p-3 text-center">
          <span className="text-[11px] uppercase tracking-wider text-purple-400">Active Dispatches</span>
          <div className="text-2xl font-bold text-purple-300 mt-1">{stats.assigned}</div>
        </div>
      </div>

      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="font-bold text-lg text-white">Emergency Incidents Queue</h2>
            <p className="text-xs text-slate-400">Review citizen reports, execute verification, and dispatch responder squads</p>
          </div>
          <div className="flex gap-1 rounded-xl bg-slate-800 p-1 text-xs">
            {['ALL', 'PENDING', 'VERIFIED', 'ASSIGNED', 'RESOLVED'].map((s) => (
              <button
                key={s}
                onClick={() => setFilter(s)}
                className={`px-2.5 py-1 rounded-lg font-medium transition-all cursor-pointer ${filter === s ? 'bg-sky-600 text-white shadow' : 'text-slate-400 hover:text-white'}`}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        <QueryState loading={inc.isLoading} error={inc.error} empty={filtered.length === 0} retry={() => void inc.refetch()} />
        <ul className="space-y-3">{filtered.map((i) => <Row key={i.id} i={i} />)}</ul>
      </Card>

      <Card>
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-bold text-sm text-slate-200">System Audit Trail & Operations Log</h2>
          <span className="text-[11px] text-slate-400">Append-Only Immutability</span>
        </div>
        <ul className="space-y-1.5 text-xs font-mono">
          {logs.data?.slice(0, 10).map((l) => (
            <li key={l.id} className="flex items-center justify-between rounded bg-slate-800/50 px-3 py-1.5 text-slate-300">
              <span className="text-slate-400">{new Date(l.at).toLocaleTimeString()}</span>
              <span className="font-semibold text-sky-400">{l.actor}</span>
              <span className="text-slate-200">{l.action}</span>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
