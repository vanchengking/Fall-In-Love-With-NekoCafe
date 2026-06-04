<template>
  <div class="admin-layout">
    <CustomerNavbar />
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import CustomerNavbar from '@/components/CustomerNavbar.vue'
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #faf9f6;
}
.admin-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}
</style>
