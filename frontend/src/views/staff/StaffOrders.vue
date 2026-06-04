<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
      <h2 style="margin: 0">订单管理</h2>
      <span v-if="lastUpdated" style="font-size: 12px; color: #667085">
        {{ lastUpdated.toLocaleTimeString() }} 自动刷新中
      </span>
    </div>
    <div class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-card">
        <div class="order-left">
          <el-tag :type="o.status === 'paid' ? 'success' : 'info'" size="small">{{ o.status }}</el-tag>
          <h4>订单 #{{ o.id }}</h4>
          <p>预约 #{{ o.reservation_id }} · {{ cents(o.total_cents) }}</p>
          <p class="meta">{{ o.payment_status }}</p>
        </div>
      </div>
      <el-empty v-if="!orders.length" description="暂无订单" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import type { Order } from '@/types'

const orders = ref<Order[]>([])

async function loadOrders() {
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { /* keep previous */ }
}

const { lastUpdated } = usePolling(loadOrders, 10000)
</script>

<style scoped>
.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-card { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.order-left h4 { margin: 6px 0 2px; color: #172033; }
.order-left p { font-size: 13px; color: #667085; }
.meta { font-size: 12px; }
</style>
