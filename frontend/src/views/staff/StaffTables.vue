<template>
  <div>
    <h2 style="margin-bottom: 20px">桌位状态</h2>
    <div class="table-grid">
      <div v-for="table in tables" :key="table.id" class="table-card" :class="{ 'cat-zone': table.cat_zone }">
        <div class="table-code">{{ table.code }}</div>
        <div class="table-info">{{ table.seats }}人 · {{ table.area }}</div>
        <div class="table-queue">
          <span v-if="table._resCount > 0" class="queue-badge">{{ table._resCount }} 个预约</span>
          <span v-else class="queue-free">空闲</span>
        </div>
        <el-tag v-if="table.cat_zone" size="small" type="success" style="margin-top: 8px">猫区</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/http'
import { fallbackTables } from '@/utils/fallback'
import type { DiningTable, Reservation } from '@/types'

interface TableWithCount extends DiningTable { _resCount: number }

const tables = ref<TableWithCount[]>([])

onMounted(async () => {
  try {
    const [t, r] = await Promise.all([
      api.get<DiningTable[]>('/tables', { storeId: 1 }),
      api.get<Reservation[]>('/reservations', { storeId: 1 }),
    ])
    const today = new Date().toISOString().slice(0, 10)
    const todayRes = r.filter(res => res.reservation_date === today && ['booked', 'seated', 'dining'].includes(res.status))
    tables.value = t.map(table => ({
      ...table,
      _resCount: todayRes.filter(res => res.table_id === table.id).length,
    }))
  } catch {
    tables.value = fallbackTables.map(t => ({ ...t, _resCount: 0 }))
  }
})
</script>

<style scoped>
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; }
.table-card { background: #fff; padding: 20px; border-radius: 12px; border: 1px solid #e8e5df; text-align: center; }
.table-card.cat-zone { border-color: #86efac; background: #f0fdf4; }
.table-code { font-size: 24px; font-weight: 700; margin-bottom: 4px; }
.table-info { font-size: 13px; color: #667085; }
.table-queue { margin-top: 8px; }
.queue-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; background: #fef3c7; color: #92400e; }
.queue-free { font-size: 12px; color: #0f766e; }
</style>
