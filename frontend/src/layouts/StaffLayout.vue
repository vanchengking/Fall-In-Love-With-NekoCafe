<template>
  <div class="staff-layout">
    <StaffNavbar @store-change="onStoreChange" />
    <main class="staff-main">
      <router-view :store-id="currentStoreId" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StaffNavbar from '@/components/StaffNavbar.vue'

const currentStoreId = ref(1)
function onStoreChange(id: number) { currentStoreId.value = id }
</script>

<style scoped>
.staff-layout {
  min-height: 100vh;
  background: var(--wash);
}
.staff-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-lg);
}
</style>
