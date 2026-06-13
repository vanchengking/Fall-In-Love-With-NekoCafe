<template>
  <header class="customer-navbar">
    <div class="navbar-inner">
      <router-link to="/customer" class="navbar-brand">
        <div class="brand-icon">N</div>
        <span>NekoCafe</span>
      </router-link>

      <nav class="navbar-links">
        <router-link to="/customer" class="nav-link" :class="{ active: $route.path === '/customer' }">首页</router-link>
        <router-link to="/customer/stores" class="nav-link" :class="{ active: $route.path.startsWith('/customer/stores') }">门店</router-link>
        <router-link to="/customer/reservation" class="nav-link" :class="{ active: $route.path === '/customer/reservation' }">预约</router-link>
        <router-link to="/customer/order" class="nav-link" :class="{ active: $route.path === '/customer/order' }">点单</router-link>
        <router-link to="/customer/reviews" class="nav-link" :class="{ active: $route.path === '/customer/reviews' }">评价</router-link>
      </nav>

      <div class="navbar-right">
        <router-link to="/customer/order" class="cart-btn">
          <ShoppingCart :size="20" />
          <span v-if="cart.orderCount > 0" class="cart-badge">{{ cart.orderCount }}</span>
        </router-link>
        <el-dropdown trigger="click">
          <div class="user-btn">
            <UserCircle :size="22" />
            <span>{{ auth.user?.name || '我的' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/customer/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ShoppingCart, UserCircle } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.customer-navbar {
  background: var(--paper);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-base);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.navbar-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-decoration: none;
  color: var(--ink);
  font-weight: 700;
  font-size: var(--text-lg);
}
.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--coral);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
}
.navbar-links {
  display: flex;
  gap: 4px;
}
.nav-link {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: var(--muted);
  font-size: var(--text-sm);
  transition: all var(--transition-fast);
}
.nav-link:hover, .nav-link.active {
  color: var(--teal);
  background: var(--teal-light);
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-base);
}
.cart-btn {
  position: relative;
  color: var(--muted);
  text-decoration: none;
  display: flex;
}
.cart-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background: var(--coral);
  color: #fff;
  font-size: 11px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: grid;
  place-items: center;
}
.user-btn {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--muted);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: color var(--transition-fast);
}
.user-btn:hover { color: var(--teal); }
</style>
