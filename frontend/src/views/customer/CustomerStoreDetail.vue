<template>
  <div>
    <div class="detail-header">
      <el-button @click="$router.push('/stores')" :icon="ArrowLeft">返回</el-button>
      <h2 style="margin: 0 12px">{{ storeName }}</h2>
      <el-button type="primary" style="margin-left: auto" @click="$router.push(`/reservation`)">预约此门店</el-button>
    </div>
    <el-tabs>
      <el-tab-pane label="猫咪档案">
        <div class="cat-grid">
          <div v-for="cat in cats" :key="cat.id" class="cat-card">
            <div class="cat-img-wrap">
              <img v-if="cat.photo_url" :src="cat.photo_url" class="cat-photo" />
              <div v-else class="cat-no-img"><Cat :size="40" /></div>
            </div>
            <div class="cat-info">
              <h4>{{ cat.name }}</h4>
              <span class="cat-breed">{{ cat.breed }}</span>
              <span class="cat-health" :class="healthClass(cat.health_status)">{{ cat.health_status }}</span>
            </div>
            <div class="cat-tags">
              <span v-for="tag in cat.personality_tags" :key="tag" class="cat-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="桌位">
        <div class="table-grid">
          <div v-for="table in tables" :key="table.id" class="table-card">
            <div class="table-status-dot" :class="tableStatusClass(table.status)"></div>
            <strong>{{ table.code }}</strong>
            <span>{{ table.seats }} 人 · {{ table.area }}</span>
            <div class="table-tags">
              <el-tag v-if="table.cat_zone" size="small" type="success">猫区</el-tag>
              <el-tag size="small" :type="tableStatusTagType(table.status)">{{ tableStatusLabel(table.status) }}</el-tag>
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="菜单">
        <div class="menu-list">
          <div v-for="item in menuItems" :key="item.id" class="menu-row">
            <div style="display: flex; align-items: center; gap: 12px">
              <img v-if="item.photo_url" :src="item.photo_url" class="menu-photo" />
              <div>
                <strong>{{ item.name }}</strong>
                <span class="desc">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
              </div>
            </div>
            <div class="price-wrapper">
              <!-- 有折扣时显示原价（划线）和折扣价 -->
              <template v-if="memberDiscount < 1.0">
                <span class="original-price">{{ cents(item.price_cents) }}</span>
                <span class="discount-badge" v-if="memberDiscount === 0.85">8.5折</span>
                <span class="discount-badge" v-else-if="memberDiscount === 0.9">9折</span>
                <span class="discount-badge" v-else-if="memberDiscount === 0.95">9.5折</span>
                <span class="discounted-price">{{ cents(getDiscountedPrice(item.price_cents)) }}</span>
              </template>
              <!-- 无折扣时只显示原价 -->
              <template v-else>
                <span class="price">{{ cents(item.price_cents) }}</span>
              </template>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@lucide/vue'
import { Cat } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { fallbackTables, fallbackCats, fallbackMenuItems, fallbackStores } from '@/utils/fallback'
import type { DiningTable, Cat as CatType, MenuItem, Store } from '@/types'

const route = useRoute()
const storeId = Number(route.params.id)
const storeName = ref('')
const tables = ref<DiningTable[]>([])
const cats = ref<CatType[]>([])
const menuItems = ref<MenuItem[]>([])
const auth = useAuthStore()
const memberDiscount = ref<number>(1.0)  // 会员折扣率

onMounted(async () => {
  try {
    const [t, c, m, s] = await Promise.all([
      api.get<DiningTable[]>('/tables', { storeId }),
      api.get<CatType[]>('/cats', { storeId }),
      api.get<MenuItem[]>('/menu-items', { storeId }),
      api.get<Store[]>('/stores'),
    ])
    tables.value = t; cats.value = c; menuItems.value = m
    storeName.value = s.find(x => x.id === storeId)?.name || `门店 #${storeId}`
  } catch {
    tables.value = fallbackTables; cats.value = fallbackCats; menuItems.value = fallbackMenuItems
    storeName.value = fallbackStores.find(x => x.id === storeId)?.name || `门店 #${storeId}`
  }

  // 获取会员折扣率
  if (auth.isAuthenticated) {
    try {
      const memberInfo = await api.get<{discount: number}>('/users/me/member')
      memberDiscount.value = memberInfo.discount || 1.0
    } catch {
      memberDiscount.value = 1.0
    }
  }
})

// 计算折扣价
function getDiscountedPrice(priceCents: number): number {
  return Math.round(priceCents * memberDiscount.value)
}

// 桌位状态相关
function tableStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    available: '可用',
    occupied: '使用中',
    reserved: '已预约',
    maintenance: '维护中',
  }
  return map[status || 'available'] || status || '可用'
}
function tableStatusTagType(status?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    available: 'success',
    occupied: 'danger',
    reserved: 'warning',
    maintenance: 'info',
  }
  return map[status || 'available'] || 'success'
}
function tableStatusClass(status?: string): string {
  return 'dot-' + (status || 'available')
}

// 猫咪健康状态样式
function healthClass(status?: string): string {
  if (!status) return 'warn'
  const s = status.toLowerCase()
  if (s.includes('健康') || s.includes('healthy') || s.includes('正常')) return 'healthy'
  if (s.includes('疫苗') || s.includes('vaccin')) return 'vaccinated'
  return 'warn'
}
</script>

<style scoped>
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 12px; flex-wrap: wrap; }
.store-info { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.store-info h2 { margin: 0; font-size: 22px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 猫咪卡片 */
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
.cat-card { background: #fff; border-radius: 14px; border: 1px solid #e8e5df; overflow: hidden; display: flex; flex-direction: column; transition: box-shadow 0.2s; }
.cat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.cat-img-wrap { width: 100%; height: 160px; background: #faf6f2; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.cat-photo { width: 100%; height: 100%; object-fit: cover; display: block; }
.cat-no-img { color: #bfbfbf; display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; }
.cat-info { padding: 12px 14px 0; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.cat-info h4 { margin: 0; font-size: 15px; color: #1a1a1a; }
.cat-breed { font-size: 12px; color: #999; }
.cat-health { font-size: 11px; padding: 1px 8px; border-radius: 10px; margin-left: auto; flex-shrink: 0; }
.cat-health.healthy { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.cat-health.vaccinated { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
.cat-health.warn { background: #fffbe6; color: #faad14; border: 1px solid #ffe58f; }
.cat-tags { padding: 10px 14px 14px; display: flex; gap: 5px; flex-wrap: wrap; }
.cat-tag { display: inline-block; background: #f6f3f0; color: #8c6d58; font-size: 11px; padding: 2px 8px; border-radius: 10px; border: 1px solid #e8e0d8; }

/* 桌位卡片 */
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
.table-card { padding: 20px 16px; background: #fff; border-radius: 14px; border: 1px solid #f0ebe5; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 8px; position: relative; transition: box-shadow 0.2s; }
.table-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.table-status-dot { width: 10px; height: 10px; border-radius: 50%; margin-bottom: 2px; }
.dot-available { background: #52c41a; }
.dot-occupied { background: #ff4d4f; }
.dot-reserved { background: #faad14; }
.dot-maintenance { background: #8c8c8c; }
.table-card strong { font-size: 18px; color: #1a1a1a; }
.table-card > span { font-size: 13px; color: #667085; }
.table-tags { display: flex; gap: 4px; flex-wrap: wrap; justify-content: center; }

/* 菜单列表 */
.menu-list { display: flex; flex-direction: column; gap: 12px; }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: #fff; border-radius: 14px; border: 1px solid #f0ebe5; transition: box-shadow 0.2s; }
.menu-row:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.menu-photo { width: 64px; height: 64px; object-fit: cover; border-radius: 10px; flex-shrink: 0; }
.desc { display: block; font-size: 13px; color: #667085; margin-top: 2px; }
.price-wrapper { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.original-price { font-size: 12px; color: #999; text-decoration: line-through; }
.discount-badge { font-size: 11px; background: #e6f7ff; color: #1890ff; padding: 1px 6px; border-radius: 4px; font-weight: 600; }
.discounted-price { font-weight: 700; color: #e86f51; font-size: 15px; }
.price { font-weight: 700; color: #e86f51; }
</style>
