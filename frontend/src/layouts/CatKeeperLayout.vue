<template>
  <div class="ck-layout">
    <header class="ck-navbar">
      <div class="navbar-inner">
        <router-link to="/cat-keeper/cats" class="navbar-brand">
          <div class="brand-icon">N</div>
          <span>NekoCafe</span>
        </router-link>

        <nav class="navbar-links">
          <router-link to="/cat-keeper/cats" class="nav-link" :class="{ active: $route.path.startsWith('/cat-keeper/cats') }">
            <Cat :size="16" />
            <span>猫咪档案</span>
          </router-link>
        </nav>

        <div class="navbar-right">
          <span class="role-tag">猫咪管家</span>
          <el-dropdown trigger="click">
            <div class="user-btn">
              <UserCircle :size="20" />
              <span>{{ auth.user?.name || '猫咪管家' }}</span>
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
    <main class="ck-main">
      <router-view />
    </main>
    <footer class="ck-footer">
      <span>&copy; 2026 NekoCafe 猫咪管家</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Cat, UserCircle } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.ck-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--wash);
}
.ck-navbar {
  background: var(--paper);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}
.navbar-inner {
  max-width: 1200px;
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
  color: var(--gold);
  background: var(--gold-light);
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
.ck-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-lg);
}
.ck-footer {
  text-align: center;
  padding: var(--space-lg) var(--space-base);
  font-size: var(--text-xs);
  color: var(--muted);
  border-top: 1px solid var(--line);
  background: var(--paper);
}
</style>
