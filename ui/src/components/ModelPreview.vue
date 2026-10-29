<script setup lang="ts">
import { computed } from 'vue'

import type { ModelDetail, SeparatorSpec } from '../types/model'

const props = defineProps<{ model: ModelDetail }>()

const hideSet = computed(() => new Set(props.model.hideCells ?? []))
const blockSet = computed(() => new Set(props.model.blockCells ?? []))

function cellClass(rowIndex: number, colIndex: number): string {
  const tag = `c_${rowIndex}_${colIndex}`
  if (hideSet.value.has(tag)) {
    return 'cell cell--hidden'
  }
  if (blockSet.value.has(tag)) {
    return 'cell cell--block'
  }
  return 'cell'
}

function middleColumn(tubeCount: number): number {
  return Math.floor(tubeCount / 2) + 1
}

function separatorAfter(rowIndex: number): SeparatorSpec | undefined {
  return (props.model.separators ?? []).find((item) => item.afterRow === rowIndex)
}
</script>

<template>
  <div class="preview">
    <div class="preview__legend">
      <span><i class="legend-dot legend-dot--normal" />可用管位</span>
      <span><i class="legend-dot legend-dot--hidden" />隐藏洞位</span>
      <span><i class="legend-dot legend-dot--block" />阻挡洞位</span>
      <span v-if="model.wideCenterGap"><i class="legend-dot legend-dot--gap" />中央加宽</span>
    </div>

    <div class="preview__scroll">
      <div class="preview__body">
        <template v-for="row in model.rows" :key="row.rowIndex">
          <div class="preview__row">
            <span class="preview__row-label">{{ row.rowIndex + 1 }}</span>
            <div class="preview__cells">
              <span
                v-for="col in row.tubeCount"
                :key="col"
                :class="[
                  cellClass(row.rowIndex, col - 1),
                  {
                    'cell--center-gap':
                      model.wideCenterGap && col === middleColumn(row.tubeCount)
                  }
                ]"
                :title="`c_${row.rowIndex}_${col - 1}`"
              />
            </div>
          </div>
          <div
            v-if="separatorAfter(row.rowIndex)"
            class="preview__separator"
            :style="{ height: Math.max(4, separatorAfter(row.rowIndex)?.heightDp ?? 4) + 'px' }"
          >
            <span>分隔条 {{ separatorAfter(row.rowIndex)?.heightDp }}dp（行索引 {{ row.rowIndex }} 之后）</span>
          </div>
        </template>
      </div>
    </div>

    <p class="muted preview__note">
      示意图按行渲染管位，行首偏移与真实像素尺寸以 Android 端布局为准。
    </p>
  </div>
</template>

<style scoped>
.preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview__legend {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-muted);
}

.preview__legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-dot--normal {
  background: #5b9cf5;
}

.legend-dot--hidden {
  background: #e8edf5;
}

.legend-dot--block {
  background: #303133;
}

.legend-dot--gap {
  background: linear-gradient(90deg, #5b9cf5 40%, transparent 40%, transparent 60%, #5b9cf5 60%);
}

.preview__scroll {
  overflow: auto;
  max-height: 520px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fbfcfe;
  padding: 16px;
}

.preview__body {
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
  min-width: 100%;
}

.preview__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview__row-label {
  width: 24px;
  flex: none;
  text-align: right;
  font-size: 11px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.preview__cells {
  display: flex;
  gap: 3px;
  align-items: center;
}

.cell {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: #5b9cf5;
  flex: none;
}

.cell--hidden {
  background: #e8edf5;
  outline: 1px dashed #cfd8e6;
  outline-offset: -1px;
}

.cell--block {
  background: #303133;
}

.cell--center-gap {
  margin-left: 16px;
}

.preview__separator {
  position: relative;
  margin-left: 34px;
  margin-top: 2px;
  margin-bottom: 2px;
  border-radius: 3px;
  background: repeating-linear-gradient(
    45deg,
    #ffd591,
    #ffd591 6px,
    #ffe7ba 6px,
    #ffe7ba 12px
  );
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
}

.preview__separator span {
  font-size: 10px;
  color: #ad6800;
  white-space: nowrap;
  padding: 0 8px;
}

.preview__note {
  font-size: 12px;
  margin: 0;
}
</style>
