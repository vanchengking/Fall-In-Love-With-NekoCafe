<template>
  <div class="payment-page">
    <div class="payment-card">
      <div v-if="!paid">
        <h2>确认支付</h2>
        <p class="subtitle">沙箱支付环境，不会产生真实扣款</p>
        <div class="order-detail">
          <div class="detail-row"><span>预约编号</span><strong>#{{ cart.reservationId }}</strong></div>
          <div v-for="item in cart.selectedOrderItems" :key="item.menuItemId" class="detail-row">
            <span>{{ item.name }} × {{ item.quantity }}</span>
            <span>{{ cents(item.subtotal) }}</span>
          </div>
          
          <!-- 原价和折扣价对比 -->
          <div v-if="orderInfo.original_total_cents && orderInfo.original_total_cents !== orderInfo.total_cents" class="price-comparison">
            <div class="detail-row original-price">
              <span>原价</span>
              <span class="strikethrough">{{ cents(orderInfo.original_total_cents) }}</span>
            </div>
            <div class="detail-row discount">
              <span>折扣优惠（{{ (orderInfo.discount_rate * 10).toFixed(1) }}折）</span>
              <span class="discount-amount">- {{ cents(orderInfo.original_total_cents - orderInfo.total_cents) }}</span>
            </div>
          </div>
          
          <div class="detail-total">
            <span>应付金额</span>
            <strong>{{ cents(cart.orderTotal) }}</strong>
          </div>
        </div>
        <el-button type="primary" size="large" style="width: 100%; margin-top: 20px" :loading="loading" @click="handlePay">
          沙箱支付 {{ cents(cart.orderTotal) }}
        </el-button>
        <el-button style="width: 100%; margin-top: 8px" @click="$router.push('/order')">返回修改</el-button>
      </div>

      <div v-else class="pay-success">
        <div class="success-icon">✓</div>
        <h2>支付成功</h2>
        <p>订单号：#{{ orderId }}</p>
        <p>支付金额：{{ cents(orderTotal) }}</p>
        <p v-if="orderInfo.discount_rate && orderInfo.discount_rate < 1" class="discount-applied">
          已应用会员折扣：{{ (orderInfo.discount_rate * 10).toFixed(1) }}折
        </p>
        <p class="sandbox-note">（沙箱环境，非真实交易）</p>
        <el-button type="primary" style="margin-top: 20px" @click="$router.push('/profile')">查看订单</el-button>
        <el-button style="margin-top: 8px" @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'

const cart = useCartStore()
const loading = ref(false)
const paid = ref(false)
const orderId = ref(0)
const orderTotal = ref(0)
const orderInfo = ref<any>({})

async function handlePay() {
  if (!cart.reservationId || !cart.selectedOrderItems.length) {
    ElMessage.error('无待支付订单')
    return
  }
  loading.value = true
  try {
    const items = cart.selectedOrderItems.map(i => ({ menuItemId: i.menuItemId, quantity: i.quantity }))
    const order = await api.post<{ 
      id: number; 
      total_cents: number;
      original_total_cents: number;
      discount_rate: number;
    }>('/orders', { reservationId: cart.reservationId, items })
    orderId.value = order.id
    orderTotal.value = order.total_cents
    orderInfo.value = order  // 保存订单信息（包含原价和折扣率）
    paid.value = true
    cart.clearCart()
    cart.setReservationId(null)
    ElMessage.success('支付成功（沙箱）')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.payment-page { max-width: 480px; margin: 40px auto; }
.payment-card { background: #fff; padding: 32px; border-radius: 16px; border: 1px solid #e8e5df; box-shadow: 0 8px 24px rgba(0,0,0,0.06); }
.payment-card h2 { margin-bottom: 4px; }
.subtitle { color: #667085; font-size: 14px; margin-bottom: 24px; }
.order-detail { background: #f8f9fa; border-radius: 10px; padding: 16px; }
.detail-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
.detail-total { display: flex; justify-content: space-between; padding-top: 12px; margin-top: 8px; border-top: 2px solid #0f766e; font-size: 18px; }
.detail-total strong { color: #e86f51; }

/* 原价和折扣价对比样式 */
.price-comparison { margin-top: 12px; padding: 12px; background: #fff; border-radius: 8px; border: 1px solid #e8e5df; }
.original-price { color: #999; }
.strikethrough { text-decoration: line-through; color: #999; }
.discount { color: #e6a23c; }
.discount-amount { color: #e6a23c; font-weight: 700; }
.discount-applied { color: #0f766e; font-size: 14px; margin-top: 8px; }

.pay-success { text-align: center; }
.success-icon { width: 64px; height: 64px; border-radius: 50%; background: #0f766e; color: #fff; font-size: 32px; display: grid; place-items: center; margin: 0 auto 16px; }
.sandbox-note { color: #667085; font-size: 13px; margin-top: 8px; }
</style>
