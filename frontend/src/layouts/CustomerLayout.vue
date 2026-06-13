<template>
  <div class="customer-layout">
    <CustomerNavbar />
    <main class="customer-main">
      <router-view />
    </main>
    <footer class="customer-footer">
      <span>&copy; 2026 NekoCafe 智慧餐饮预约平台</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import CustomerNavbar from '@/components/CustomerNavbar.vue'
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--wash);
}
.customer-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-lg) var(--space-base);
}
.customer-footer {
  text-align: center;
  padding: var(--space-lg) var(--space-base);
  font-size: var(--text-xs);
  color: var(--muted);
  border-top: 1px solid var(--line);
  background: var(--paper);
}
</style>
