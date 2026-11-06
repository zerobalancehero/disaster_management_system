import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ShieldAlert,
  CheckCircle,
  XCircle,
  Cpu,
  MapPin,
  Clock,
  User,
  Search,
  Radio,
  History,
  AlertOctagon
} from 'lucide-react'
import * as admin from '../api/admin.api'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import type { ApiError, Incident, Recommendation } from '../types'

function IncidentRow({ i }: { i: Incident }) {
  const qc = useQueryClient()
  const [reason, setReason] = useState('')
  const [recs, setRecs] = useState<Recommendation[] | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const [isRejectOpen, setIsRejectOpen] = useState(false)

  const lat = i.lat || i.location?.latitude || 0
  const lon = i.lon || i.location?.longitude || 0

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['admin-incidents'] })
    void qc.invalidateQueries({ queryKey: ['audit'] })
    void qc.invalidateQueries({ queryKey: ['admin-dashboard'] })
  }

  const opts = { onSuccess: refresh, onError: (e: ApiError) => { setErr(e.message); refresh() } }
  const verify = useMutation({ mutationFn: () => admin.verifyIncident(i.id), ...opts })
  const reject = useMutation({
    mutationFn: () => admin.rejectIncident(i.id, reason),
    onSuccess: () => {
      setIsRejectOpen(false)
      refresh()
    },
    onError: (e: ApiError) => { setErr(e.message); refresh() }
  })
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
    <li className="rounded-xl border border-slate-800/80 bg-slate-900/70 p-4 transition-all hover:border-slate-700/90 hover:shadow-lg">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold text-white tracking-tight">{i.title || `${i.type} Incident`}</span>
          <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-sky-300 font-mono font-semibold border border-slate-700/60">
            {i.type}
          </span>
        </div>
        <Badge text={i.status} />
      </div>

      <p className="text-xs text-slate-300 mb-3 leading-relaxed">{i.description}</p>

      <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-400 gap-2 border-t border-slate-800/80 pt-2.5">
        <a
          href={`https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}`}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1 text-sky-400 hover:text-sky-300 underline"
        >
          <MapPin size={12} /> {lat.toFixed(4)}, {lon.toFixed(4)}
        </a>
        <span className="flex items-center gap-1">
          <Clock size={12} /> {new Date(i.createdAt ?? '').toLocaleString()}
        </span>
        {i.reporterName && (
          <span className="flex items-center gap-1 text-slate-300">
            <User size={12} /> Reporter: {i.reporterName}
          </span>
        )}
      </div>

      {i.rejectionReason && (
        <div className="mt-2.5 rounded-lg border border-red-500/30 bg-red-950/20 p-2.5 text-xs text-red-300 flex items-start gap-2">
          <AlertOctagon size={14} className="shrink-0 mt-0.5" />
          <div>
            <strong>Rejection Reason:</strong> {i.rejectionReason}
          </div>
        </div>
      )}

      {/* PENDING: Action buttons */}
      {i.status === 'PENDING' && (
        <div className="mt-3 pt-3 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Btn
              onClick={() => verify.mutate()}
              disabled={verify.isPending}
              className="bg-emerald-600 hover:bg-emerald-500 gap-1.5"
            >
              <CheckCircle size={14} /> Verify Incident
            </Btn>
            <Btn
              onClick={() => setIsRejectOpen(!isRejectOpen)}
              className="bg-rose-700 hover:bg-rose-600 gap-1.5"
            >
              <XCircle size={14} /> Reject
            </Btn>
          </div>

          {isRejectOpen && (
            <div className="w-full flex items-center gap-2 mt-2 pt-2 border-t border-rose-900/40">
              <input
                aria-label="Rejection reason"
                placeholder="Specify justification reason for rejection..."
                className="flex-1 rounded-lg border border-slate-700 bg-slate-800/90 px-3 py-1.5 text-xs text-white focus:outline-none focus:border-rose-500"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              <Btn
                className="bg-rose-600 hover:bg-rose-500"
                disabled={!reason.trim() || reject.isPending}
                onClick={() => reject.mutate()}
              >
                Confirm Reject
              </Btn>
            </div>
          )}
        </div>
      )}

      {/* VERIFIED: AI Recommendation trigger */}
      {i.status === 'VERIFIED' && (
        <div className="mt-3 pt-2.5 border-t border-slate-800/80">
          <Btn
            onClick={() => rec.mutate()}
            disabled={rec.isPending}
            className="bg-indigo-600 hover:bg-indigo-500 gap-1.5 w-full sm:w-auto"
          >
            <Cpu size={14} /> Trigger AI Squad Dispatch Recommendation
          </Btn>
        </div>
      )}

      {/* Recommendations view */}
      {recs && (
        <div className="mt-3 rounded-xl border border-indigo-500/30 bg-indigo-950/20 p-3.5 space-y-2.5">
          <div className="flex items-center justify-between text-xs font-semibold text-indigo-300">
            <span className="flex items-center gap-1.5">
              <Radio size={14} /> Ranked Squad Dispatch Options
            </span>
            <span className="text-[10px] text-slate-400 font-mono">Haversine Proximity + Skills Match</span>
          </div>

          {recs.length === 0 && (
            <p className="text-xs text-slate-400 italic">No available response squads match the criteria.</p>
          )}

          <ul className="space-y-2">
            {recs.map((r) => (
              <li
                key={r.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-indigo-500/30 bg-slate-900/90 p-3 text-xs"
              >
                <div>
                  <div className="font-semibold text-white flex items-center gap-2">
                    {r.teamName}
                    {r.score != null && (
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                        Score: {r.score}%
                      </span>
                    )}
                  </div>
                  <div className="text-[11px] text-slate-400 mt-1">
                    Specializations: {(r.skills?.length ?? 0) > 0 ? r.skills!.join(', ') : 'General Emergency Rescue'}
                  </div>
                </div>

                <div className="flex gap-2">
                  <Btn
                    className="bg-emerald-600 hover:bg-emerald-500 text-xs gap-1"
                    disabled={approve.isPending}
                    onClick={() => {
                      if (window.confirm(`Confirm immediate dispatch of ${r.teamName} to this incident?`)) {
                        approve.mutate(r.id)
                      }
                    }}
                  >
                    <CheckCircle size={13} /> Dispatch Squad
                  </Btn>
                  <Btn
                    className="bg-slate-700 hover:bg-slate-600 text-xs"
                    disabled={decline.isPending}
                    onClick={() => decline.mutate(r.id)}
                  >
                    Decline
                  </Btn>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {err && (
        <p role="alert" className="mt-2 text-xs text-rose-400 flex items-center gap-1.5">
          <AlertOctagon size={13} /> {err}
        </p>
      )}
    </li>
  )
}

export default function AdminPage() {
  const [filter, setFilter] = useState<string>('ALL')
  const [searchQuery, setSearchQuery] = useState<string>('')

  const inc = useQuery<Incident[], ApiError>({
    queryKey: ['admin-incidents'],
    queryFn: admin.getIncidents,
    refetchInterval: 15_000,
  })

  const logs = useQuery({
    queryKey: ['audit'],
    queryFn: admin.getAuditLogs,
    refetchInterval: 30_000,
  })

  const allIncidents = inc.data ?? []

  // Filtering & search
  const filtered = allIncidents.filter((i) => {
    const matchesFilter = filter === 'ALL' || i.status === filter
    const matchesSearch =
      !searchQuery.trim() ||
      (i.title?.toLowerCase().includes(searchQuery.toLowerCase()) ?? false) ||
      i.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.type.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (i.reporterName?.toLowerCase().includes(searchQuery.toLowerCase()) ?? false)
    return matchesFilter && matchesSearch
  })

  const stats = {
    total: allIncidents.length,
    pending: allIncidents.filter((i) => i.status === 'PENDING').length,
    verified: allIncidents.filter((i) => i.status === 'VERIFIED').length,
    assigned: allIncidents.filter((i) => i.status === 'ASSIGNED').length,
    resolved: allIncidents.filter((i) => i.status === 'RESOLVED').length,
  }

  return (
    <div className="space-y-6">
      {/* ── Top Incident Metrics ───────────────────────────── */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-3.5 text-center backdrop-blur-sm">
          <span className="text-[11px] uppercase tracking-wider text-slate-400 font-bold">Total Reports</span>
          <div className="text-2xl font-bold text-white mt-1 font-mono-numbers">{stats.total}</div>
        </div>
        <div className="rounded-xl border border-amber-500/20 bg-amber-950/20 p-3.5 text-center backdrop-blur-sm">
          <span className="text-[11px] uppercase tracking-wider text-amber-400 font-bold">Pending Triage</span>
          <div className="text-2xl font-bold text-amber-300 mt-1 font-mono-numbers">{stats.pending}</div>
        </div>
        <div className="rounded-xl border border-blue-500/20 bg-blue-950/20 p-3.5 text-center backdrop-blur-sm">
          <span className="text-[11px] uppercase tracking-wider text-blue-400 font-bold">Verified Ready</span>
          <div className="text-2xl font-bold text-blue-300 mt-1 font-mono-numbers">{stats.verified}</div>
        </div>
        <div className="rounded-xl border border-purple-500/20 bg-purple-950/20 p-3.5 text-center backdrop-blur-sm">
          <span className="text-[11px] uppercase tracking-wider text-purple-400 font-bold">Active Dispatches</span>
          <div className="text-2xl font-bold text-purple-300 mt-1 font-mono-numbers">{stats.assigned}</div>
        </div>
      </div>

      {/* ── Incident Queue Card ────────────────────────────── */}
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="font-bold text-lg text-white flex items-center gap-2">
              <ShieldAlert size={20} className="text-sky-400" /> Emergency Incidents Queue
            </h2>
            <p className="text-xs text-slate-400">
              Review citizen emergency reports, triage severity, and trigger AI-ranked squad dispatch
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* Search Input */}
            <div className="relative">
              <Search size={14} className="absolute left-2.5 top-2.5 text-slate-400" />
              <input
                type="text"
                placeholder="Search incidents..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 pr-3 py-1 text-xs rounded-lg border border-slate-700 bg-slate-800 text-white placeholder-slate-400 focus:outline-none focus:border-sky-500"
              />
            </div>

            {/* Filter Pills */}
            <div className="flex gap-1 rounded-xl bg-slate-800/80 p-1 text-xs border border-slate-700/60">
              {['ALL', 'PENDING', 'VERIFIED', 'ASSIGNED', 'RESOLVED'].map((s) => (
                <button
                  key={s}
                  onClick={() => setFilter(s)}
                  className={`px-2.5 py-1 rounded-lg font-semibold transition-all cursor-pointer ${
                    filter === s
                      ? 'bg-sky-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        </div>

        <QueryState
          loading={inc.isLoading}
          error={inc.error}
          empty={filtered.length === 0}
          retry={() => void inc.refetch()}
        />

        <ul className="space-y-3">
          {filtered.map((i) => (
            <IncidentRow key={i.id} i={i} />
          ))}
        </ul>
      </Card>

      {/* ── System Audit Trail ─────────────────────────────── */}
      <Card>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <History size={16} className="text-sky-400" />
            <h2 className="font-bold text-sm text-slate-200">System Audit Trail & Operations Log</h2>
          </div>
          <span className="text-[11px] text-slate-400 font-mono">Append-Only Immutability</span>
        </div>

        <div className="rounded-xl border border-slate-800/80 bg-slate-950/40 overflow-hidden">
          <ul className="divide-y divide-slate-800/60 text-xs font-mono">
            {logs.data?.slice(0, 10).map((l) => (
              <li
                key={l.id}
                className="flex flex-wrap items-center justify-between px-3.5 py-2 hover:bg-slate-800/30 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <span className="text-slate-500 text-[11px]">
                    {new Date(l.at ?? l.timestamp ?? '').toLocaleTimeString()}
                  </span>
                  <span className="font-semibold text-sky-400">{l.actor ?? l.actorName}</span>
                  <span className="text-slate-300">{l.action}</span>
                </div>
                {l.details && <span className="text-slate-500 text-[11px] truncate max-w-xs">{l.details}</span>}
              </li>
            ))}
            {(!logs.data || logs.data.length === 0) && !logs.isLoading && (
              <li className="p-4 text-center text-slate-500 text-xs">No audit events recorded yet.</li>
            )}
          </ul>
        </div>
      </Card>
    </div>
  )
}
