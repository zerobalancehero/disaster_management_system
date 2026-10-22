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
  const avail = useMutation({ mutationFn: setTeamAvailability })
  return (
    <>
      <Card>
        <h2 className="mb-2 font-semibold">Team availability</h2>
        <div className="flex gap-2">
          <Btn onClick={() => avail.mutate(true)}>Available</Btn>
          <Btn className="bg-slate-600" onClick={() => avail.mutate(false)}>Unavailable</Btn>
        </div>
        {avail.isSuccess && <p role="status" className="mt-2 text-sm">Availability updated.</p>}
      </Card>
      <Card>
        <h2 className="mb-2 font-semibold">My assignments</h2>
        <QueryState loading={q.isLoading} error={q.error} empty={q.data?.length === 0} retry={() => void q.refetch()} />
        {status.error && <p role="alert" className="text-red-400">{(status.error as unknown as ApiError).message}</p>}
        <ul className="space-y-2">
          {q.data?.map((a) => (
            <li key={a.id} className="rounded bg-slate-800 p-3">
              <div className="flex items-center justify-between">
                <strong>{a.incident.type}</strong><Badge text={a.status} />
              </div>
              <p className="text-sm text-slate-300">{a.incident.description}</p>
              <div className="mt-2 flex gap-2">
                {nextStatuses(a.status).map((s) => (
                  <Btn key={s} disabled={status.isPending} onClick={() => status.mutate({ id: a.id, s })}>Mark {s.replace('_', ' ')}</Btn>))}
              </div>
            </li>))}
        </ul>
      </Card>
    </>
  )
}
