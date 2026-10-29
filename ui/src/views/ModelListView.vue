<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import { deleteModel, listModels } from '../api/models'
import type { ModelSummary } from '../types/model'
import { formatTime } from '../utils/model'

const router = useRouter()

const loading = ref(false)
const models = ref<ModelSummary[]>([])
const keyword = ref('')

const filtered = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) {
    return models.value
  }
  return models.value.filter(
    (item) => item.code.toLowerCase().includes(key) || item.name.toLowerCase().includes(key)
  )
})

const totalTubes = computed(() => models.value.reduce((sum, item) => sum + item.tubeCount, 0))

async function load() {
  loading.value = true
  try {
    models.value = await listModels()
  } catch {
    // 错误提示已由 HTTP 拦截器统一处理。
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goCreate() {
  router.push('/models/new')
}

function goDetail(row: ModelSummary) {
  router.push(`/models/${row.id}`)
}

function goEdit(row: ModelSummary) {
  router.push(`/models/${row.id}/edit`)
}

async function remove(row: ModelSummary) {
  try {
    await ElMessageBox.confirm(
      `删除后 Android 端将不再看到模型 ${row.code}，历史数据保留但不可访问。是否继续？`,
      '删除模型',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteModel(row.id)
    ElMessage.success('模型已删除')
    await load()
  } catch {
    // 错误提示已由 HTTP 拦截器统一处理。
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">模型列表</h1>
        <p class="page-subtitle">
          共 {{ models.length }} 个模型，合计 {{ totalTubes }} 根管子。Android 端按版本号自动刷新。
        </p>
      </div>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按编码或名称搜索"
          clearable
          :prefix-icon="Search"
          style="width: 220px"
        />
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="goCreate">新建模型</el-button>
      </div>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="filtered"
        row-key="id"
        stripe
        empty-text="暂无模型，点击右上角新建，或启动后端执行种子数据初始化。"
      >
        <el-table-column label="图片" width="96">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
              class="thumb"
            >
              <template #error>
                <div class="thumb thumb--empty">加载失败</div>
              </template>
            </el-image>
            <div v-else class="thumb thumb--empty">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="120">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row)">{{ row.code }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="行数" width="90" align="right">
          <template #default="{ row }">{{ row.rowCount }}</template>
        </el-table-column>
        <el-table-column label="管数" width="100" align="right">
          <template #default="{ row }">{{ row.tubeCount }}</template>
        </el-table-column>
        <el-table-column label="版本" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="goEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.thumb--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 12px;
  background: #fafbfd;
}
</style>
