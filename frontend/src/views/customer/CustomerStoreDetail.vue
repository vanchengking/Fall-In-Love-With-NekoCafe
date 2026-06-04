<template>
  <div>
    <div class="detail-header">
      <h2>{{ storeName }}</h2>
      <el-button type="primary" @click="$router.push('/reservation')">预约此门店</el-button>
    </div>
    <el-tabs>
      <el-tab-pane label="猫咪档案">
        <div class="cat-grid">
          <div v-for="cat in cats" :key="cat.id" class="cat-card">
            <img v-if="cat.photo_url" :src="cat.photo_url" class="cat-photo" />
            <div v-else class="cat-avatar"><Cat :size="28" /></div>
            <h4>{{ cat.name }}</h4>
            <p>{{ cat.breed }} · {{ cat.health_status }}</p>
            <div class="tags">
              <el-tag v-for="tag in cat.personality_tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="桌位">
        <div class="table-grid">
          <div v-for="table in tables" :key="table.id" class="table-card">
            <strong>{{ table.code }}</strong>
            <span>{{ table.seats }} 人 · {{ table.area }}</span>
            <el-tag v-if="table.cat_zone" size="small" type="success">猫区</el-tag>
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
            <span class="price">{{ cents(item.price_cents) }}</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Cat } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { fallbackTables, fallbackCats, fallbackMenuItems, fallbackStores } from '@/utils/fallback'
import type { DiningTable, Cat as CatType, MenuItem, Store } from '@/types'

const route = useRoute()
const storeId = Number(route.params.id)
const storeName = ref('')
const tables = ref<DiningTable[]>([])
const cats = ref<CatType[]>([])
const menuItems = ref<MenuItem[]>([])

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
})
</script>

<style scoped>
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.cat-card { padding: 20px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; text-align: center; }
.cat-photo { width: 100%; height: 100px; object-fit: cover; border-radius: 8px; margin-bottom: 8px; }
.cat-avatar { color: #0f766e; margin-bottom: 8px; }
.cat-card h4 { margin-bottom: 4px; }
.cat-card p { color: #667085; font-size: 13px; margin-bottom: 8px; }
.tags { display: flex; gap: 4px; justify-content: center; flex-wrap: wrap; }
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; }
.table-card { padding: 16px; background: #fff; border-radius: 10px; border: 1px solid #e8e5df; text-align: center; display: flex; flex-direction: column; gap: 4px; }
.menu-list { display: flex; flex-direction: column; gap: 10px; }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 10px; border: 1px solid #e8e5df; }
.menu-photo { width: 56px; height: 56px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.desc { display: block; font-size: 13px; color: #667085; margin-top: 2px; }
.price { font-weight: 700; color: #e86f51; }
</style>
