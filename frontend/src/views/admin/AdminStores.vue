<template>
  <div>
    <h2 style="margin-bottom: 20px">门店管理</h2>
    <div v-for="store in stores" :key="store.id" class="store-section">
      <h3>{{ store.name }}</h3>
      <p>{{ store.city }} · {{ store.address }} · {{ store.phone }}</p>
      <p>{{ store.table_count }} 桌 · {{ store.total_seats }} 座</p>

      <h4 style="margin: 16px 0 8px">桌位列表</h4>
      <div class="table-grid">
        <div v-for="table in getTables(store.id)" :key="table.id" class="table-card">
          <strong>{{ table.code }}</strong>
          <span>{{ table.seats }}人 · {{ table.area }}</span>
          <el-tag v-if="table.cat_zone" size="small" type="success">猫区</el-tag>
        </div>
      </div>

      <h4 style="margin: 16px 0 8px">菜品管理</h4>
      <div class="menu-grid">
        <div v-for="item in getMenus(store.id)" :key="item.id" class="menu-card">
          <div class="menu-photo-area">
            <PhotoUpload :model-value="item.photo_url || ''" @update:model-value="updateMenuPhoto(item.id, $event)" placeholder="菜品图片" />
          </div>
          <div class="menu-detail">
            <strong>{{ item.name }}</strong>
            <span class="menu-meta">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
            <span class="menu-price">{{ cents(item.price_cents) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { fallbackStores, fallbackTables, fallbackMenuItems } from '@/utils/fallback'
import PhotoUpload from '@/components/PhotoUpload.vue'
import type { Store, DiningTable, MenuItem } from '@/types'

const stores = ref<Store[]>([])
const allTables = ref<DiningTable[]>([])
const allMenus = ref<MenuItem[]>([])

function getTables(storeId: number) {
  return allTables.value.filter(t => t.store_id === storeId)
}

function getMenus(storeId: number) {
  return allMenus.value.filter(m => m.store_id === storeId)
}

async function updateMenuPhoto(id: number, photoUrl: string) {
  try {
    await api.patch(`/menu-items/${id}/photo`, { photoUrl })
    const item = allMenus.value.find(m => m.id === id)
    if (item) item.photo_url = photoUrl
    ElMessage.success('菜品图片已更新')
  } catch (e) { ElMessage.error((e as Error).message) }
}

onMounted(async () => {
  try {
    const [s, t, m] = await Promise.all([
      api.get<Store[]>('/stores'),
      api.get<DiningTable[]>('/tables'),
      api.get<MenuItem[]>('/menu-items'),
    ])
    stores.value = s; allTables.value = t; allMenus.value = m
  } catch {
    stores.value = fallbackStores; allTables.value = fallbackTables; allMenus.value = fallbackMenuItems
  }
})
</script>

<style scoped>
.store-section { background: #fff; padding: 24px; border-radius: 14px; border: 1px solid #e8e5df; margin-bottom: 20px; }
.store-section h3 { margin-bottom: 8px; }
.store-section p { color: #667085; font-size: 14px; }
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; }
.table-card { padding: 14px; background: #f8f9fa; border-radius: 10px; text-align: center; display: flex; flex-direction: column; gap: 4px; }
.menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.menu-card { display: flex; gap: 14px; padding: 14px; background: #f8f9fa; border-radius: 12px; align-items: center; }
.menu-photo-area { flex-shrink: 0; }
.menu-detail { display: flex; flex-direction: column; gap: 4px; }
.menu-detail strong { font-size: 15px; color: #172033; }
.menu-meta { font-size: 12px; color: #667085; }
.menu-price { font-size: 16px; font-weight: 700; color: #e86f51; }
</style>
