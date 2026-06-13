<template>
  <div class="admin-layout">
    <AdminNavbar />
    <main class="admin-main">
      <router-view />
    </main>
    <footer class="admin-footer">
      <span>&copy; 2026 NekoCafe 管理后台</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import AdminNavbar from '@/components/AdminNavbar.vue'
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--wash);
}
.admin-main {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-lg);
}
.admin-footer {
  text-align: center;
  padding: var(--space-lg) var(--space-base);
  font-size: var(--text-xs);
  color: var(--muted);
  border-top: 1px solid var(--line);
  background: var(--paper);
}
</style>
