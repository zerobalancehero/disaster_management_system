import axios from 'axios'
import { config } from '../lib/config'
import { normalizeError } from './errors'

// The only Axios instance. HttpOnly-cookie sessions assumed (see README).
export const api = axios.create({ baseURL: config.apiBaseUrl, withCredentials: true })
api.interceptors.response.use((r) => r, (e) => Promise.reject(normalizeError(e)))
