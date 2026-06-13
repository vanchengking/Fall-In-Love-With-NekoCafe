<template>
  <header class="staff-navbar">
    <div class="navbar-inner">
      <router-link to="/staff" class="navbar-brand">
        <div class="brand-icon">N</div>
        <span>NekoCafe</span>
      </router-link>

      <nav class="navbar-links">
        <router-link to="/staff" class="nav-link" :class="{ active: $route.path === '/staff' }">
          <CalendarDays :size="16" />今日预约
        </router-link>
        <router-link to="/staff/reservations" class="nav-link" :class="{ active: $route.path === '/staff/reservations' }">
          <ClipboardList :size="16" />预约管理
        </router-link>
        <router-link to="/staff/orders" class="nav-link" :class="{ active: $route.path === '/staff/orders' }">
          <ShoppingBag :size="16" />订单
        </router-link>
        <router-link to="/staff/tables" class="nav-link" :class="{ active: $route.path === '/staff/tables' }">
          <LayoutGrid :size="16" />桌位状态
        </router-link>
      </nav>

      <div class="navbar-right">
        <el-select v-model="storeId" size="small" style="width: 140px" @change="onStoreChange">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-dropdown trigger="click">
          <div class="user-btn">
            <UserCircle :size="20" />
            <span>{{ auth.user?.name || '店员' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarDays, ClipboardList, ShoppingBag, LayoutGrid, UserCircle } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { fallbackStores } from '@/utils/fallback'
import type { Store } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const stores = ref<Store[]>([])
const storeId = ref(1)

const emit = defineEmits<{ (e: 'store-change', id: number): void }>()

function onStoreChange(id: number) {
  emit('store-change', id)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
})
</script>

<style scoped>
.staff-navbar {
  background: #fff;
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}
.navbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 56px;
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
  width: 32px; height: 32px;
  border-radius: var(--radius-sm);
  background: var(--coral);
  color: #fff;
  display: grid; place-items: center;
  font-weight: 800;
  font-size: 16px;
}
.navbar-links {
  display: flex;
  gap: 2px;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-base);
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: var(--muted);
  font-size: var(--text-sm);
  font-weight: 500;
  transition: all var(--transition-fast);
  border-bottom: 2px solid transparent;
}
.nav-link:hover { color: var(--teal); background: var(--teal-light); }
.nav-link.active {
  color: var(--teal);
  border-bottom-color: var(--teal);
  background: transparent;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-base);
}
.user-btn {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--muted);
  font-size: var(--text-sm);
  cursor: pointer;
}
.user-btn:hover { color: var(--teal); }

@media (max-width: 768px) {
  .navbar-inner { padding: 0 var(--space-base); }
  .navbar-links { gap: 0; }
  .nav-link { padding: var(--space-sm); font-size: var(--text-xs); }
  .nav-link span:last-child { display: none; }
}
</style>
