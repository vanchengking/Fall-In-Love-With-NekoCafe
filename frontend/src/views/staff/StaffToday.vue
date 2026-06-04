<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
      <h2 style="margin: 0">今日预约</h2>
      <span v-if="lastUpdated" style="font-size: 12px; color: #667085">
        {{ lastUpdated.toLocaleTimeString() }} 自动刷新中
      </span>
    </div>
    <div class="stat-cards">
      <div class="stat-card"><span>今日预约</span><strong>{{ stats.total }}</strong></div>
      <div class="stat-card"><span>已入座</span><strong>{{ stats.seated }}</strong></div>
      <div class="stat-card"><span>已完成</span><strong>{{ stats.finished }}</strong></div>
      <div class="stat-card"><span>待处理</span><strong>{{ stats.pending }}</strong></div>
    </div>
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" @change="loadReservations">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="booked">待确认</el-radio-button>
        <el-radio-button value="seated">已入座</el-radio-button>
        <el-radio-button value="finished">已完成</el-radio-button>
      </el-radio-group>
    </div>
    <div class="res-list">
      <div v-for="r in reservations" :key="r.id" class="res-card">
        <div class="res-info">
          <el-tag :type="statusType(r.status)" size="small">{{ statusLabel(r.status) }}</el-tag>
          <h4>{{ r.customer_name || r.customerName }}</h4>
          <p>{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</p>
        </div>
        <div class="res-actions">
          <el-button size="small" type="success" :disabled="r.status !== 'booked'" @click="updateStatus(r, 'seated')">入座</el-button>
          <el-button size="small" :disabled="r.status !== 'seated'" @click="updateStatus(r, 'finished')">完桌</el-button>
          <el-button size="small" type="danger" plain :disabled="r.status !== 'booked'" @click="updateStatus(r, 'cancelled')">取消</el-button>
        </div>
      </div>
      <el-empty v-if="!reservations.length" description="暂无预约" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { statusLabel, statusType } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import type { Reservation } from '@/types'

const reservations = ref<Reservation[]>([])
const statusFilter = ref('')

const stats = computed(() => ({
  total: reservations.value.length,
  seated: reservations.value.filter(r => r.status === 'seated').length,
  finished: reservations.value.filter(r => r.status === 'finished').length,
  pending: reservations.value.filter(r => r.status === 'booked').length,
}))

async function loadReservations() {
  const params: Record<string, unknown> = {}
  if (statusFilter.value) params.status = statusFilter.value
  try { reservations.value = await api.get<Reservation[]>('/reservations', params) }
  catch { /* keep previous data */ }
}

async function updateStatus(r: Reservation, status: string) {
  try {
    const updated = await api.patch<Reservation>(`/reservations/${r.id}/status`, { status })
    reservations.value = reservations.value.map(x => x.id === updated.id ? updated : x)
    ElMessage.success(`预约 #${updated.id} → ${statusLabel(status)}`)
  } catch (e) { ElMessage.error((e as Error).message) }
}

const { lastUpdated } = usePolling(loadReservations, 10000)
</script>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; }
.stat-card span { display: block; font-size: 13px; color: #667085; margin-bottom: 4px; }
.stat-card strong { font-size: 24px; color: #172033; }
.filter-bar { margin-bottom: 16px; }
.res-list { display: flex; flex-direction: column; gap: 10px; }
.res-card { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.res-info h4 { margin: 6px 0 2px; color: #172033; }
.res-info p { font-size: 13px; color: #667085; }
.res-actions { display: flex; gap: 6px; }
</style>
