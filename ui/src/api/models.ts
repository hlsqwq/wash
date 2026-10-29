import { del, get, post, put } from './http'
import type { ModelDetail, ModelPayload, ModelSummary } from '../types/model'

export function listModels(): Promise<ModelSummary[]> {
  return get<ModelSummary[]>('/api/v1/models')
}

export function getModel(id: number | string): Promise<ModelDetail> {
  return get<ModelDetail>(`/api/v1/models/${id}`)
}

export function createModel(payload: ModelPayload): Promise<ModelDetail> {
  return post<ModelDetail>('/api/v1/models', payload)
}

export function updateModel(id: number | string, payload: ModelPayload): Promise<ModelDetail> {
  return put<ModelDetail>(`/api/v1/models/${id}`, payload)
}

export function deleteModel(id: number | string): Promise<void> {
  return del<void>(`/api/v1/models/${id}`)
}
