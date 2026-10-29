/** 把换行/逗号/空格分隔的洞位文本解析成数组。 */
export function parseCellList(text: string): string[] {
  return text
    .split(/[\s,;，；]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

/** 洞位数组序列化为每行一个，便于编辑。 */
export function formatCellList(cells: string[] | null | undefined): string {
  return (cells ?? []).join('\n')
}

export function formatTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString('zh-CN')
}
