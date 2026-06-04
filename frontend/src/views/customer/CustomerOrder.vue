<template>
  <div>
    <h2 style="margin-bottom: 20px">在线点单</h2>
    <div class="order-layout">
      <div class="order-menu">
        <div v-for="item in cart.menuItems" :key="item.id" class="menu-item">
          <div>
            <strong>{{ item.name }}</strong>
            <span class="desc">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
          </div>
          <div class="item-right">
            <span class="price">{{ cents(item.price_cents) }}</span>
            <el-input-number :model-value="cart.selectedMenu[item.id] || 0" @update:model-value="cart.setQuantity(item.id, $event)" :min="0" :max="9" size="small" />
          </div>
        </div>
      </div>
      <div class="order-cart">
        <h3>购物车</h3>
        <div v-if="cart.selectedOrderItems.length === 0" class="empty">请从左侧选择菜品</div>
        <div v-else>
          <div v-for="item in cart.selectedOrderItems" :key="item.menuItemId" class="cart-item">
            <span>{{ item.name }} × {{ item.quantity }}</span>
            <span>{{ cents(item.subtotal) }}</span>
          </div>
          <div class="cart-total">
            <span>合计</span>
            <span>{{ cents(cart.orderTotal) }}</span>
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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import type { MenuItem } from '@/types'

const cart = useCartStore()
const router = useRouter()

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

onMounted(async () => {
  if (!cart.menuItems.length) {
    try { cart.menuItems = await api.get<MenuItem[]>('/menu-items', { storeId: 1 }) }
    catch { /* keep empty */ }
  }
})
</script>

<style scoped>
.order-layout { display: grid; grid-template-columns: 1fr 320px; gap: 24px; }
@media (max-width: 768px) { .order-layout { grid-template-columns: 1fr; } }
.order-menu { display: flex; flex-direction: column; gap: 10px; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #fff; border-radius: 10px; border: 1px solid #e8e5df; }
.desc { display: block; font-size: 13px; color: #667085; margin-top: 2px; }
.item-right { display: flex; align-items: center; gap: 12px; }
.price { font-weight: 700; color: #e86f51; }
.order-cart { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; position: sticky; top: 84px; }
.order-cart h3 { margin-bottom: 16px; }
.empty { color: #667085; text-align: center; padding: 20px 0; }
.cart-item { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
.cart-total { display: flex; justify-content: space-between; padding-top: 12px; margin-top: 8px; border-top: 2px solid #0f766e; font-weight: 700; font-size: 16px; }
.hint { font-size: 12px; color: #667085; text-align: center; margin-top: 8px; }
</style>
