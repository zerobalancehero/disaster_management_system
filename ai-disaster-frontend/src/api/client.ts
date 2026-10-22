import axios from 'axios'
import { config } from '../lib/config'
import { normalizeError } from './errors'

export const api = axios.create({
  baseURL: config.apiBaseUrl,
  withCredentials: true,
})

// Attach Bearer JWT token if available
api.interceptors.request.use((reqConfig) => {
  const token = localStorage.getItem('disaster_token')
  if (token && reqConfig.headers) {
    reqConfig.headers.Authorization = `Bearer ${token}`
  }
  return reqConfig
})

api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401) {
      localStorage.removeItem('disaster_token')
    }
    return Promise.reject(normalizeError(e))
  }
)

