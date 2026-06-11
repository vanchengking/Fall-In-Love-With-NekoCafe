<template>
  <div>
    <h2 style="margin-bottom: 20px">选择门店</h2>
    <div class="store-grid">
      <div v-for="store in stores" :key="store.id" class="store-card" @click="$router.push(`/stores/${store.id}`)">
        <h3>{{ store.name }}</h3>
        <p>{{ store.city }} · {{ store.address }}</p>
        <p class="store-meta">电话：{{ store.phone }}</p>
        <p v-if="store.table_count" class="store-meta">{{ store.table_count }} 桌 · {{ store.total_seats }} 座</p>
        <div style="display: flex; gap: 8px; margin-top: 12px" @click.stop>
          <el-button type="primary" plain size="small" @click="$router.push(`/stores/${store.id}`)">查看详情</el-button>
          <el-button size="small" @click="$router.push(`/stores/${store.id}/reviews`)">查看评价</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/http'
import { fallbackStores } from '@/utils/fallback'
import type { Store } from '@/types'

const stores = ref<Store[]>([])
onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
})
</script>

<style scoped>
.store-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.store-card {
  padding: 24px; background: #fff; border-radius: 12px;
  border: 1px solid #e8e5df; cursor: pointer;
  transition: box-shadow 0.2s;
}
.store-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.store-card h3 { font-size: 18px; margin-bottom: 8px; }
.store-card p { color: #667085; font-size: 14px; }
.store-meta { margin-top: 4px; }
</style>
