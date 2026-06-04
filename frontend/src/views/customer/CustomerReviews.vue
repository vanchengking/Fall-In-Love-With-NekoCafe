<template>
  <div>
    <h2 style="margin-bottom: 20px">顾客评价</h2>

    <div class="review-form panel">
      <h3 style="margin-bottom: 12px">写评价</h3>
      <el-form label-position="top" @submit.prevent="submitReview">
        <el-form-item label="评分">
          <el-rate v-model="form.rating" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="分享你的体验..." />
        </el-form-item>
        <el-form-item label="关联猫咪（可选）">
          <el-select v-model="form.catId" clearable placeholder="选择猫咪" style="width: 100%">
            <el-option v-for="cat in cats" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button>
      </el-form>
    </div>

    <div class="review-list">
      <h3 style="margin: 24px 0 12px">全部评价</h3>
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <div class="review-header">
          <el-rate :model-value="r.rating" disabled :max="5" />
          <span class="review-time">{{ r.created_at?.slice(0, 10) || '' }}</span>
        </div>
        <p class="review-content">{{ r.content || '用户未填写评价内容' }}</p>
        <div class="review-meta">
          <span v-if="r.cat_id">猫咪 #{{ r.cat_id }}</span>
          <span v-if="r.reservation_id">预约 #{{ r.reservation_id }}</span>
        </div>
      </div>
      <el-empty v-if="!reviews.length" description="暂无评价" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import type { Review, Cat } from '@/types'

const auth = useAuthStore()
const reviews = ref<Review[]>([])
const cats = ref<Cat[]>([])
const submitting = ref(false)

const form = reactive({
  storeId: 1,
  catId: null as number | null,
  reservationId: null as number | null,
  rating: 5,
  content: '',
})

async function submitReview() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  submitting.value = true
  try {
    await api.post('/reviews', {
      storeId: form.storeId,
      catId: form.catId,
      reservationId: form.reservationId,
      rating: form.rating,
      content: form.content,
    })
    ElMessage.success('评价提交成功')
    form.content = ''
    form.catId = null
    form.rating = 5
    await loadReviews()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    submitting.value = false
  }
}

async function loadReviews() {
  try { reviews.value = await api.get<Review[]>('/reviews', { storeId: form.storeId }) }
  catch { reviews.value = [] }
}

onMounted(async () => {
  await loadReviews()
  try { cats.value = await api.get<Cat[]>('/cats', { storeId: form.storeId }) }
  catch { /* keep empty */ }
})
</script>

<style scoped>
.panel { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; margin-bottom: 20px; }
.review-list { margin-top: 8px; }
.review-card { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; margin-bottom: 10px; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.review-time { font-size: 13px; color: #667085; }
.review-content { font-size: 14px; color: #172033; line-height: 1.6; margin-bottom: 8px; }
.review-meta { display: flex; gap: 12px; font-size: 12px; color: #667085; }
</style>
