<template>
  <div class="page-enter-active">
    <h2>选择门店</h2>
    <div v-if="loading" class="store-list">
      <div v-for="i in 3" :key="i" class="store-card">
        <div class="skeleton" style="height: 22px; width: 60%; margin-bottom: var(--space-sm)"></div>
        <div class="skeleton" style="height: 16px; width: 80%; margin-bottom: var(--space-sm)"></div>
        <div class="skeleton" style="height: 16px; width: 40%"></div>
      </div>
    </div>
    <div v-else-if="!stores.length" class="empty-state">
      <el-empty description="暂无门店数据" />
    </div>
    <div v-else class="store-list">
      <div v-for="(store, idx) in stores" :key="store.id"
           class="store-card card-interactive"
           :class="idx % 2 === 0 ? 'store-card--left' : 'store-card--right'"
           tabindex="0" role="link" :aria-label="`查看门店 ${store.name}`"
           @click="$router.push(`/customer/stores/${store.id}`)"
           @keydown.enter="$router.push(`/customer/stores/${store.id}`)"
           @keydown.space.prevent="$router.push(`/customer/stores/${store.id}`)">
        <div class="store-visual">
          <div class="store-icon">🏪</div>
        </div>
        <div class="store-content">
          <div class="store-badge">{{ store.city }}</div>
          <h3>{{ store.name }}</h3>
          <p>{{ store.address }}</p>
          <p class="store-meta">{{ store.phone }} · {{ store.table_count }} 桌 · {{ store.total_seats }} 座</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/http'
import { fallbackStores } from '@/utils/fallback'
import type { Store } from '@/types'

const stores = ref<Store[]>([])
const loading = ref(true)

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
  finally { loading.value = false }
})
</script>

<style scoped>
.empty-state { text-align: center; padding: var(--space-2xl); }
.store-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.store-card {
  display: flex;
  align-items: center;
  gap: var(--space-xl);
  padding: var(--space-xl);
  background: var(--paper);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
}
.store-card--right {
  flex-direction: row-reverse;
}
.store-visual {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--teal-light), var(--teal-light));
  border-radius: var(--radius-lg);
  display: grid;
  place-items: center;
  font-size: 36px;
}
.store-card--right .store-visual {
  background: linear-gradient(135deg, var(--coral-light), var(--coral-light));
}
.store-content {
  flex: 1;
}
.store-badge {
  display: inline-block;
  padding: 2px var(--space-md);
  background: var(--teal-light);
  color: var(--teal-dark);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  margin-bottom: var(--space-sm);
}
.store-card--right .store-badge {
  background: var(--coral-light);
  color: var(--coral);
}
.store-card h3 {
  margin-bottom: var(--space-xs);
}
.store-card p {
  color: var(--muted);
  font-size: var(--text-sm);
}
.store-meta {
  margin-top: var(--space-xs);
  font-size: var(--text-xs) !important;
}
@media (max-width: 640px) {
  .store-card, .store-card--right {
    flex-direction: column;
    text-align: center;
    gap: var(--space-base);
    padding: var(--space-lg);
  }
  .store-visual { width: 64px; height: 64px; font-size: 28px; }
}
</style>
