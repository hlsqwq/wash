<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, Delete, Plus } from '@element-plus/icons-vue'

import ModelImageUpload from '../components/ModelImageUpload.vue'
import { createModel, getModel, updateModel } from '../api/models'
import type { ModelPayload } from '../types/model'
import { formatCellList, parseCellList } from '../utils/model'

interface RowDraft {
  tubeCount: number
  offsetX: number
  offsetY: number
}

interface SeparatorDraft {
  afterRow: number
  heightDp: number
}

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'model-edit')
const modelId = computed(() => String(route.params.id ?? ''))

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')

const form = reactive({
  code: '',
  name: '',
  description: '',
  imageUrl: null as string | null,
  dx: 1730,
  dy: 1488,
  wideCenterGap: false,
  rows: [{ tubeCount: 1, offsetX: 0, offsetY: 0 }] as RowDraft[],
  hideText: '',
  blockText: '',
  separators: [] as SeparatorDraft[]
})

const totalTubes = computed(() =>
  form.rows.reduce((sum, row) => sum + (Number(row.tubeCount) || 0), 0)
)

const rules: FormRules = {
  code: [
    { required: true, message: '请输入模型编码', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_-]{1,64}$/,
      message: '只能包含字母、数字、下划线和中划线，长度 1-64',
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { max: 128, message: '名称不能超过 128 个字符', trigger: 'blur' }
  ],
  dx: [{ required: true, message: '请输入 dx', trigger: 'change' }],
  dy: [{ required: true, message: '请输入 dy', trigger: 'change' }],
  rows: [{ validator: validateRows, trigger: 'change' }]
}

function validateRows(_rule: unknown, _value: unknown, callback: (error?: Error) => void) {
  if (!form.rows.length) {
    callback(new Error('至少需要一行'))
    return
  }
  const invalid = form.rows.findIndex(
    (row) => !Number.isInteger(row.tubeCount) || row.tubeCount < 1 || row.tubeCount > 200
  )
  if (invalid >= 0) {
    callback(new Error(`第 ${invalid + 1} 行管子数必须是 1-200 的整数`))
    return
  }
  callback()
}

onMounted(async () => {
  if (!isEdit.value) {
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const detail = await getModel(modelId.value)
    form.code = detail.code
    form.name = detail.name
    form.description = detail.description ?? ''
    form.imageUrl = detail.imageUrl
    form.dx = detail.dx
    form.dy = detail.dy
    form.wideCenterGap = detail.wideCenterGap
    form.rows = detail.rows.map((row) => ({
      tubeCount: row.tubeCount,
      offsetX: row.offsetX,
      offsetY: row.offsetY
    }))
    form.hideText = formatCellList(detail.hideCells)
    form.blockText = formatCellList(detail.blockCells)
    form.separators = detail.separators.map((item) => ({
      afterRow: item.afterRow,
      heightDp: item.heightDp
    }))
  } catch {
    loadError.value = '模型不存在或已被删除'
  } finally {
    loading.value = false
  }
})

function addRow() {
  form.rows.push({ tubeCount: 1, offsetX: 0, offsetY: form.rows.length })
}

function removeRow(index: number) {
  if (form.rows.length <= 1) {
    ElMessage.warning('至少保留一行')
    return
  }
  form.rows.splice(index, 1)
}

const batchVisible = ref(false)
const batch = reactive({
  count: 28,
  tubeCount: 10,
  offsetX: 0,
  offsetYStart: 0,
  offsetYStep: 1
})

async function openBatch() {
  if (form.rows.length > 1) {
    try {
      await ElMessageBox.confirm('批量生成会替换当前所有行数据，是否继续？', '批量生成', {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  batch.count = form.rows.length > 1 ? form.rows.length : 28
  batch.tubeCount = form.rows[0]?.tubeCount ?? 10
  batch.offsetX = form.rows[0]?.offsetX ?? 0
  batch.offsetYStart = form.rows[0]?.offsetY ?? 0
  batch.offsetYStep = 1
  batchVisible.value = true
}

function applyBatch() {
  const count = Math.floor(Number(batch.count))
  const tubeCount = Math.floor(Number(batch.tubeCount))
  const step = Number(batch.offsetYStep) || 0
  if (count < 1 || count > 300) {
    ElMessage.warning('行数必须在 1-300 之间')
    return
  }
  if (tubeCount < 1 || tubeCount > 200) {
    ElMessage.warning('每行管数必须在 1-200 之间')
    return
  }
  form.rows = Array.from({ length: count }, (_, index) => ({
    tubeCount,
    offsetX: Number(batch.offsetX) || 0,
    offsetY: Math.round(((Number(batch.offsetYStart) || 0) + index * step) * 1000) / 1000
  }))
  batchVisible.value = false
}

function addSeparator() {
  form.separators.push({ afterRow: 0, heightDp: 16 })
}

function removeSeparator(index: number) {
  form.separators.splice(index, 1)
}

function validateCells(tokens: string[], label: string): string | null {
  for (const token of tokens) {
    const matched = /^c_(\d+)_(\d+)$/.exec(token)
    if (!matched) {
      return `${label} 中的 "${token}" 格式不正确，应为 c_行_列`
    }
    const row = Number(matched[1])
    const col = Number(matched[2])
    if (row >= form.rows.length) {
      return `${label} 中的 "${token}" 行号越界`
    }
    if (col >= form.rows[row].tubeCount) {
      return `${label} 中的 "${token}" 列号越界`
    }
  }
  return null
}

function buildPayload(): ModelPayload {
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    description: form.description.trim(),
    imageUrl: form.imageUrl || null,
    dx: Number(form.dx),
    dy: Number(form.dy),
    wideCenterGap: form.wideCenterGap,
    rows: form.rows.map((row, index) => ({
      rowIndex: index,
      tubeCount: Number(row.tubeCount),
      offsetX: Number(row.offsetX) || 0,
      offsetY: Number(row.offsetY) || 0
    })),
    hideCells: parseCellList(form.hideText).map((cell) => cell.toLowerCase()),
    blockCells: parseCellList(form.blockText).map((cell) => cell.toLowerCase()),
    separators: [...form.separators]
      .map((item) => ({ afterRow: Number(item.afterRow), heightDp: Number(item.heightDp) }))
      .sort((a, b) => a.afterRow - b.afterRow)
  }
}

async function submit() {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  for (const [label, text] of [
    ['隐藏洞位', form.hideText],
    ['堵塞洞位', form.blockText]
  ] as const) {
    const message = validateCells(parseCellList(text).map((cell) => cell.toLowerCase()), label)
    if (message) {
      ElMessage.warning(message)
      return
    }
  }

  saving.value = true
  try {
    const payload = buildPayload()
    const saved = isEdit.value
      ? await updateModel(modelId.value, payload)
      : await createModel(payload)
    ElMessage.success(isEdit.value ? '模型已更新，版本号已递增' : '模型创建成功')
    router.push(`/models/${saved.id}`)
  } catch {
    // 错误提示已由 HTTP 拦截器统一处理。
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="page-header">
      <div>
        <el-button link :icon="ArrowLeft" @click="router.push('/models')">返回列表</el-button>
        <h1 class="page-title">{{ isEdit ? '编辑模型' : '新建模型' }}</h1>
        <p class="page-subtitle">
          行数、总管数与说明由服务端根据行数据重新计算；每次保存版本号自动 +1，Android 端据此刷新缓存。
        </p>
      </div>
      <div class="toolbar">
        <el-button @click="router.push('/models')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </div>
    </div>

    <el-result v-if="loadError" icon="warning" title="无法加载模型" :sub-title="loadError">
      <template #extra>
        <el-button type="primary" @click="router.push('/models')">返回列表</el-button>
      </template>
    </el-result>

    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      label-position="right"
    >
      <div class="card">
        <h2 class="card-title">基本信息</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="模型编码" prop="code">
              <el-input v-model="form.code" placeholder="例如 e107" maxlength="64" />
            </el-form-item>
            <el-form-item label="模型名称" prop="name">
              <el-input v-model="form.name" placeholder="例如 E107" maxlength="128" />
            </el-form-item>
            <el-form-item label="说明">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="2"
                maxlength="255"
                show-word-limit
                placeholder="留空时由服务端生成：共 X 行，合计 Y 根管子"
              />
            </el-form-item>
            <el-form-item label="中缝加宽">
              <el-switch v-model="form.wideCenterGap" active-text="启用" inactive-text="关闭" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="dx" prop="dx">
              <el-input-number v-model="form.dx" :min="1" :max="1000000" :step="1" />
            </el-form-item>
            <el-form-item label="dy" prop="dy">
              <el-input-number v-model="form.dy" :min="1" :max="1000000" :step="1" />
            </el-form-item>
            <el-form-item label="模型图片">
              <ModelImageUpload v-model="form.imageUrl" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="card">
        <div class="row-header">
          <div>
            <h2 class="card-title">行数据</h2>
            <p class="muted row-hint">
              共 {{ form.rows.length }} 行，{{ totalTubes }} 根管子。offsetX / offsetY 为每行行首偏移。
            </p>
          </div>
          <div class="toolbar">
            <el-button @click="openBatch">批量生成</el-button>
            <el-button type="primary" plain :icon="Plus" @click="addRow">添加一行</el-button>
          </div>
        </div>

        <el-form-item prop="rows" class="rows-form-item">
          <el-table :data="form.rows" size="small" border max-height="480" class="rows-table">
            <el-table-column label="行索引" width="80" align="center">
              <template #default="{ $index }">{{ $index }}</template>
            </el-table-column>
            <el-table-column label="管子数" width="150">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.tubeCount"
                  :min="1"
                  :max="200"
                  :step="1"
                  step-strictly
                  size="small"
                  controls-position="right"
                  style="width: 110px"
                />
              </template>
            </el-table-column>
            <el-table-column label="offsetX" min-width="150">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.offsetX"
                  :step="0.5"
                  :precision="2"
                  size="small"
                  controls-position="right"
                  style="width: 130px"
                />
              </template>
            </el-table-column>
            <el-table-column label="offsetY" min-width="150">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.offsetY"
                  :step="0.5"
                  :precision="2"
                  size="small"
                  controls-position="right"
                  style="width: 130px"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" :icon="Delete" @click="removeRow($index)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </div>

      <div class="card">
        <h2 class="card-title">特殊洞位与分隔条</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="隐藏洞位">
              <el-input
                v-model="form.hideText"
                type="textarea"
                :rows="6"
                placeholder="c_0_1&#10;c_0_7&#10;也支持逗号分隔"
              />
            </el-form-item>
            <p class="muted field-hint">格式 c_行_列（0 基），每行一个或用逗号分隔。</p>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="堵塞洞位">
              <el-input
                v-model="form.blockText"
                type="textarea"
                :rows="6"
                placeholder="c_21_1&#10;c_21_11"
              />
            </el-form-item>
            <p class="muted field-hint">堵塞洞位仅改变显示样式，不影响管位编号。</p>
          </el-col>
        </el-row>

        <div class="separator-header">
          <h3 class="sub-title">横向分隔条</h3>
          <el-button size="small" :icon="Plus" @click="addSeparator">添加分隔条</el-button>
        </div>
        <el-table v-if="form.separators.length" :data="form.separators" size="small" border>
          <el-table-column label="afterRow（0 基）" min-width="160">
            <template #default="{ row }">
              <el-input-number
                v-model="row.afterRow"
                :min="0"
                :max="Math.max(0, form.rows.length - 1)"
                :step="1"
                step-strictly
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column label="高度 dp" min-width="160">
            <template #default="{ row }">
              <el-input-number
                v-model="row.heightDp"
                :min="1"
                :max="500"
                :step="1"
                step-strictly
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" :icon="Delete" @click="removeSeparator($index)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-else class="muted field-hint">当前模型没有横向分隔条。</p>
      </div>

      <div class="form-actions">
        <el-button @click="router.push('/models')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </div>
    </el-form>

    <el-dialog v-model="batchVisible" title="批量生成行数据" width="420px">
      <el-form label-width="110px">
        <el-form-item label="行数">
          <el-input-number v-model="batch.count" :min="1" :max="300" :step="1" step-strictly />
        </el-form-item>
        <el-form-item label="每行管数">
          <el-input-number v-model="batch.tubeCount" :min="1" :max="200" :step="1" step-strictly />
        </el-form-item>
        <el-form-item label="offsetX">
          <el-input-number v-model="batch.offsetX" :step="0.5" :precision="2" />
        </el-form-item>
        <el-form-item label="起始 offsetY">
          <el-input-number v-model="batch.offsetYStart" :step="0.5" :precision="2" />
        </el-form-item>
        <el-form-item label="Y 步进">
          <el-input-number v-model="batch.offsetYStep" :step="0.5" :precision="2" />
        </el-form-item>
      </el-form>
      <p class="muted field-hint">生成结果是等管数的规则行；实际模型可再逐行调整。</p>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatch">生成并替换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.row-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.row-header .card-title {
  margin-bottom: 4px;
}

.row-hint {
  margin: 0;
  font-size: 12px;
}

.rows-form-item {
  margin-bottom: 0;
}

.rows-table {
  width: 100%;
}

.field-hint {
  font-size: 12px;
  margin: -8px 0 12px;
}

.separator-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 10px;
}

.sub-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
