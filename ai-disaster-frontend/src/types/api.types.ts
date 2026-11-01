export interface GeoPointDto {
  latitude: number
  longitude: number
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ApiErrorResponse {
  timestamp?: string
  status: number
  code?: string
  message: string
  path?: string
  details?: Record<string, string>
}
