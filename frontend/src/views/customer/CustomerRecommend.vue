<template>
  <div class="recommend-page">
    <div class="page-header">
      <el-button @click="$router.back()">返回</el-button>
      <h2>专属推荐</h2>
      <el-button type="primary" @click="loadRecommendations" :loading="loading">刷新推荐</el-button>
    </div>

    <!-- 门店选择器 -->
    <div class="store-selector">
      <span class="selector-label">选择门店</span>
      <el-select
        v-model="storeId"
        placeholder="请选择门店"
        filterable
        style="width: 280px"
        @change="onStoreChange"
      >
        <el-option
          v-for="s in stores"
          :key="s.id"
          :label="s.name"
          :value="s.id"
        />
    </el-select>
    </div>

    <div v-if="storeId" class="recommend-content">
      <!-- 推荐猫咪 -->
      <div class="section">
        <h3>推荐猫咪 <span class="sub">最匹配你的猫咪伙伴</span></h3>
        <div class="cat-list">
          <div v-for="cat in recommendations.cat" :key="cat.id" class="cat-recommend-card">
            <div class="cat-img">
              <img v-if="cat.photo_url" :src="cat.photo_url" />
              <div v-else class="cat-no-img"><Cat :size="36" /></div>
            </div>
            <div class="cat-info">
              <h4>{{ cat.name }}</h4>
              <p class="cat-breed">{{ cat.breed }}</p>
              <div class="match-score">
                <span class="score-label">匹配度</span>
                <el-progress
                  :percentage="Math.min(100, Math.max(0, (cat.score || 0) * 5))"
                  :color="scoreColor(cat.score)"
                  :stroke-width="8"
                />
                <span class="score-num">{{ cat.score || 0 }}分</span>
              </div>
              <div class="reasons">
                <span v-for="(reason, idx) in cat.reasons" :key="idx" class="reason-tag">{{ reason }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!recommendations.cat || recommendations.cat.length === 0" class="empty-tip">
          暂无匹配猫咪
        </div>
      </div>

      <!-- 推荐桌位 -->
      <div class="section">
        <h3>推荐桌位 <span class="sub">适合你的用餐位置</span></h3>
        <div class="table-list">
          <div v-for="table in recommendations.tables" :key="table.id" class="table-recommend-card">
            <div class="table-icon">🪑</div>
            <div class="table-info">
              <strong>{{ table.code }}</strong>
              <span>{{ table.seats }}人 · {{ table.area }}</span>
              <div class="match-score">
                <el-progress
                  :percentage="Math.min(100, Math.max(0, (table.score || 0) * 5))"
                  :color="scoreColor(table.score)"
                  :stroke-width="6"
                />
                <span class="score-num">{{ table.score || 0 }}分</span>
              </div>
              <div class="reasons">
                <span v-for="(reason, idx) in table.reasons" :key="idx" class="reason-tag">{{ reason }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!recommendations.tables || recommendations.tables.length === 0" class="empty-tip">
          暂无可用桌位
        </div>
      </div>

      <!-- 推荐菜品 -->
      <div class="section">
        <h3>推荐菜品 <span class="sub">根据你的口味推荐</span></h3>
        <div class="menu-list">
          <div v-for="item in recommendations.menuItems" :key="item.id" class="menu-recommend-card">
            <img v-if="item.photo_url" :src="item.photo_url" class="menu-photo" />
            <div class="menu-info">
              <strong>{{ item.name }}</strong>
              <span class="menu-category">{{ item.category }}</span>
              <div class="match-score">
                <el-progress
                  :percentage="Math.min(100, Math.max(0, (item.score || 0) * 10))"
                  :color="scoreColor(item.score)"
                  :stroke-width="6"
                />
                <span class="score-num">{{ item.score || 0 }}分</span>
              </div>
              <div class="reasons">
                <span v-for="(reason, idx) in item.reasons" :key="idx" class="reason-tag">{{ reason }}</span>
              </div>
            </div>
            <div class="menu-price">{{ cents(item.price_cents) }}</div>
          </div>
        </div>
        <div v-if="!recommendations.menuItems || recommendations.menuItems.length === 0" class="empty-tip">
          暂无推荐菜品
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Cat } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const storeId = ref<number | null>(null)
const stores = ref<any[]>([])
const recommendations = ref<any>({
  cat: [],
  tables: [],
  menuItems: []
})

onMounted(async () => {
  // 加载门店列表
  try {
    const res = await api.get<any[]>('/stores')
    stores.value = res || []
    console.log('门店列表加载成功', stores.value)
  } catch (e: any) {
    console.error('加载门店失败', e)
    ElMessage.error('加载门店列表失败：' + (e.message || '未知错误'))
  }

  // 从路由参数或本地存储获取门店ID
  const id = route.query.storeId || localStorage.getItem('lastStoreId')
  if (id) {
    storeId.value = Number(id)
    await loadRecommendations()
  }
})

async function onStoreChange(val: number) {
  storeId.value = val
  localStorage.setItem('lastStoreId', String(val))
  await loadRecommendations()
}

async function loadRecommendations() {
  if (!storeId.value) {
    return
  }
  loading.value = true
  try {
    const prefs = Array.isArray(auth.user?.preferences) ? auth.user.preferences.join(',') : ''
    console.log('请求推荐参数:', { userId: auth.user?.id, storeId: storeId.value, preferences: prefs })
    const result = await api.get<any>('/recommendations', {
      userId: auth.user?.id,
      storeId: storeId.value,
      preferences: prefs || undefined,
    })
    console.log('推荐接口返回:', result)
    recommendations.value = {
      cat: result.cat ? [result.cat] : [],
      tables: result.tables || [],
      menuItems: result.menuItems || []
    }
  } catch (err: any) {
    console.error('推荐加载失败', err)
    ElMessage.error('推荐加载失败：' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function scoreColor(score?: number): string {
  if (!score) return '#909399'
  if (score >= 15) return '#67c23a'
  if (score >= 10) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.recommend-page { max-width: 900px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.page-header h2 { margin: 0; flex: 1; }

.store-selector { display: flex; align-items: center; gap: 10px; padding: 14px 18px; background: #fff; border-radius: 14px; border: 1px solid #f0ebe5; margin-bottom: 24px; }
.selector-label { font-size: 14px; color: #666; flex-shrink: 0; }

.section { margin-bottom: 32px; }
.section h3 { font-size: 18px; margin-bottom: 16px; color: #1a1a1a; }
.section h3 .sub { font-size: 13px; color: #999; font-weight: normal; margin-left: 8px; }

/* 猫咪推荐卡片 */
.cat-list { display: flex; flex-direction: column; gap: 16px; }
.cat-recommend-card { display: flex; gap: 16px; padding: 16px; background: #fff; border-radius: 14px; border: 1px solid #f0ebe5; }
.cat-img { width: 100px; height: 100px; border-radius: 12px; overflow: hidden; flex-shrink: 0; }
.cat-img img { width: 100%; height: 100%; object-fit: cover; }
.cat-no-img { width: 100%; height: 100%; background: #faf6f2; display: flex; align-items: center; justify-content: center; color: #bfbfbf; }
.cat-info { flex: 1; min-width: 0; }
.cat-info h4 { margin: 0 0 4px; font-size: 16px; }
.cat-breed { color: #999; font-size: 12px; margin-bottom: 8px; display: block; }
.match-score { display: flex; align-items: center; gap: 8px; margin: 8px 0; }
.score-label { font-size: 12px; color: #999; flex-shrink: 0; }
.score-num { font-size: 13px; font-weight: 600; color: #e86f51; flex-shrink: 0; min-width: 40px; }
.reasons { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 6px; }
.reason-tag { display: inline-block; background: #f6f3f0; color: #8c6d58; font-size: 11px; padding: 2px 8px; border-radius: 10px; border: 1px solid #e8e0d8; }

/* 桌位推荐 */
.table-list { display: flex; flex-direction: column; gap: 12px; }
.table-recommend-card { display: flex; align-items: flex-start; gap: 12px; padding: 14px 16px; background: #fff; border-radius: 12px; border: 1px solid #f0ebe5; }
.table-icon { font-size: 24px; flex-shrink: 0; }
.table-info { flex: 1; }
.table-info strong { font-size: 15px; }
.table-info > span { display: block; font-size: 13px; color: #999; margin: 2px 0 8px; }

/* 菜品推荐 */
.menu-list { display: flex; flex-direction: column; gap: 12px; }
.menu-recommend-card { display: flex; gap: 12px; padding: 14px 16px; background: #fff; border-radius: 12px; border: 1px solid #f0ebe5; align-items: center; }
.menu-photo { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.menu-info { flex: 1; min-width: 0; }
.menu-info strong { font-size: 15px; }
.menu-category { display: block; font-size: 13px; color: #999; margin: 2px 0 6px; }
.menu-price { font-weight: 700; color: #e86f51; font-size: 15px; flex-shrink: 0; }

.empty-tip { text-align: center; padding: 24px; color: #999; font-size: 14px; }
</style>
