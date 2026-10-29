export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface RowSpec {
  rowIndex: number
  tubeCount: number
  offsetX: number
  offsetY: number
}

export interface SeparatorSpec {
  afterRow: number
  heightDp: number
}

export interface ModelSummary {
  id: number
  code: string
  name: string
  description: string
  imageUrl: string | null
  rowCount: number
  tubeCount: number
  version: number
  updateTime: string | null
}

export interface ModelDetail extends ModelSummary {
  dx: number
  dy: number
  wideCenterGap: boolean
  rows: RowSpec[]
  hideCells: string[]
  blockCells: string[]
  separators: SeparatorSpec[]
}

/** 创建/更新请求体，rowCount 与 tubeCount 由服务端计算。 */
export interface ModelPayload {
  code: string
  name: string
  description: string
  imageUrl: string | null
  dx: number
  dy: number
  wideCenterGap: boolean
  rows: RowSpec[]
  hideCells: string[]
  blockCells: string[]
  separators: SeparatorSpec[]
}

export interface ImageUploadResult {
  id: string
  url: string
  filename: string
}
