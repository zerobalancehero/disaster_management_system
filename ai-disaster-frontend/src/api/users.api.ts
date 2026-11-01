import { api } from './client'
import type { User } from '../types'

export const getMyProfile = async (): Promise<User> => {
  const r = await api.get<User>('/users/me')
  return r.data
}
