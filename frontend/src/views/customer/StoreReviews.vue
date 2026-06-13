<template>
  <div class="store-reviews-page">
    <div class="page-header">
      <el-button text @click="goBack">← 返回</el-button>
      <h2 style="margin: var(--space-md) 0 var(--space-xs)">{{ storeName }}</h2>
      <div class="store-rating" v-if="ratingStats">
        <el-rate :model-value="ratingStats.avgRating" disabled :max="5" show-score
                 :score-template="'{value} 分（共 ' + ratingStats.total + ' 条评价）'" />
      </div>
    </div>

    <!-- 评分统计 -->
    <div v-if="ratingStats" class="rating-stats panel">
      <div class="stat-item" v-for="item in ratingStats.breakdown" :key="item.stars">
        <span class="stars-label">{{ item.stars }} 星</span>
        <el-progress
          :percentage="ratingStats.total > 0 ? Math.round(item.count / ratingStats.total * 100) : 0"
          :stroke-width="10"
          :show-text="false"
          color="#f7ba2a"
          style="flex:1; margin: 0 10px"
        />
        <span class="count-label">{{ item.count }} 条</span>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="review-list">
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <!-- 用户信息与评分 -->
        <div class="review-header">
          <div class="user-info">
            <el-avatar :size="32" class="avatar-bg">
              {{ r.is_anonymous ? '匿' : (r.nickname ? r.nickname.charAt(0) : '客') }}
            </el-avatar>
            <span class="nickname">
              {{ r.is_anonymous ? '匿名用户' : (r.nickname || '顾客' + r.user_id) }}
            </span>
          </div>
          <el-rate :model-value="r.rating" disabled :max="5" />
        </div>

        <div class="review-time">{{ formatTime(r.created_at) }}</div>

        <!-- 各维度评分 -->
        <div v-if="hasDimensionScores(r)" class="dimension-scores">
          <span v-if="r.food_rating">🍽️ 菜品: {{ r.food_rating }}分</span>
          <span v-if="r.service_rating">🛎️ 服务: {{ r.service_rating }}分</span>
          <span v-if="r.environment_rating">🏠 环境: {{ r.environment_rating }}分</span>
          <span v-if="r.cat_rating">🐱 猫咪: {{ r.cat_rating }}分</span>
        </div>

        <!-- 评价内容 -->
        <p class="review-content">{{ r.content || '该用户未填写评价内容' }}</p>

        <!-- 关联猫咪 -->
        <div v-if="r.cat_name" class="cat-tag">
          🐱 评价了猫咪：{{ r.cat_name }}
        </div>

        <!-- 商家回复 -->
        <div v-if="r.reply" class="merchant-reply">
          <div class="reply-label">📢 商家回复：</div>
          <div class="reply-content">{{ r.reply }}</div>
          <div class="reply-time">{{ formatTime(r.replied_at) }}</div>
        </div>
      </div>

      <el-empty v-if="!loading && !reviews.length" description="暂无评价，快来抢沙发吧！" />
      <div v-if="loading" class="loading-text">加载中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'

const route = useRoute()
const router = useRouter()

const storeId = ref<number>(Number(route.params.id))
const storeName = ref<string>('门店详情')
const reviews = ref<any[]>([])
const loading = ref(false)
const ratingStats = ref<{ avgRating: number; total: number; breakdown: { stars: number; count: number }[] } | null>(null)

function hasDimensionScores(r: any) {
  return r.food_rating || r.service_rating || r.environment_rating || r.cat_rating
}

function formatTime(t: string | null) {
  if (!t) return ''
  return t.slice(0, 10)
}

function goBack() {
  router.push('/stores')
}

async function loadStoreInfo() {
  try {
    const stores = await api.get<any[]>('/stores', { id: storeId.value })
    if (stores && stores.length > 0) {
      storeName.value = stores[0].name || '门店详情'
    }
  } catch {
    storeName.value = '门店详情'
  }
}

async function loadReviews() {
  loading.value = true
  try {
    const data = await api.get<any[]>(`/reviews/store/${storeId.value}`, { limit: 100 })
    reviews.value = data || []

    // 计算统计
    if (reviews.value.length > 0) {
      const total = reviews.value.length
      const sum = reviews.value.reduce((acc, r) => acc + (r.rating || 0), 0)
      const avg = Math.round(sum / total * 10) / 10
      const breakdown = [5, 4, 3, 2, 1].map(stars => ({
        stars,
        count: reviews.value.filter(r => r.rating === stars).length
      }))
      ratingStats.value = { avgRating: avg, total, breakdown }
    }
  } catch (e: any) {
    const msg = e.response?.data?.message || e.message || '未知错误'
    ElMessage.error('加载评价失败：' + msg)
    console.error('加载评价失败', e)
    reviews.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadStoreInfo(), loadReviews()])
})
</script>

<style scoped>
.store-reviews-page { max-width: 800px; margin: 0 auto; padding: var(--space-base); }
.page-header { margin-bottom: var(--space-base); }
.store-rating { margin-top: var(--space-xs); }
.rating-stats.panel {
  background: #fff; padding: var(--space-base) var(--space-lg); border-radius: var(--radius-md);
  border: 1px solid #e8e5df; margin-bottom: var(--space-base);
}
.stat-item { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: 6px; font-size: var(--text-sm); }
.stars-label { width: 40px; text-align: right; color: #666; }
.count-label { width: 60px; color: #999; }
.review-list { margin-top: var(--space-sm); }
.review-card {
  background: #fff; padding: var(--space-base); border-radius: var(--radius-md);
  border: 1px solid #e8e5df; margin-bottom: var(--space-md);
}
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-xs); }
.user-info { display: flex; align-items: center; gap: var(--space-sm); }
.avatar-bg { background: #e8d5b7; color: #5c4a1e; font-size: var(--text-base); }
.nickname { font-size: var(--text-base); font-weight: 500; color: #333; }
.review-time { font-size: var(--text-xs); color: #999; margin-bottom: var(--space-sm); }
.dimension-scores {
  display: flex; gap: var(--space-base); flex-wrap: wrap; font-size: var(--text-sm);
  color: #4b5d6e; margin-bottom: var(--space-sm);
}
.review-content { font-size: var(--text-base); color: var(--ink); line-height: 1.6; margin-bottom: var(--space-sm); }
.cat-tag { font-size: var(--text-xs); color: #8b6914; margin-bottom: var(--space-sm); }
.merchant-reply {
  background: #f6f4ef; border-radius: var(--radius-sm);
  padding: var(--space-sm) var(--space-md); margin-top: var(--space-sm); font-size: var(--text-sm); color: #5c4a1e;
}
.reply-label { font-weight: 600; margin-bottom: var(--space-xs); }
.reply-content { line-height: 1.5; }
.reply-time { font-size: 11px; color: #999; margin-top: var(--space-xs); }
.loading-text { text-align: center; color: #999; padding: var(--space-base); }
</style>
