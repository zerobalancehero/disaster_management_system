import type { AssignmentStatus, Role } from '../types'

const NEXT: Record<AssignmentStatus, AssignmentStatus[]> = {
  APPROVED: ['EN_ROUTE'],
  EN_ROUTE: ['ON_SCENE'],
  ON_SCENE: ['RESOLVED'],
  RESOLVED: [],
}
export const canTransition = (from: AssignmentStatus, to: AssignmentStatus) => NEXT[from].includes(to)
export const nextStatuses = (from: AssignmentStatus) => NEXT[from]
export const homePath = (role: Role) => `/${role.toLowerCase()}`
