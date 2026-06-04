<template>
  <div>
    <h2 style="margin-bottom: 20px">预约管理</h2>
    <el-form inline style="margin-bottom: 16px" @submit.prevent="search">
      <el-form-item><el-input v-model="searchForm.mobileNumber" placeholder="手机号" clearable /></el-form-item>
      <el-form-item><el-date-picker v-model="searchForm.date" type="date" value-format="YYYY-MM-DD" placeholder="日期" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>
    <div class="res-list">
      <div v-for="r in reservations" :key="r.id" class="res-card">
        <div>
          <el-tag :type="statusType(r.status)" size="small">{{ statusLabel(r.status) }}</el-tag>
          <strong style="margin-left: 8px">{{ r.customer_name || r.customerName }}</strong>
          <span class="meta">{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</span>
        </div>
        <div class="actions">
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { statusLabel, statusType } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import type { Reservation } from '@/types'

const reservations = ref<Reservation[]>([])
const searchForm = reactive({ mobileNumber: '', date: '' })

async function search() {
  const params: Record<string, unknown> = {}
  if (searchForm.mobileNumber) params.mobileNumber = searchForm.mobileNumber
  if (searchForm.date) params.date = searchForm.date
  try { reservations.value = await api.get<Reservation[]>('/reservations', params) }
  catch { reservations.value = [] }
}

async function updateStatus(r: Reservation, status: string) {
  try {
    const updated = await api.patch<Reservation>(`/reservations/${r.id}/status`, { status })
    reservations.value = reservations.value.map(x => x.id === updated.id ? updated : x)
    ElMessage.success(`预约 #${updated.id} → ${statusLabel(status)}`)
  } catch (e) { ElMessage.error((e as Error).message) }
}

usePolling(search, 10000)
</script>

<style scoped>
.res-list { display: flex; flex-direction: column; gap: 10px; }
.res-card { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.meta { display: block; font-size: 13px; color: #667085; margin-top: 4px; }
.actions { display: flex; gap: 6px; }
</style>
