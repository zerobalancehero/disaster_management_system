import axios from 'axios'
import { config } from './config'
import { normalizeError } from './errors'

export const api = axios.create({
  baseURL: config.apiBaseUrl,
  withCredentials: true,
})

// Attach Bearer JWT token automatically if stored
api.interceptors.request.use((reqConfig) => {
  const token = localStorage.getItem('disaster_token')
  if (token) {
    reqConfig.headers.set('Authorization', `Bearer ${token}`)
  }
  return reqConfig
})

// Global response interceptor: 401 handling & structured error normalization
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('disaster_token')
      localStorage.removeItem('disaster_user')
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(normalizeError(error))
  }
)
