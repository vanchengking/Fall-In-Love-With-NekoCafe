<template>
  <div class="page-enter-active">
    <h2>在线点单</h2>
    <!-- 骨架屏 -->
    <div v-if="loading" class="order-layout">
      <div class="order-menu">
        <div v-for="i in 5" :key="i" class="menu-item">
          <div style="display: flex; align-items: center; gap: var(--space-sm)">
            <div class="skeleton" style="width: 56px; height: 56px; border-radius: var(--radius-md); flex-shrink: 0"></div>
            <div>
              <div class="skeleton" style="height: 16px; width: 100px; margin-bottom: var(--space-xs)"></div>
              <div class="skeleton" style="height: 13px; width: 140px"></div>
            </div>
          </div>
          <div class="skeleton" style="height: 16px; width: 60px"></div>
        </div>
      </div>
      <div class="order-cart">
        <div class="skeleton" style="height: 20px; width: 80px; margin-bottom: var(--space-base)"></div>
        <div class="skeleton" style="height: 60px; border-radius: var(--radius-md)"></div>
      </div>
    </div>
    <!-- 无数据 -->
    <div v-else-if="!cart.menuItems.length">
      <el-empty description="暂无可点菜品" />
    </div>
    <!-- 正常内容 -->
    <div v-else class="order-layout">
      <div class="order-menu">
        <div v-for="item in cart.menuItems" :key="item.id" class="menu-item">
          <div style="display: flex; align-items: center; gap: var(--space-sm)">
            <img v-if="item.photo_url" :src="item.photo_url" :alt="item.name" class="menu-photo" />
            <div v-else class="menu-photo-placeholder">🍽️</div>
            <div>
              <strong>{{ item.name }}</strong>
              <span class="desc">{{ tagLabels([item.category]) }} · {{ tagLabels(item.tags) }}</span>
            </div>
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
          <el-button type="primary" class="order-btn" :disabled="!cart.reservationId" :loading="ordering" @click="handleOrder">
            确认下单
          </el-button>
          <p v-if="!cart.reservationId" class="hint">请先创建预约后再下单</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { api } from '@/utils/http'
import { cents, tagLabels } from '@/utils/format'
import type { MenuItem } from '@/types'

const cart = useCartStore()
const router = useRouter()
const loading = ref(true)
const ordering = ref(false)

function handleOrder() {
  if (!cart.reservationId) { ElMessage.warning('请先创建预约'); return }
  if (!cart.selectedOrderItems.length) { ElMessage.warning('请选择菜品'); return }
  ordering.value = true
  setTimeout(() => { router.push('/customer/payment'); ordering.value = false }, 300)
}

onMounted(async () => {
  if (!cart.menuItems.length) {
    try { cart.menuItems = await api.get<MenuItem[]>('/menu-items', { storeId: 1 }) }
    catch { /* keep empty */ }
  }
  loading.value = false
})
</script>

<style scoped>
.order-layout { display: grid; grid-template-columns: 1fr 320px; gap: var(--space-lg); }
@media (max-width: 768px) { .order-layout { grid-template-columns: 1fr; } }
.order-menu { display: flex; flex-direction: column; gap: var(--space-sm); }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: var(--space-base); background: var(--paper); border-radius: var(--radius-lg); border: 1px solid var(--line); transition: all var(--transition-fast); }
.menu-item:hover { border-color: var(--teal-light); box-shadow: var(--shadow-sm); }
.menu-photo { width: 56px; height: 56px; object-fit: cover; border-radius: var(--radius-sm); flex-shrink: 0; }
.menu-photo-placeholder { width: 56px; height: 56px; border-radius: var(--radius-sm); background: var(--wash); display: grid; place-items: center; font-size: 22px; flex-shrink: 0; }
.desc { display: block; font-size: var(--text-sm); color: var(--muted); margin-top: var(--space-xs); }
.item-right { display: flex; align-items: center; gap: var(--space-md); }
.price { font-weight: 700; color: var(--coral); }
.order-cart { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); position: sticky; top: 84px; }
.order-cart h3 { margin-bottom: var(--space-base); }
.empty { color: var(--muted); text-align: center; padding: var(--space-lg) 0; }
.cart-item { display: flex; justify-content: space-between; padding: var(--space-sm) 0; font-size: var(--text-sm); }
.cart-total { display: flex; justify-content: space-between; padding-top: var(--space-md); margin-top: var(--space-sm); border-top: 2px solid var(--teal); font-weight: 700; font-size: var(--text-base); }
.hint { font-size: var(--text-xs); color: var(--muted); text-align: center; margin-top: var(--space-sm); }
.order-btn { width: 100%; margin-top: var(--space-md); }
</style>
