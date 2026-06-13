<template>
  <header class="admin-navbar">
    <div class="navbar-inner">
      <router-link to="/admin/dashboard" class="navbar-brand">
        <div class="brand-icon">N</div>
        <span>NekoCafe</span>
      </router-link>

      <nav class="navbar-links">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" :size="16" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="navbar-right">
        <span class="role-tag">{{ roleLabel }}</span>
        <el-dropdown trigger="click">
          <div class="user-btn">
            <UserCircle :size="20" />
            <span>{{ auth.user?.name || '管理员' }}</span>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LayoutDashboard, Store, Cat, Users, Megaphone, ClipboardList, UserCircle } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const role = computed(() => auth.user?.role || 'admin')

const roleLabel = computed(() => ({
  manager: '店长',
  operator: '总部运营',
  cat_keeper: '猫咪管家',
  admin: '管理员',
}[role.value] || '管理员'))

interface MenuItem { path: string; label: string; icon: any; roles: string[] }

const allMenuItems: MenuItem[] = [
  { path: '/admin/dashboard', label: '数据看板', icon: LayoutDashboard, roles: ['manager', 'operator', 'admin'] },
  { path: '/admin/stores', label: '门店管理', icon: Store, roles: ['manager', 'operator', 'admin'] },
  { path: '/admin/cats', label: '猫咪档案', icon: Cat, roles: ['manager', 'cat_keeper', 'admin'] },
  { path: '/admin/staff', label: '人员管理', icon: Users, roles: ['manager', 'admin'] },
  { path: '/admin/campaigns', label: '活动配置', icon: Megaphone, roles: ['operator', 'admin'] },
  { path: '/admin/audit-logs', label: '审计日志', icon: ClipboardList, roles: ['manager', 'operator', 'admin'] },
]

const menuItems = computed(() => allMenuItems.filter(item => item.roles.includes(role.value)))

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-navbar {
  background: var(--paper);
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
  gap: var(--space-xs);
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
  background: var(--teal-light);
  font-weight: 600;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-base);
}
.role-tag {
  font-size: var(--text-xs);
  color: var(--teal);
  background: var(--teal-light);
  padding: 2px var(--space-sm);
  border-radius: 99px;
  font-weight: 600;
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
