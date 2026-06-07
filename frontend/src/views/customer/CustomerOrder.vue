<template>
  <div>
    <h2 style="margin-bottom: 20px">在线点单</h2>
    <div v-if="memberDiscount < 1.0" class="discount-banner">
      当前会员折扣：{{ memberDiscountLabel }}，价格已自动应用折扣
    </div>
    <div class="order-layout">
      <div class="order-menu">
        <div v-for="item in cart.menuItems" :key="item.id" class="menu-item">
          <div style="display: flex; align-items: center; gap: 12px">
            <img v-if="item.photo_url" :src="item.photo_url" class="menu-photo" />
            <div>
              <strong>{{ item.name }}</strong>
              <span class="desc">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
            </div>
          </div>
          <div class="item-right">
            <!-- 有折扣时显示原价（划线）和折扣价 -->
            <div class="price-wrapper">
              <template v-if="memberDiscount < 1.0">
                <span class="original-price">{{ cents(item.price_cents) }}</span>
                <span class="discounted-price">{{ cents(getDiscountedPrice(item.price_cents)) }}</span>
              </template>
              <template v-else>
                <span class="price">{{ cents(item.price_cents) }}</span>
              </template>
            </div>
            <el-input-number :model-value="cart.selectedMenu[item.id] || 0" @update:model-value="cart.setQuantity(item.id, $event)" :min="0" :max="9" size="small" />
          </div>
        </div>
      </div>
      <div class="order-cart">
        <h3>购物车</h3>
        <div v-if="cart.selectedOrderItems.length === 0" class="empty">请从左侧选择菜品</div>
        <div v-else>
          <!-- 原价和折扣价对比 -->
          <div v-if="memberDiscount < 1.0" class="price-comparison">
            <div class="price-row">
              <span>原价合计</span>
              <span class="original">{{ cents(cart.orderTotal) }}</span>
            </div>
            <div class="price-row discount">
              <span>{{ memberDiscountLabel }}优惠</span>
              <span class="discount-amount">- {{ cents(cart.orderTotal - discountedTotal) }}</span>
            </div>
          </div>
          <div v-for="item in cart.selectedOrderItems" :key="item.menuItemId" class="cart-item">
            <span>{{ item.name }} × {{ item.quantity }}</span>
            <span>{{ cents(item.subtotal) }}</span>
          </div>
          <div class="cart-total">
            <span>实付金额</span>
            <span>{{ cents(memberDiscount < 1.0 ? discountedTotal : cart.orderTotal) }}</span>
          </div>
          <el-button type="primary" style="width: 100%; margin-top: 12px" :disabled="!cart.reservationId" @click="handleOrder">
            确认下单
          </el-button>
          <p v-if="!cart.reservationId" class="hint">请先创建预约后再下单</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import type { MenuItem } from '@/types'

const cart = useCartStore()
const router = useRouter()
const auth = useAuthStore()
const memberDiscount = ref<number>(1.0)
const memberDiscountLabel = computed(() => {
  if (memberDiscount.value === 0.85) return '8.5折'
  if (memberDiscount.value === 0.9) return '9折'
  if (memberDiscount.value === 0.95) return '9.5折'
  return ''
})

// 折后价合计（前端估算，实际以后端为准）
const discountedTotal = computed(() => Math.round(cart.orderTotal * memberDiscount.value))

// 获取会员折扣率
onMounted(async () => {
  if (auth.isAuthenticated) {
    try {
      const info = await api.get<{ discount: number }>('/users/me/member')
      memberDiscount.value = info.discount || 1.0
    } catch {
      memberDiscount.value = 1.0
    }
  }
  if (!cart.menuItems.length) {
    try { cart.menuItems = await api.get<MenuItem[]>('/menu-items', { storeId: 1 }) }
    catch { /* keep empty */ }
  }
})

// 计算折扣价
function getDiscountedPrice(priceCents: number): number {
  return Math.round(priceCents * memberDiscount.value)
}

function handleOrder() {
  if (!cart.reservationId) {
    ElMessage.warning('请先创建预约')
    return
  }
  if (!cart.selectedOrderItems.length) {
    ElMessage.warning('请选择菜品')
    return
  }
  router.push('/payment')
}
</script>

<style scoped>
.order-layout { display: grid; grid-template-columns: 1fr 320px; gap: 24px; }
@media (max-width: 768px) { .order-layout { grid-template-columns: 1fr; } }
.order-menu { display: flex; flex-direction: column; gap: 10px; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.menu-photo { width: 56px; height: 56px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.desc { display: block; font-size: 13px; color: #667085; margin-top: 2px; }
.item-right { display: flex; align-items: center; gap: 12px; }
.price-wrapper { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.original-price { font-size: 12px; color: #999; text-decoration: line-through; }
.discounted-price { font-weight: 700; color: #e86f51; font-size: 15px; }
.price { font-weight: 700; color: #e86f51; }
.discount-banner { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 10px 16px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; text-align: center; }
.order-cart { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; position: sticky; top: 84px; }
.order-cart h3 { margin-bottom: 16px; }
.empty { color: #667085; text-align: center; padding: 20px 0; }
.cart-item { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
.price-comparison { margin-bottom: 12px; padding: 12px; background: #fff8f0; border-radius: 8px; }
.price-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.original { color: #999; }
.discount { color: #e6a23c; }
.discount-amount { color: #e6a23c; font-weight: 700; }
.cart-total { display: flex; justify-content: space-between; padding-top: 12px; margin-top: 8px; border-top: 2px solid #0f766e; font-weight: 700; font-size: 16px; }
.hint { font-size: 12px; color: #667085; text-align: center; margin-top: 8px; }
</style>
