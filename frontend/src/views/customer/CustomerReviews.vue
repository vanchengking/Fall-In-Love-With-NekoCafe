<template>
  <div class="page-enter-active">
    <h2>顾客评价</h2>

    <div class="review-form panel">
      <h3>写评价</h3>
      <el-form ref="formRef" :model="form" :rules="reviewRules" label-position="top" @submit.prevent="submitReview">
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
      <h3 class="section-title">全部评价</h3>
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div v-for="i in 3" :key="i" class="review-card">
          <div style="display: flex; justify-content: space-between; margin-bottom: var(--space-sm)">
            <div class="skeleton" style="height: 16px; width: 100px"></div>
            <div class="skeleton" style="height: 13px; width: 80px"></div>
          </div>
          <div class="skeleton" style="height: 14px; width: 100%; margin-bottom: var(--space-xs)"></div>
          <div class="skeleton" style="height: 14px; width: 60%"></div>
        </div>
      </template>
      <template v-else>
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
      </template>
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
const loading = ref(true)
const formRef = ref()
const reviewRules = {
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }],
}

const form = reactive({
  storeId: 1,
  catId: null as number | null,
  reservationId: null as number | null,
  rating: 5,
  content: '',
})

async function submitReview() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  submitting.value = true
  try {
    await api.post('/reviews', {
      storeId: form.storeId, catId: form.catId,
      reservationId: form.reservationId, rating: form.rating, content: form.content,
    })
    ElMessage.success('评价提交成功')
    form.content = ''; form.catId = null; form.rating = 5
    await loadReviews()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { submitting.value = false }
}

async function loadReviews() {
  try { reviews.value = await api.get<Review[]>(`/reviews/store/${form.storeId}`) }
  catch {
    const { fallbackReviews } = await import('@/utils/fallback')
    reviews.value = fallbackReviews.filter(r => r.store_id === form.storeId)
  }
}

onMounted(async () => {
  await Promise.all([
    loadReviews(),
    api.get<Cat[]>('/cats', { storeId: form.storeId }).then(d => { cats.value = d }).catch(() => {}),
  ])
  loading.value = false
})
</script>

<style scoped>
.panel { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); margin-bottom: var(--space-lg); }
.panel h3 { margin-bottom: var(--space-md); }
.section-title { margin: var(--space-lg) 0 var(--space-md); }
.review-list { margin-top: var(--space-sm); }
.review-card { background: var(--paper); padding: var(--space-base); border-radius: var(--radius-lg); border: 1px solid var(--line); margin-bottom: var(--space-sm); transition: all var(--transition-fast); }
.review-card:hover { border-color: var(--teal-light); box-shadow: var(--shadow-sm); }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-sm); }
.review-time { font-size: var(--text-sm); color: var(--muted); }
.review-content { font-size: var(--text-sm); color: var(--ink); line-height: 1.6; margin-bottom: var(--space-sm); }
.review-meta { display: flex; gap: var(--space-md); font-size: var(--text-xs); color: var(--muted); }
</style>
