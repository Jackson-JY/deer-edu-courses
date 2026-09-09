import axios, { type AxiosInstance } from 'axios'
import type { ApiResult, ApiError } from '@ielts/shared-types'

const TOKEN_KEY = 'ielts_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

const http: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(handleBizError(body as ApiError))
    }
    return resp
  },
  (error) => {
    const status = error?.response?.status
    // HTTP 401/403：token 失效或权限不足 → 清 token 并触发重新登录
    if (status === 401 || status === 403) {
      clearToken()
      window.dispatchEvent(new CustomEvent('auth:kicked'))
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }
    const body = error?.response?.data as ApiError | undefined
    if (body) return Promise.reject(handleBizError(body))
    return Promise.reject(error)
  },
)

// 统一处理业务错误码：被踢/未绑手机/未购等
function handleBizError(err: ApiError): ApiError {
  switch (err.bizCode) {
    case 'SESSION_KICKED':
      clearToken()
      // TODO: 弹出"账号已在别处登录"，跳登录
      window.dispatchEvent(new CustomEvent('auth:kicked'))
      break
    case 'NEED_BIND_PHONE':
      window.dispatchEvent(new CustomEvent('auth:need-bind-phone'))
      break
    case 'NO_ENTITLEMENT':
    case 'COURSE_EXPIRED':
      // 由调用方决定跳购买/续费
      break
    default:
      break
  }
  return err
}

// 解包 ApiResult.data，调用方直接拿业务数据
export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const resp = await http.get<ApiResult<T>>(url, { params })
  return resp.data.data
}
export async function post<T>(url: string, data?: unknown, headers?: Record<string, string>): Promise<T> {
  const resp = await http.post<ApiResult<T>>(url, data, { headers })
  return resp.data.data
}

/** 上传文件专用：不限超时（大文件上传可能很久） */
export async function postUpload<T>(url: string, formData: FormData): Promise<T> {
  const resp = await http.post<ApiResult<T>>(url, formData, { timeout: 0 })
  return resp.data.data
}
export async function put<T>(url: string, data?: unknown): Promise<T> {
  const resp = await http.put<ApiResult<T>>(url, data)
  return resp.data.data
}
export async function del<T>(url: string): Promise<T> {
  const resp = await http.delete<ApiResult<T>>(url)
  return resp.data.data
}

export { http }
