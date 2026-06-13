<template>
  <div class="photo-upload">
    <div v-if="modelValue" class="photo-preview">
      <img :src="modelValue" alt="photo" />
      <el-button class="remove-btn" type="danger" :icon="X" circle size="small" @click="$emit('update:modelValue', '')" />
    </div>
    <el-upload v-else :show-file-list="false" :before-upload="handleUpload" accept="image/*">
      <div class="upload-trigger">
        <el-icon :size="24"><Plus /></el-icon>
        <span>{{ placeholder }}</span>
      </div>
    </el-upload>
    <el-progress v-if="uploading" :percentage="100" :indeterminate="true" :stroke-width="4" style="margin-top: 6px" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { X } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import type { UploadRawFile } from 'element-plus'

defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const uploading = ref(false)

async function handleUpload(file: UploadRawFile) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)

    let token = ''
    try {
      const raw = localStorage.getItem('neko-auth')
      if (raw) token = JSON.parse(raw).token || ''
    } catch { /* ignore */ }

    const res = await fetch('/api/upload', {
      method: 'POST',
      body: formData,
      headers: { Authorization: `Bearer ${token}` },
    })

    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}))
      throw new Error(errBody?.error?.message || `HTTP ${res.status}`)
    }

    const json = await res.json()
    const url = json.data?.url
    if (url) {
      emit('update:modelValue', url)
      ElMessage.success('上传成功')
    } else {
      throw new Error('未返回文件 URL')
    }
  } catch (e) {
    ElMessage.error(`上传失败：${(e as Error).message}`)
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<style scoped>
.photo-upload { display: inline-block; }
.upload-trigger {
  width: 100px; height: 100px;
  border: 2px dashed #d1d5db; border-radius: var(--radius-md);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: var(--space-xs);
  color: #667085; font-size: var(--text-xs); cursor: pointer;
  transition: border-color 0.15s;
}
.upload-trigger:hover { border-color: #0f766e; color: #0f766e; }
.photo-preview { position: relative; display: inline-block; }
.photo-preview img { width: 100px; height: 100px; object-fit: cover; border-radius: var(--radius-md); border: 1px solid #e8e5df; }
.remove-btn { position: absolute; top: -6px; right: -6px; }
</style>
