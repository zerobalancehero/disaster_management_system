import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMyAssignments, setTeamAvailability, updateAssignmentStatus } from '../api/responder.api'
import { Badge, Btn, Card, QueryState } from '../components/ui'
import { nextStatuses } from '../lib/transitions'
import type { ApiError, AssignmentView } from '../types'

export default function ResponderPage() {
  const qc = useQueryClient()
  const q = useQuery<AssignmentView[], ApiError>({ queryKey: ['my-assignments'], queryFn: getMyAssignments })
  const status = useMutation({
    mutationFn: (v: { id: string; s: Parameters<typeof updateAssignmentStatus>[1] }) => updateAssignmentStatus(v.id, v.s),
    onSettled: () => void qc.invalidateQueries({ queryKey: ['my-assignments'] }), // 409 also refreshes stale UI
  })
  const avail = useMutation({
    mutationFn: setTeamAvailability,
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['my-assignments'] }),
  })

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">🛡️</span>
              <h2 className="font-bold text-lg text-white">Squad Operational Readiness</h2>
            </div>
            <p className="text-xs text-slate-400">Control squad availability for incoming dispatch recommendations</p>
          </div>
          <div className="flex gap-2">
            <Btn
              className="bg-emerald-600 hover:bg-emerald-500 font-semibold px-4 py-2"
              onClick={() => avail.mutate(true)}
              disabled={avail.isPending}
            >
              🟢 Mark Squad Available
            </Btn>
            <Btn
              className="bg-slate-700 hover:bg-slate-600 font-semibold px-4 py-2"
              onClick={() => avail.mutate(false)}
              disabled={avail.isPending}
            >
              ⚪ Stand Down / Resting
            </Btn>
          </div>
        </div>
        {avail.isSuccess && (
          <p role="status" className="mt-3 text-xs text-emerald-400 border border-emerald-500/20 bg-emerald-950/20 p-2 rounded-lg">
            ✓ Squad readiness status synchronized with central command.
          </p>
        )}
      </Card>

      <Card>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="font-bold text-lg text-white">Active Team Dispatch Assignments</h2>
            <p className="text-xs text-slate-400">Execute field operations according to strict transition protocols</p>
          </div>
          <span className="text-xs text-slate-400 font-mono">
            {q.data?.length ?? 0} Assigned Missions
          </span>
        </div>

        <QueryState loading={q.isLoading} error={q.error} empty={q.data?.length === 0} retry={() => void q.refetch()} />
        {status.error && (
          <div role="alert" className="mb-3 rounded-lg border border-red-500/30 bg-red-950/20 p-3 text-xs text-red-300">
            ⚠️ {(status.error as unknown as ApiError).message}
          </div>
        )}

        <ul className="space-y-4">
          {q.data?.map((a) => (
            <li key={a.id} className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-base font-bold text-white">
                    {a.incident.title || `${a.incident.type} Mission`}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                    {a.incident.type}
                  </span>
                </div>
                <Badge text={a.status} />
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">{a.incident.description}</p>

              <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-400 gap-2 border-t border-slate-800/80 pt-2">
                <span>📍 Target Location: {a.incident.lat.toFixed(4)}, {a.incident.lon.toFixed(4)}</span>
                <span>📅 Incident Logged: {new Date(a.incident.createdAt).toLocaleTimeString()}</span>
              </div>

              {a.updates && a.updates.length > 0 && (
                <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-2.5 text-xs space-y-1">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Response Log:</span>
                  {a.updates.map((u, idx) => (
                    <div key={idx} className="text-[11px] text-slate-300 flex justify-between">
                      <span>• {u.note}</span>
                      <span className="text-slate-500 font-mono">{new Date(u.at).toLocaleTimeString()}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="pt-2 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-2">
                <span className="text-xs font-semibold text-slate-400">Advance Workflow:</span>
                <div className="flex flex-wrap gap-2">
                  {nextStatuses(a.status).map((s) => (
                    <Btn
                      key={s}
                      disabled={status.isPending}
                      className={s === 'RESOLVED' ? 'bg-emerald-600 hover:bg-emerald-500' : 'bg-sky-600 hover:bg-sky-500'}
                      onClick={() => status.mutate({ id: a.id, s })}
                    >
                      → Mark as {s.replace('_', ' ')}
                    </Btn>
                  ))}
                  {nextStatuses(a.status).length === 0 && (
                    <span className="text-xs text-emerald-400 font-medium">✓ Mission Completed</span>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
