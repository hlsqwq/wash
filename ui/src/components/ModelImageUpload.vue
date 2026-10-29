<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'

import { uploadImage } from '../api/files'

const props = defineProps<{ modelValue: string | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string | null): void }>()

const uploading = ref(false)
const ALLOWED = ['image/png', 'image/jpeg', 'image/webp']
const MAX_SIZE = 5 * 1024 * 1024

async function handleUpload(options: UploadRequestOptions) {
  const file = options.file
  if (file.size > MAX_SIZE) {
    ElMessage.error('图片不能超过 5MB')
    return
  }
  if (file.type && !ALLOWED.includes(file.type)) {
    ElMessage.error('仅支持 PNG / JPEG / WebP 图片')
    return
  }
  uploading.value = true
  try {
    const result = await uploadImage(file)
    emit('update:modelValue', result.url)
    ElMessage.success('图片上传成功')
  } catch {
    // 错误提示已由 HTTP 拦截器统一处理。
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="uploader">
    <div class="uploader__preview">
      <el-image
        v-if="props.modelValue"
        :src="props.modelValue"
        :preview-src-list="[props.modelValue]"
        preview-teleported
        fit="contain"
        class="uploader__image"
      />
      <div v-else class="uploader__empty">
        <span>暂无图片</span>
        <small>上传后可在这里预览</small>
      </div>
    </div>

    <div class="uploader__actions">
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        accept="image/png,image/jpeg,image/webp"
        :disabled="uploading"
      >
        <el-button :loading="uploading">{{ props.modelValue ? '更换图片' : '上传图片' }}</el-button>
      </el-upload>
      <el-button v-if="props.modelValue" text type="danger" @click="emit('update:modelValue', null)">
        移除
      </el-button>
      <span class="muted uploader__hint">PNG / JPEG / WebP，不超过 5MB</span>
    </div>
  </div>
</template>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.uploader__preview {
  width: 240px;
  height: 160px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background: #fafbfd;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.uploader__image {
  width: 100%;
  height: 100%;
}

.uploader__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.uploader__empty small {
  font-size: 12px;
}

.uploader__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.uploader__hint {
  font-size: 12px;
}
</style>
