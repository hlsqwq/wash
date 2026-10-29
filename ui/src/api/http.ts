import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

import type { ApiEnvelope } from '../types/model'

/**
 * 统一 HTTP 客户端。
 *
 * 后端所有 JSON 接口返回 { code, message, data }：
 * - code === 0 时直接把 data 交给调用方；
 * - code !== 0 时弹出后端给出的可读 message 并 reject。
 *
 * 图片读取接口返回二进制，不经过这里的 JSON 拆包。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiEnvelope<unknown> | undefined
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return response.data
  },
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    let message = '网络异常，请确认后端服务已启动'
    if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.response?.status === 413) {
      message = '上传内容超过大小限制'
    } else if (error.response?.data?.message) {
      message = error.response.data.message
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return http.get(url, { params }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: unknown): Promise<T> {
  return http.post(url, data) as unknown as Promise<T>
}

export function put<T>(url: string, data?: unknown): Promise<T> {
  return http.put(url, data) as unknown as Promise<T>
}

export function del<T>(url: string): Promise<T> {
  return http.delete(url) as unknown as Promise<T>
}

export default http
