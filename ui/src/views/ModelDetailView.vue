<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Edit } from '@element-plus/icons-vue'

import ModelPreview from '../components/ModelPreview.vue'
import { deleteModel, getModel } from '../api/models'
import type { ModelDetail } from '../types/model'
import { formatTime } from '../utils/model'

const props = defineProps<{ id?: string }>()
const router = useRouter()

const model = ref<ModelDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    model.value = await getModel(props.id ?? '')
  } catch {
    errorMessage.value = '模型不存在或已被删除'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goEdit() {
  if (model.value) {
    router.push(`/models/${model.value.id}/edit`)
  }
}

async function remove() {
  if (!model.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除后 Android 端将不再看到模型 ${model.value.code}，是否继续？`,
      '删除模型',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteModel(model.value.id)
    ElMessage.success('模型已删除')
    router.push('/models')
  } catch {
    // 错误提示已由 HTTP 拦截器统一处理。
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <el-result
      v-if="errorMessage"
      icon="warning"
      title="无法加载模型"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/models')">返回列表</el-button>
      </template>
    </el-result>

    <template v-else-if="model">
      <div class="page-header">
        <div>
          <el-button link :icon="ArrowLeft" @click="router.push('/models')">返回列表</el-button>
          <h1 class="page-title">
            {{ model.code }}
            <span class="muted detail-name">{{ model.name }}</span>
          </h1>
          <p class="page-subtitle">
            版本 v{{ model.version }} · 最后更新 {{ formatTime(model.updateTime) }}
          </p>
        </div>
        <div class="toolbar">
          <el-button :icon="Edit" type="primary" @click="goEdit">编辑</el-button>
          <el-button :icon="Delete" type="danger" plain @click="remove">删除</el-button>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-card">
          <span class="stat-card__label">总行数</span>
          <strong>{{ model.rowCount }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">总管数</span>
          <strong>{{ model.tubeCount }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">机台尺寸</span>
          <strong>{{ model.dx }} × {{ model.dy }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-card__label">隐藏 / 堵塞</span>
          <strong>{{ model.hideCells.length }} / {{ model.blockCells.length }}</strong>
        </div>
      </div>

      <div class="card">
        <h2 class="card-title">基本信息</h2>
        <div class="overview">
          <div class="overview__image">
            <el-image
              v-if="model.imageUrl"
              :src="model.imageUrl"
              :preview-src-list="[model.imageUrl]"
              preview-teleported
              fit="contain"
              class="overview__image-inner"
            >
              <template #error>
                <div class="overview__image-empty">图片加载失败</div>
              </template>
            </el-image>
            <div v-else class="overview__image-empty">暂无模型图片</div>
          </div>

          <el-descriptions :column="2" border class="overview__desc">
            <el-descriptions-item label="模型编码">{{ model.code }}</el-descriptions-item>
            <el-descriptions-item label="模型名称">{{ model.name }}</el-descriptions-item>
            <el-descriptions-item label="说明" :span="2">
              {{ model.description || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="宽 dx">{{ model.dx }}</el-descriptions-item>
            <el-descriptions-item label="高 dy">{{ model.dy }}</el-descriptions-item>
            <el-descriptions-item label="中缝加宽">
              {{ model.wideCenterGap ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="横向分隔条">
              {{ model.separators.length }} 条
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <div class="card">
        <h2 class="card-title">布局示意</h2>
        <ModelPreview :model="model" />
      </div>

      <div class="card">
        <h2 class="card-title">每行管子数</h2>
        <el-table :data="model.rows" size="small" max-height="460" border>
          <el-table-column prop="rowIndex" label="行索引" width="90" align="center" />
          <el-table-column prop="tubeCount" label="管子数" width="100" align="right" />
          <el-table-column prop="offsetX" label="offsetX" min-width="120" align="right" />
          <el-table-column prop="offsetY" label="offsetY" min-width="120" align="right" />
        </el-table>
      </div>

      <div class="card">
        <h2 class="card-title">特殊洞位与分隔条</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :md="8">
            <h3 class="sub-title">隐藏洞位（{{ model.hideCells.length }}）</h3>
            <div v-if="model.hideCells.length" class="tag-list">
              <el-tag v-for="cell in model.hideCells" :key="cell" size="small">{{ cell }}</el-tag>
            </div>
            <p v-else class="muted">无</p>
          </el-col>
          <el-col :xs="24" :md="8">
            <h3 class="sub-title">堵塞洞位（{{ model.blockCells.length }}）</h3>
            <div v-if="model.blockCells.length" class="tag-list">
              <el-tag v-for="cell in model.blockCells" :key="cell" size="small" type="info">
                {{ cell }}
              </el-tag>
            </div>
            <p v-else class="muted">无</p>
          </el-col>
          <el-col :xs="24" :md="8">
            <h3 class="sub-title">横向分隔条（{{ model.separators.length }}）</h3>
            <el-table v-if="model.separators.length" :data="model.separators" size="small" border>
              <el-table-column prop="afterRow" label="行索引之后" align="center" />
              <el-table-column prop="heightDp" label="高度 dp" align="center" />
            </el-table>
            <p v-else class="muted">无</p>
          </el-col>
        </el-row>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-name {
  font-size: 15px;
  font-weight: 400;
  margin-left: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 720px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.stat-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card__label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-card strong {
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.overview {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.overview__image {
  width: 280px;
  height: 200px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fafbfd;
  overflow: hidden;
  flex: none;
}

.overview__image-inner {
  width: 100%;
  height: 100%;
}

.overview__image-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
}

.overview__desc {
  flex: 1;
  min-width: 320px;
}

.sub-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 500;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
