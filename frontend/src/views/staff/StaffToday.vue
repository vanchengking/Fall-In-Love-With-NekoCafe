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
    <div class="today-sections">
      <section class="panel">
        <div class="panel-header">
          <h3>今日预约</h3>
          <span>{{ todayReservations.length }} 条</span>
        </div>
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" @change="loadReservations">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="created">待确认</el-radio-button>
        <el-radio-button value="booked">已预约</el-radio-button>
        <el-radio-button value="seated">已入座</el-radio-button>
        <el-radio-button value="dining">用餐中</el-radio-button>
        <el-radio-button value="finished">已完成</el-radio-button>
        <el-radio-button value="cancelled">已取消</el-radio-button>
        <el-radio-button value="no_show">未到店</el-radio-button>
      </el-radio-group>
    </div>
    <div class="res-list">
      <div v-for="r in todayReservations" :key="r.id" class="res-card">
        <div class="res-info">
          <el-tag :type="statusType(r.status)" size="small">{{ r.status_label || statusLabel(r.status) }}</el-tag>
          <h4>{{ r.customer_name || r.customerName }}</h4>
          <p>{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</p>
        </div>
        <div class="res-actions">
          <el-button size="small" type="success" :disabled="r.status !== 'booked'" @click="updateStatus(r, 'seated')">入座</el-button>
          <el-button size="small" type="warning" :disabled="r.status !== 'seated'" @click="updateStatus(r, 'dining')">开始用餐</el-button>
          <el-button size="small" :disabled="r.status !== 'dining'" @click="updateStatus(r, 'finished')">完桌</el-button>
          <el-button size="small" :disabled="r.status !== 'booked'" @click="updateStatus(r, 'no_show')">标记未到</el-button>
          <el-button size="small" type="danger" plain :disabled="r.status !== 'created' && r.status !== 'booked'" @click="updateStatus(r, 'cancelled')">取消</el-button>
        </div>
      </div>
      <el-empty v-if="!todayReservations.length" description="今日暂无预约" />
    </div>
      </section>
      <section class="panel">
        <div class="panel-header">
          <h3>待处理订单</h3>
          <span>{{ pendingOrders.length }} 条</span>
        </div>
        <div class="order-list">
          <div v-for="order in pendingOrders" :key="order.id" class="order-card">
            <div>
              <div class="order-title">
                <strong>订单 #{{ order.id }}</strong>
                <el-tag type="warning" size="small">{{ order.payment_status }}</el-tag>
              </div>
              <p>{{ order.customer_name || '顾客' }} · 预约 #{{ order.reservation_id }}</p>
              <p class="order-meta">
                {{ order.reservation_date || today }} {{ order.reservation_time || '' }} · {{ cents(order.total_cents) }}
              </p>
            </div>
          </div>
          <el-empty v-if="!pendingOrders.length" description="暂无待处理订单" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { cents, statusLabel, statusType } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import type { Order, Reservation } from '@/types'

const reservations = ref<Reservation[]>([])
const orders = ref<Order[]>([])
const statusFilter = ref('')
const today = new Date().toISOString().slice(0, 10)

const todayReservations = computed(() =>
  reservations.value.filter(r => r.reservation_date === today),
)

const pendingOrders = computed(() =>
  orders.value.filter(order =>
    order.status === 'paid'
    && order.payment_status !== 'refunded'
    && (!order.reservation_date || order.reservation_date === today),
  ),
)

const stats = computed(() => ({
  total: todayReservations.value.length,
  seated: todayReservations.value.filter(r => r.status === 'seated').length,
  finished: todayReservations.value.filter(r => r.status === 'finished').length,
  pending: todayReservations.value.filter(r => r.status === 'created' || r.status === 'booked').length
    + pendingOrders.value.length,
}))

async function loadReservations() {
  const params: Record<string, unknown> = {}
  if (statusFilter.value) params.status = statusFilter.value
  try { reservations.value = await api.get<Reservation[]>('/reservations', params) }
  catch { /* keep previous data */ }
}

async function loadOrders() {
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { /* keep previous data */ }
}

async function refreshAll() {
  await Promise.all([loadReservations(), loadOrders()])
}

async function updateStatus(r: Reservation, status: string) {
  try {
    const updated = await api.patch<Reservation>(`/reservations/${r.id}/status`, { status })
    reservations.value = reservations.value.map(x => x.id === updated.id ? updated : x)
    ElMessage.success(`预约 #${updated.id} → ${statusLabel(status)}`)
  } catch (e) { ElMessage.error((e as Error).message) }
}

const { lastUpdated } = usePolling(refreshAll, 10000)
</script>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; }
.stat-card span { display: block; font-size: 13px; color: #667085; margin-bottom: 4px; }
.stat-card strong { font-size: 24px; color: #172033; }
.today-sections { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; }
.panel { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.panel-header h3 { margin: 0; font-size: 16px; color: #172033; }
.filter-bar { margin-bottom: 16px; }
.res-list { display: flex; flex-direction: column; gap: 10px; }
.res-card { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.res-info h4 { margin: 6px 0 2px; color: #172033; }
.res-info p { font-size: 13px; color: #667085; }
.res-actions { display: flex; gap: 6px; }
.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-card { padding: 14px; border-radius: 12px; border: 1px solid #e8e5df; background: #fcfcfd; }
.order-title { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 6px; }
.order-card p { margin: 4px 0 0; font-size: 13px; color: #667085; }
.order-meta { color: #475467; }
@media (max-width: 960px) {
  .today-sections { grid-template-columns: 1fr; }
}
</style>
