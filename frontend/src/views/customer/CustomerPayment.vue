<template>
  <div class="payment-page page-enter-active">
    <div class="payment-card">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div class="skeleton" style="height: 28px; width: 50%; margin-bottom: var(--space-sm)"></div>
        <div class="skeleton" style="height: 14px; width: 70%; margin-bottom: var(--space-lg)"></div>
        <div class="skeleton" style="height: 120px; border-radius: var(--radius-md); margin-bottom: var(--space-lg)"></div>
        <div class="skeleton" style="height: 44px; border-radius: var(--radius-md)"></div>
      </template>
      <!-- 无数据 -->
      <template v-else-if="!cart.reservationId && !paid">
        <el-empty description="无待支付订单">
          <el-button type="primary" @click="$router.push('/customer/order')">去点单</el-button>
        </el-empty>
      </template>
      <!-- 支付中 -->
      <template v-else-if="!paid">
        <h2>确认支付</h2>
        <p class="subtitle">沙箱支付环境，不会产生真实扣款</p>
        <div class="order-detail">
          <div class="detail-row"><span>预约编号</span><strong>#{{ cart.reservationId }}</strong></div>
          <div v-for="item in cart.selectedOrderItems" :key="item.menuItemId" class="detail-row">
            <span>{{ item.name }} × {{ item.quantity }}</span>
            <span>{{ cents(item.subtotal) }}</span>
          </div>
          <div class="detail-total">
            <span>应付金额</span>
            <strong>{{ cents(cart.orderTotal) }}</strong>
          </div>
        </div>
        <el-button type="primary" size="large" class="pay-btn" :loading="paying" @click="handlePay">
          沙箱支付 {{ cents(cart.orderTotal) }}
        </el-button>
        <el-button class="back-btn" @click="$router.push('/customer/order')">返回修改</el-button>
      </template>
      <!-- 支付成功 -->
      <template v-else>
        <div class="pay-success">
          <div class="success-icon">✓</div>
          <h2>支付成功</h2>
          <p>订单号：#{{ orderId }}</p>
          <p>支付金额：{{ cents(orderTotal) }}</p>
          <p class="sandbox-note">（沙箱环境，非真实交易）</p>
          <el-button type="primary" class="success-btn" @click="$router.push('/customer/profile')">查看订单</el-button>
          <el-button class="success-back-btn" @click="$router.push('/customer')">返回首页</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'

const cart = useCartStore()
const loading = ref(true)
const paying = ref(false)
const paid = ref(false)
const orderId = ref(0)
const orderTotal = ref(0)

async function handlePay() {
  if (!cart.reservationId || !cart.selectedOrderItems.length) {
    ElMessage.error('无待支付订单'); return
  }
  paying.value = true
  try {
    const items = cart.selectedOrderItems.map(i => ({ menuItemId: i.menuItemId, quantity: i.quantity }))
    const order = await api.post<{ id: number; total_cents: number }>('/orders', { reservationId: cart.reservationId, items })
    orderId.value = order.id
    orderTotal.value = order.total_cents
    paid.value = true
    cart.clearCart()
    cart.setReservationId(null)
    ElMessage.success('支付成功（沙箱）')
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { paying.value = false }
}

onMounted(() => { loading.value = false })
</script>

<style scoped>
.payment-page { max-width: 480px; margin: var(--space-2xl) auto; }
.payment-card { background: var(--paper); padding: var(--space-xl); border-radius: var(--radius-lg); border: 1px solid var(--line); box-shadow: var(--shadow-md); }
.payment-card h2 { margin-bottom: var(--space-xs); }
.subtitle { color: var(--muted); font-size: var(--text-sm); margin-bottom: var(--space-lg); }
.order-detail { background: var(--wash); border-radius: var(--radius-md); padding: var(--space-base); }
.detail-row { display: flex; justify-content: space-between; padding: var(--space-sm) 0; font-size: var(--text-sm); }
.detail-total { display: flex; justify-content: space-between; padding-top: var(--space-md); margin-top: var(--space-sm); border-top: 2px solid var(--teal); font-size: var(--text-lg); }
.detail-total strong { color: var(--coral); }
.pay-success { text-align: center; }
.success-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--teal); color: #fff; font-size: 32px; display: grid; place-items: center; margin: 0 auto var(--space-base); }
.sandbox-note { color: var(--muted); font-size: var(--text-sm); margin-top: var(--space-sm); }
.pay-btn { width: 100%; margin-top: var(--space-lg); }
.back-btn { width: 100%; margin-top: var(--space-sm); }
.success-btn { margin-top: var(--space-lg); }
.success-back-btn { margin-top: var(--space-sm); }
</style>
