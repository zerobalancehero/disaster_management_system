import axios from 'axios'
import type { ApiError } from '../types'

export function normalizeError(e: unknown): ApiError {
  if (axios.isAxiosError(e)) {
    if (!e.response) return { status: 0, message: 'Network error: API unreachable.' }
    const body = e.response.data as { message?: string } | undefined
    return { status: e.response.status, message: body?.message ?? e.message }
  }
  return { status: 500, message: e instanceof Error ? e.message : 'Unknown error' }
}
