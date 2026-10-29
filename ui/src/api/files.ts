import http from './http'
import type { ImageUploadResult } from '../types/model'

/** 上传模型图片，字段名必须为 file。返回后端代理地址，不暴露对象存储信息。 */
export function uploadImage(file: File): Promise<ImageUploadResult> {
  const form = new FormData()
  form.append('file', file)
  return http.post('/api/v1/files/images', form) as unknown as Promise<ImageUploadResult>
}
