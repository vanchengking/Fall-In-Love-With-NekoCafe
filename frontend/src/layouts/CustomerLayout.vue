<template>
  <div class="customer-layout">
    <CustomerNavbar />
    <main class="customer-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import CustomerNavbar from '@/components/CustomerNavbar.vue'
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  background: #faf9f6;
}
.customer-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}
</style>
