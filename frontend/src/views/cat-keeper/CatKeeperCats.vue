<template>
  <div class="page-enter-active">
    <div class="ck-toolbar">
      <h2>猫咪档案</h2>
      <div class="toolbar-right">
        <el-tag type="info" size="small" effect="plain">{{ auth.user?.name || '猫咪管家' }}</el-tag>
      </div>
    </div>

    <!-- 疫苗提醒 -->
    <div v-if="vaccineAlerts.length" class="vaccine-alerts">
      <div v-for="cat in vaccineAlerts" :key="cat.id" class="va-item">
        <AlertTriangle :size="14" class="alert-icon" />
        <span><strong>{{ cat.name }}</strong> 距上次疫苗已超 30 天，建议安排接种</span>
      </div>
    </div>

    <!-- 猫咪卡片网格 -->
    <div v-loading="loading" class="cat-grid">
      <div v-for="cat in cats" :key="cat.id" class="cat-card" role="button" tabindex="0" :aria-label="`查看 ${cat.name} 健康档案`" @click="$router.push(`/cat-keeper/cats/${cat.id}/health`)" @keydown.enter="$router.push(`/cat-keeper/cats/${cat.id}/health`)">
        <div class="cc-photo-wrap">
          <img v-if="cat.photo_url" :src="cat.photo_url" :alt="cat.name" class="cat-photo" />
          <div v-else class="cat-avatar"><Cat :size="32" /></div>
          <span class="cc-status" :class="'ccs-' + cat.health_status">{{ healthStatusLabel(cat.health_status) }}</span>
        </div>
        <div class="cc-info">
          <h3>{{ cat.name }}</h3>
          <p class="cc-breed">{{ cat.breed }}</p>
          <div class="cc-meta">
            <span v-if="cat.weight_kg" class="cc-weight">{{ cat.weight_kg }} kg</span>
            <span v-if="cat.last_vaccine_at" class="cc-vaccine">💉 {{ cat.last_vaccine_at }}</span>
          </div>
          <div class="cc-tags">
            <el-tag v-for="t in (cat.personality_tags || []).slice(0, 3)" :key="t" size="small" effect="plain">{{ tagLabel(t) }}</el-tag>
          </div>
        </div>
        <div class="cc-arrow">
          <ChevronRight :size="16" />
        </div>
      </div>
      <el-empty v-if="!loading && !cats.length" description="暂无猫咪档案" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Cat, AlertTriangle, ChevronRight } from '@lucide/vue'
import { api } from '@/utils/http'
import { tagLabel } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { fallbackCats } from '@/utils/fallback'
import type { Cat as CatType } from '@/types'

const auth = useAuthStore()
const cats = ref<CatType[]>([])
const loading = ref(false)

function healthStatusLabel(s?: string): string {
  return { healthy: '健康', observe: '观察中', sick: '生病', resting: '休息' }[s || 'healthy'] || '健康'
}

const vaccineAlerts = computed(() => {
  const now = Date.now()
  return cats.value.filter(c => {
    if (!c.last_vaccine_at) return true
    const days = (now - new Date(c.last_vaccine_at).getTime()) / 86400000
    return days > 30
  })
})

async function loadCats() {
  loading.value = true
  try {
    const storeId = auth.user?.store_id || auth.user?.storeId || 1
    cats.value = await api.get<CatType[]>('/cats', { storeId })
  } catch {
    cats.value = fallbackCats
  } finally {
    loading.value = false
  }
}

onMounted(loadCats)
</script>

<style scoped>
.ck-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.ck-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.alert-icon { color: var(--gold); }

.vaccine-alerts {
  background: var(--gold-light); border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-base); margin-bottom: var(--space-base);
  display: flex; flex-direction: column; gap: var(--space-xs);
}
.va-item { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-sm); color: #92400e; }

.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-base);
}
.cat-card {
  display: flex; align-items: center; gap: var(--space-base);
  background: var(--paper); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: var(--space-lg);
  cursor: pointer; transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}
.cat-card:hover { border-color: var(--teal); box-shadow: var(--shadow-lg); transform: translateY(-3px); }

.cc-photo-wrap {
  position: relative; flex-shrink: 0;
  width: 80px; height: 80px; border-radius: var(--radius-lg);
  overflow: hidden; box-shadow: var(--shadow-sm);
}
.cat-photo { width: 100%; height: 100%; object-fit: cover; }
.cat-avatar {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, var(--teal-light), #d4f0ed);
  color: var(--teal);
  display: grid; place-items: center;
  font-size: 28px;
}
.cc-status {
  position: absolute; bottom: 2px; left: 2px;
  font-size: 10px; padding: 2px var(--space-xs); border-radius: 99px;
  color: #fff; font-weight: 600;
}
.ccs-healthy { background: var(--success); }
.ccs-observe { background: var(--gold); }
.ccs-sick { background: var(--danger); }
.ccs-resting { background: var(--muted); }

.cc-info { flex: 1; min-width: 0; }
.cc-info h3 { margin: 0 0 var(--space-xs); font-size: var(--text-lg); font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cc-breed { font-size: var(--text-sm); color: var(--muted); margin: 0 0 var(--space-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cc-meta { display: flex; gap: var(--space-sm); font-size: var(--text-xs); color: var(--muted); margin-bottom: var(--space-sm); }
.cc-tags { display: flex; gap: var(--space-xs); flex-wrap: wrap; }

.cc-arrow { color: var(--muted); flex-shrink: 0; transition: transform var(--transition-fast); }
.cat-card:hover .cc-arrow { transform: translateX(3px); }
</style>
