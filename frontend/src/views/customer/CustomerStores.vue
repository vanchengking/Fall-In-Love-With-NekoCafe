<template>
  <div>
    <div class="stores-header">
      <div>
        <h2 style="margin-bottom: 4px">选择门店</h2>
        <p class="stores-summary">全国 {{ cityCount }} 个城市 · {{ stores.length }} 家门店</p>
      </div>
      <div class="stores-filters">
        <el-input
          v-model="keyword"
          placeholder="搜索门店名称 / 地址"
          clearable
          style="width: 220px"
        />
        <el-select v-model="selectedCity" placeholder="全部城市" clearable style="width: 140px">
          <el-option v-for="city in cities" :key="city" :label="`${city}（${cityStoreCount[city]}）`" :value="city" />
        </el-select>
      </div>
    </div>

    <div class="city-chips">
      <span class="city-chip" :class="{ active: !selectedCity }" @click="selectedCity = ''">全部</span>
      <span
        v-for="city in cities"
        :key="city"
        class="city-chip"
        :class="{ active: selectedCity === city }"
        @click="selectedCity = selectedCity === city ? '' : city"
      >{{ city }}</span>
    </div>

    <el-empty v-if="filteredStores.length === 0" description="没有符合条件的门店，换个城市或关键词试试" />
    <div v-else class="store-grid">
      <div v-for="store in filteredStores" :key="store.id" class="store-card" @click="$router.push(`/stores/${store.id}`)">
        <div class="store-title">
          <h3>{{ store.name }}</h3>
          <span class="store-city-tag">{{ store.city }}</span>
        </div>
        <p class="store-meta">{{ store.address }}</p>
        <p class="store-meta">电话：{{ store.phone }}</p>
        <p class="store-meta">营业时间：{{ businessHours(store) }}</p>
        <p v-if="store.table_count" class="store-meta store-stat">{{ store.table_count }} 桌 · {{ store.total_seats }} 座</p>
        <div style="display: flex; gap: 8px; margin-top: 12px" @click.stop>
          <el-button type="primary" plain size="small" @click="$router.push(`/stores/${store.id}`)">查看详情</el-button>
          <el-button size="small" @click="$router.push(`/stores/${store.id}/reviews`)">查看评价</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '@/utils/http'
import { businessHours } from '@/utils/format'
import { fallbackStores } from '@/utils/fallback'
import type { Store } from '@/types'

const stores = ref<Store[]>([])
const selectedCity = ref('')
const keyword = ref('')

const cities = computed(() => [...new Set(stores.value.map(s => s.city))])
const cityCount = computed(() => cities.value.length)
const cityStoreCount = computed(() => {
  const counts: Record<string, number> = {}
  for (const store of stores.value) counts[store.city] = (counts[store.city] || 0) + 1
  return counts
})

const filteredStores = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return stores.value.filter(store => {
    if (selectedCity.value && store.city !== selectedCity.value) return false
    if (kw && !`${store.name}${store.address}`.toLowerCase().includes(kw)) return false
    return true
  })
})

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
})
</script>

<style scoped>
.stores-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; margin-bottom: 14px; }
.stores-summary { color: #667085; font-size: 13px; }
.stores-filters { display: flex; gap: 10px; flex-wrap: wrap; }

.city-chips { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; }
.city-chip {
  padding: 4px 14px; border-radius: 16px; font-size: 13px; cursor: pointer;
  background: #fff; color: #667085; border: 1px solid #e8e5df; transition: all 0.15s;
}
.city-chip:hover { border-color: #0f766e; color: #0f766e; }
.city-chip.active { background: #0f766e; border-color: #0f766e; color: #fff; }

.store-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.store-card {
  padding: 24px; background: #fff; border-radius: 12px;
  border: 1px solid #e8e5df; cursor: pointer;
  transition: box-shadow 0.2s;
}
.store-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.store-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.store-card h3 { font-size: 18px; margin: 0; }
.store-city-tag {
  flex-shrink: 0; font-size: 12px; color: #0f766e; background: #e8f6f1;
  padding: 2px 10px; border-radius: 10px;
}
.store-card p { color: #667085; font-size: 14px; }
.store-meta { margin-top: 4px; }
.store-stat { color: #8c6d58; }
</style>
