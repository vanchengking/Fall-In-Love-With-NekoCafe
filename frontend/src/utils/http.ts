import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const http: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  try {
    const raw = localStorage.getItem('neko-auth')
    if (raw) {
      const { token } = JSON.parse(raw)
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
  } catch {
    // ignore
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (envelope && typeof envelope === 'object' && 'error' in envelope) {
      const msg = envelope.error?.message || '请求失败'
      return Promise.reject(new Error(msg))
    }
    const payload = envelope?.data ?? envelope
    if (payload === null || payload === undefined) {
      return Promise.reject(new Error('响应数据为空'))
    }
    return payload
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('neko-auth')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      const body = error.response.data
      const msg = body?.error?.message || body?.message || `HTTP ${status}`
      return Promise.reject(new Error(msg))
    }
    return Promise.reject(new Error(error.message || '网络连接失败'))
  },
)

function wrapData(payload: unknown) {
  return { data: payload }
}

export const api = {
  get<T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
    return http.get(url, { params, ...config }) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, payload?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.post(url, wrapData(payload), config) as unknown as Promise<T>
  },
  patch<T = unknown>(url: string, payload?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.patch(url, wrapData(payload), config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, payload?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.put(url, wrapData(payload), config) as unknown as Promise<T>
  },
  remove<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.delete(url, config) as unknown as Promise<T>
  },
}

export default http
