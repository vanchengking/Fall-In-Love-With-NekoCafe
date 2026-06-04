<template>
  <header class="customer-navbar">
    <div class="navbar-inner">
      <router-link :to="homeRoute" class="navbar-brand">
        <div class="brand-icon">N</div>
        <span>NekoCafe</span>
      </router-link>

      <nav class="navbar-links">
        <template v-if="isCustomer">
          <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">首页</router-link>
          <router-link to="/stores" class="nav-link" :class="{ active: $route.path.startsWith('/stores') }">门店</router-link>
          <router-link to="/reservation" class="nav-link" :class="{ active: $route.path === '/reservation' }">预约</router-link>
          <router-link to="/order" class="nav-link" :class="{ active: $route.path === '/order' }">点单</router-link>
          <router-link to="/reviews" class="nav-link" :class="{ active: $route.path === '/reviews' }">评价</router-link>
        </template>
        <template v-else-if="isStaff">
          <router-link to="/staff" class="nav-link" :class="{ active: $route.path === '/staff' }">今日预约</router-link>
          <router-link to="/staff/reservations" class="nav-link" :class="{ active: $route.path === '/staff/reservations' }">预约管理</router-link>
          <router-link to="/staff/orders" class="nav-link" :class="{ active: $route.path === '/staff/orders' }">订单</router-link>
          <router-link to="/staff/tables" class="nav-link" :class="{ active: $route.path === '/staff/tables' }">桌位状态</router-link>
        </template>
        <template v-else-if="isCatKeeper">
          <router-link to="/admin/cats" class="nav-link" :class="{ active: $route.path === '/admin/cats' }">猫咪档案</router-link>
        </template>
        <template v-else-if="isManager">
          <router-link to="/admin/dashboard" class="nav-link" :class="{ active: $route.path === '/admin/dashboard' }">数据看板</router-link>
          <router-link to="/admin/staff" class="nav-link" :class="{ active: $route.path === '/admin/staff' }">人员管理</router-link>
          <router-link to="/admin/cats" class="nav-link" :class="{ active: $route.path === '/admin/cats' }">猫咪档案</router-link>
          <router-link to="/admin/stores" class="nav-link" :class="{ active: $route.path === '/admin/stores' }">门店管理</router-link>
        </template>
        <template v-else-if="isOperator">
          <router-link to="/admin/dashboard" class="nav-link" :class="{ active: $route.path === '/admin/dashboard' }">全局数据</router-link>
          <router-link to="/admin/campaigns" class="nav-link" :class="{ active: $route.path === '/admin/campaigns' }">活动配置</router-link>
          <router-link to="/admin/stores" class="nav-link" :class="{ active: $route.path === '/admin/stores' }">门店管理</router-link>
        </template>
        <template v-else>
          <router-link to="/admin/dashboard" class="nav-link" :class="{ active: $route.path === '/admin/dashboard' }">数据看板</router-link>
          <router-link to="/admin/cats" class="nav-link" :class="{ active: $route.path === '/admin/cats' }">猫咪档案</router-link>
          <router-link to="/admin/stores" class="nav-link" :class="{ active: $route.path === '/admin/stores' }">门店管理</router-link>
          <router-link to="/admin/staff" class="nav-link" :class="{ active: $route.path === '/admin/staff' }">人员管理</router-link>
          <router-link to="/admin/campaigns" class="nav-link" :class="{ active: $route.path === '/admin/campaigns' }">活动配置</router-link>
        </template>
      </nav>

      <div class="navbar-right">
        <router-link v-if="isCustomer" to="/order" class="cart-btn">
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
              <el-dropdown-item v-if="isCustomer" @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart, UserCircle } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { roleDefaultRoute } from '@/router'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

const role = computed(() => auth.user?.role || 'customer')
const isCustomer = computed(() => ['customer'].includes(role.value))
const isStaff = computed(() => ['staff'].includes(role.value))
const isCatKeeper = computed(() => role.value === 'cat_keeper')
const isManager = computed(() => role.value === 'manager')
const isOperator = computed(() => role.value === 'operator')
const isAdmin = computed(() => role.value === 'admin')
const homeRoute = computed(() => roleDefaultRoute[role.value] || '/')

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.customer-navbar {
  background: #fff;
  border-bottom: 1px solid #e8e5df;
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #172033;
  font-weight: 700;
  font-size: 18px;
}
.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e86f51;
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
  padding: 8px 14px;
  border-radius: 8px;
  text-decoration: none;
  color: #667085;
  font-size: 14px;
  transition: all 0.15s;
}
.nav-link:hover, .nav-link.active {
  color: #0f766e;
  background: #edf7f4;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.cart-btn {
  position: relative;
  color: #667085;
  text-decoration: none;
  display: flex;
}
.cart-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background: #e86f51;
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
  gap: 6px;
  color: #667085;
  font-size: 14px;
  cursor: pointer;
}
.user-btn:hover { color: #0f766e; }
</style>
