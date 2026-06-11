<template>
  <div>
    <h2 style="margin-bottom: 20px">我的评价</h2>

    <!-- 写/改评价表单 -->
    <div class="review-form panel">
      <h3 style="margin-bottom: 12px">{{ editingId ? '修改评价' : '写评价' }}</h3>
      <el-form label-position="top" @submit.prevent="submitReview">
        <el-form-item label="选择门店 *">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width: 100%" @change="onStoreChange">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="综合评分">
          <el-rate v-model="form.rating" :max="5" show-score />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜品评分">
              <el-rate v-model="form.foodRating" :max="5" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务评分">
              <el-rate v-model="form.serviceRating" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="环境评分">
              <el-rate v-model="form.environmentRating" :max="5" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="猫咪评分">
              <el-rate v-model="form.catRating" :max="5" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="评价内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="分享你的体验..." />
        </el-form-item>
        <el-form-item label="关联猫咪（可选）">
          <el-select v-model="form.catId" clearable placeholder="选择猫咪" style="width: 100%">
            <el-option v-for="cat in cats" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isAnonymous">匿名评价（其他人将看不到你的昵称）</el-checkbox>
        </el-form-item>
        <div style="display: flex; gap: 8px">
          <el-button type="primary" :loading="submitting" @click="submitReview">提交评价</el-button>
          <el-button v-if="editingId" @click="cancelEdit">取消修改</el-button>
        </div>
      </el-form>
    </div>

    <!-- 评价列表 -->
    <div class="review-list">
      <h3 style="margin: 24px 0 12px">评价列表</h3>
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <div class="review-header">
          <el-rate :model-value="r.rating" disabled :max="5" />
          <span class="review-time">{{ r.created_at?.slice(0, 10) || '' }}</span>
        </div>

        <!-- 各维度评分 -->
        <div v-if="r.food_rating || r.service_rating || r.environment_rating || r.cat_rating"
             class="dimension-scores">
          <span v-if="r.food_rating">🍽️ 菜品: {{ r.food_rating }}分</span>
          <span v-if="r.service_rating">🛎️ 服务: {{ r.service_rating }}分</span>
          <span v-if="r.environment_rating">🏠 环境: {{ r.environment_rating }}分</span>
          <span v-if="r.cat_rating">🐱 猫咪: {{ r.cat_rating }}分</span>
        </div>

        <p class="review-content">{{ r.content || '用户未填写评价内容' }}</p>

        <!-- 商家回复 -->
        <div v-if="r.reply" class="merchant-reply">
          <div class="reply-label">📢 商家回复：</div>
          <div class="reply-content">{{ r.reply }}</div>
          <div class="reply-time">{{ r.replied_at?.slice(0, 10) }}</div>
        </div>

        <div class="review-meta">
          <span v-if="r.cat_id">猫咪 #{{ r.cat_id }}</span>
          <span v-if="r.reservation_id">预约 #{{ r.reservation_id }}</span>
          <div style="margin-left: auto; display: flex; gap: 8px">
            <!-- 未回复时可以修改/删除 -->
            <el-button
              v-if="!r.reply"
              type="primary"
              size="small"
              text
              @click="startEdit(r)"
            >修改</el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click="deleteReview(r.id)"
            >删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!reviews.length" description="暂无评价" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import type { Review, Cat, Store } from '@/types'

const auth = useAuthStore()
const reviews = ref<Review[]>([])
const cats = ref<Cat[]>([])
const stores = ref<Store[]>([])
const submitting = ref(false)
const editingId = ref<number | null>(null)

const defaultForm = {
  storeId: null as number | null,
  orderId: null as number | null,
  catId: null as number | null,
  reservationId: null as number | null,
  rating: 5,
  foodRating: 0,
  serviceRating: 0,
  environmentRating: 0,
  catRating: 0,
  content: '',
  isAnonymous: false,
}
const form = reactive({ ...defaultForm })

function resetForm() {
  Object.assign(form, defaultForm)
  form.storeId = null
  editingId.value = null
}

function onStoreChange() {
  form.catId = null
  if (form.storeId) {
    api.get<Cat[]>('/cats', { storeId: form.storeId }).then(list => cats.value = list).catch(() => cats.value = [])
  } else {
    cats.value = []
  }
}

async function submitReview() {
  if (!form.storeId) {
    ElMessage.warning('请先选择门店')
    return
  }
  if (!form.rating) {
    ElMessage.warning('请选择综合评分')
    return
  }
  submitting.value = true
  try {
    const payload: any = {
      storeId: form.storeId,
      rating: form.rating,
      content: form.content,
      foodRating: form.foodRating || null,
      serviceRating: form.serviceRating || null,
      environmentRating: form.environmentRating || null,
      catRating: form.catRating || null,
      isAnonymous: form.isAnonymous,
    }
    if (form.catId) payload.catId = form.catId
    if (form.orderId) payload.orderId = form.orderId
    if (form.reservationId) payload.reservationId = form.reservationId

    if (editingId.value) {
      await api.put(`/reviews/${editingId.value}`, payload)
      ElMessage.success('修改成功')
    } else {
      await api.post('/reviews', payload)
      ElMessage.success('评价提交成功')
    }
    resetForm()
    await loadReviews()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function startEdit(r: Review) {
  editingId.value = r.id
  form.rating = r.rating || 5
  form.content = r.content || ''
  form.foodRating = r.food_rating || 0
  form.serviceRating = r.service_rating || 0
  form.environmentRating = r.environment_rating || 0
  form.catRating = r.cat_rating || 0
  form.isAnonymous = r.is_anonymous || false
  form.catId = r.cat_id || null
}

function cancelEdit() {
  resetForm()
}

async function deleteReview(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这条评价？', '提示', { type: 'warning' })
    await api.remove(`/reviews/${id}`)
    ElMessage.success('删除成功')
    await loadReviews()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message)
  }
}

async function loadReviews() {
  try {
    reviews.value = await api.get<Review[]>('/reviews/my')
  } catch { reviews.value = [] }
}

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') } catch { stores.value = [] }
  await loadReviews()
  if (form.storeId) {
    try { cats.value = await api.get<Cat[]>('/cats', { storeId: form.storeId }) } catch { cats.value = [] }
  }
})
</script>

<style scoped>
.panel { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; margin-bottom: 20px; }
.review-list { margin-top: 8px; }
.review-card { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; margin-bottom: 10px; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.review-time { font-size: 13px; color: #667085; }
.review-content { font-size: 14px; color: #172033; line-height: 1.6; margin-bottom: 8px; }
.dimension-scores { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; color: #4b5d6e; margin-bottom: 8px; }
.review-meta { display: flex; gap: 12px; font-size: 12px; color: #667085; align-items: center; }
.merchant-reply {
  background: #f6f4ef;
  border-radius: 8px;
  padding: 10px 14px;
  margin: 8px 0;
  font-size: 13px;
  color: #5c4a1e;
}
.reply-label { font-weight: 600; margin-bottom: 4px; }
.reply-content { line-height: 1.5; }
.reply-time { font-size: 11px; color: #999; margin-top: 4px; }
</style>
