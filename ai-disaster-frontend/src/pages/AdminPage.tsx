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
  const refresh = () => { void qc.invalidateQueries({ queryKey: ['admin-incidents'] }); void qc.invalidateQueries({ queryKey: ['audit'] }) }
  const opts = { onSuccess: refresh, onError: (e: ApiError) => { setErr(e.message); refresh() } }
  const verify = useMutation({ mutationFn: () => admin.verifyIncident(i.id), ...opts })
  const reject = useMutation({ mutationFn: () => admin.rejectIncident(i.id, reason), ...opts })
  const rec = useMutation({ mutationFn: () => admin.requestRecommendations(i.id), onSuccess: setRecs, onError: (e: ApiError) => setErr(e.message) })
  const approve = useMutation({ mutationFn: admin.approveRecommendation, onSuccess: () => { setRecs(null); refresh() }, onError: opts.onError })
  const decline = useMutation({ mutationFn: admin.rejectRecommendation, onSuccess: (_d, id) => setRecs((r) => r?.filter((x) => x.id !== id) ?? null) })
  return (
    <li className="rounded bg-slate-800 p-3">
      <div className="flex items-center justify-between"><strong>{i.type}</strong><Badge text={i.status} /></div>
      <p className="text-sm text-slate-300">{i.description}</p>
      {i.rejectionReason && <p className="text-sm text-red-300">Reason: {i.rejectionReason}</p>}
      {i.status === 'PENDING' && (
        <div className="mt-2 flex flex-wrap gap-2">
          <Btn onClick={() => verify.mutate()}>Verify</Btn>
          <input aria-label="Rejection reason" placeholder="Rejection reason" className="rounded bg-slate-700 px-2 text-sm" value={reason} onChange={(e) => setReason(e.target.value)} />
          <Btn className="bg-red-700 hover:bg-red-600" onClick={() => reject.mutate()}>Reject</Btn>
        </div>)}
      {i.status === 'VERIFIED' && <Btn className="mt-2" onClick={() => rec.mutate()}>Get team recommendations</Btn>}
      {recs && (
        <ul className="mt-2 space-y-1">
          {recs.length === 0 && <li className="text-sm text-slate-400">No available teams.</li>}
          {recs.map((r) => (
            <li key={r.id} className="flex items-center justify-between text-sm">
              <span>{r.teamName} · skills: {r.skills.join(', ')}</span>
              <span className="flex gap-2">
                <Btn onClick={() => { if (window.confirm(`Assign ${r.teamName}?`)) approve.mutate(r.id) }}>Approve</Btn>
                <Btn className="bg-slate-600" onClick={() => decline.mutate(r.id)}>Reject</Btn>
              </span>
            </li>))}
        </ul>)}
      {err && <p role="alert" className="mt-1 text-sm text-red-400">{err}</p>}
    </li>
  )
}

export default function AdminPage() {
  const inc = useQuery<Incident[], ApiError>({ queryKey: ['admin-incidents'], queryFn: admin.getIncidents })
  const logs = useQuery({ queryKey: ['audit'], queryFn: admin.getAuditLogs })
  return (
    <>
      <Card>
        <h2 className="mb-2 font-semibold">Incidents</h2>
        <QueryState loading={inc.isLoading} error={inc.error} empty={inc.data?.length === 0} retry={() => void inc.refetch()} />
        <ul className="space-y-2">{inc.data?.map((i) => <Row key={i.id} i={i} />)}</ul>
      </Card>
      <Card>
        <h2 className="mb-2 font-semibold">Recent audit activity</h2>
        <ul className="space-y-1 text-sm">{logs.data?.slice(0, 8).map((l) => <li key={l.id}>{new Date(l.at).toLocaleTimeString()} · {l.actor} · {l.action}</li>)}</ul>
      </Card>
    </>
  )
}
