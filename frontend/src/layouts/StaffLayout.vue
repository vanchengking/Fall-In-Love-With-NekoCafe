<template>
  <div class="staff-layout">
    <CustomerNavbar />
    <main class="staff-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import CustomerNavbar from '@/components/CustomerNavbar.vue'
</script>

<style scoped>
.staff-layout {
  min-height: 100vh;
  background: #faf9f6;
}
.staff-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}
</style>
