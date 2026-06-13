<template>
  <div class="page-enter-active">
    <!-- ═══ 门店头图 Hero ═══ -->
    <section class="store-hero">
      <img :src="storePhoto" alt="" class="store-hero-img" />
      <div class="store-hero-overlay"></div>
      <div class="store-hero-content">
        <h1>{{ storeName }}</h1>
        <div class="store-meta-row">
          <span class="store-rating">⭐ 4.8 <em>(126条评价)</em></span>
          <span class="store-dot">·</span>
          <span>北京市海淀区成府路 99 号</span>
        </div>
        <div class="store-meta-row">
          <span>🕐 营业时间：10:00 - 22:00</span>
          <span class="store-dot">·</span>
          <span>📞 010-88880001</span>
        </div>
        <el-button type="primary" size="large" class="store-hero-btn" @click="$router.push('/customer/reservation')">
          <CalendarDays :size="16" class="btn-icon" />预约此门店
        </el-button>
      </div>
    </section>

    <!-- ═══ 实时排队信息条 ═══ -->
    <div class="queue-bar">
      <div class="queue-pulse"></div>
      <span>当前等位 <strong>{{ queueCount }}</strong> 桌</span>
      <span class="queue-divider"></span>
      <span>预计等待 <strong>{{ queueCount * 8 }}</strong> 分钟</span>
      <span class="queue-divider"></span>
      <span>今日已接待 <strong>{{ servedToday }}</strong> 桌</span>
    </div>

    <h2>{{ storeName || '门店详情' }}</h2>

    <el-tabs v-model="activeTab">
      <!-- ═══ 猫咪档案 ═══ -->
      <el-tab-pane label="猫咪档案" name="cats">
        <!-- 骨架屏 -->
        <div v-if="loading" class="cat-grid">
          <div v-for="i in 4" :key="i" class="cat-card">
            <div class="skeleton" style="height: 150px"></div>
            <div style="padding: var(--space-base)">
              <div class="skeleton" style="height: 16px; width: 60%; margin-bottom: var(--space-xs)"></div>
              <div class="skeleton" style="height: 13px; width: 80%; margin-bottom: var(--space-sm)"></div>
              <div class="skeleton" style="height: 28px; width: 70px; margin: 0 auto"></div>
            </div>
          </div>
        </div>
        <div v-else-if="!cats.length">
          <el-empty description="暂无猫咪档案" />
        </div>
        <div v-else class="cat-grid">
          <div v-for="cat in cats" :key="cat.id" class="cat-card">
            <div class="cat-photo-wrap">
              <img :src="cat.photo_url || `https://placekitten.com/300/200`" :alt="cat.name" class="cat-photo" />
              <span class="cat-status-badge" :class="cat.health_status === 'healthy' ? 'healthy' : 'sick'">
                {{ cat.health_status === 'healthy' ? '健康' : '需关注' }}
              </span>
              <span class="cat-interact-badge">
                <Heart :size="10" /> 今日互动 {{ 3 + cat.id * 2 }} 次
              </span>
            </div>
            <div class="cat-body">
              <h4>{{ cat.name }}</h4>
              <p class="cat-breed">{{ cat.breed }}</p>
              <div class="cat-rating">
                <span v-for="i in 5" :key="i" :class="i <= 4 ? 'star-filled' : 'star-empty'">★</span>
                <span class="rating-count">({{ 18 + cat.id * 3 }})</span>
              </div>
              <div class="tags">
                <el-tag v-for="tag in cat.personality_tags" :key="tag" size="small" type="info" effect="plain">{{ tagLabel(tag) }}</el-tag>
              </div>
              <el-button size="small" plain class="cat-personality-btn" @click="showPersonality(cat)">
                <Info :size="14" class="btn-icon-sm" />看性格
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ═══ 桌位平面图 ═══ -->
      <el-tab-pane label="桌位" name="tables">
        <!-- 图例 -->
        <div class="floor-legend">
          <span><span class="legend-dot" style="background: #d1fae5; border-color: #6ee7b7"></span>可选</span>
          <span><span class="legend-dot" style="background: #f3f4f6; border-color: #d1d5db"></span>已占用</span>
          <span><span class="legend-dot" style="background: var(--teal)"></span>已选</span>
          <span><span class="legend-dot" style="background: #bbf7d0; border-color: #4ade80"></span>猫区</span>
        </div>

        <!-- SVG 平面图 -->
        <div class="floor-plan">
          <!-- 窗边区域 -->
          <div class="floor-zone zone-window">
            <div class="zone-label">🪟 靠窗区</div>
            <div class="zone-tables">
              <button
                v-for="table in tables.filter(t => t.area === 'window')"
                :key="table.id"
                class="floor-table"
                :class="floorTableClass(table)"
                :disabled="table.available_for_slot === false"
                @click="selectTable(table)"
              >
                <span class="ft-code">{{ table.code }}</span>
                <span class="ft-seats">{{ table.seats }}人</span>
                <span v-if="table.cat_zone" class="ft-cat">🐱</span>
              </button>
            </div>
          </div>

          <!-- 主厅 -->
          <div class="floor-zone zone-main">
            <div class="zone-label">🍽️ 主厅</div>
            <div class="zone-tables">
              <button
                v-for="table in tables.filter(t => t.area === 'main')"
                :key="table.id"
                class="floor-table"
                :class="floorTableClass(table)"
                :disabled="table.available_for_slot === false"
                @click="selectTable(table)"
              >
                <span class="ft-code">{{ table.code }}</span>
                <span class="ft-seats">{{ table.seats }}人</span>
                <span v-if="table.cat_zone" class="ft-cat">🐱</span>
              </button>
            </div>
          </div>

          <!-- 包厢 -->
          <div class="floor-zone zone-private">
            <div class="zone-label">🚪 包厢</div>
            <div class="zone-tables">
              <button
                v-for="table in tables.filter(t => t.area === 'private')"
                :key="table.id"
                class="floor-table"
                :class="floorTableClass(table)"
                :disabled="table.available_for_slot === false"
                @click="selectTable(table)"
              >
                <span class="ft-code">{{ table.code }}</span>
                <span class="ft-seats">{{ table.seats }}人</span>
              </button>
              <div v-if="!tables.filter(t => t.area === 'private').length" class="zone-empty">暂无包厢</div>
            </div>
          </div>
        </div>

        <!-- 选中提示 -->
        <div v-if="selectedTable" class="selected-bar">
          <CheckCircle :size="16" style="color: var(--teal)" />
          <span>已选 <strong>{{ selectedTable.code }}</strong>（{{ selectedTable.seats }}人 · {{ selectedTable.cat_zone ? '猫区' : tagLabel(selectedTable.area) }}）</span>
          <el-button type="primary" size="small" @click="$router.push('/customer/reservation')">去预约此桌</el-button>
          <el-button size="small" text @click="selectedTable = null">取消</el-button>
        </div>
      </el-tab-pane>

      <!-- ═══ 菜单（分类折叠） ═══ -->
      <el-tab-pane label="菜单" name="menu">
        <el-collapse v-model="activeMenuCategory">
          <el-collapse-item
            v-for="cat in menuCategories"
            :key="cat.key"
            :name="cat.key"
            :title="`${cat.icon} ${cat.label}（${cat.items.length}）`"
          >
            <div class="menu-list">
              <div v-for="(item, idx) in cat.items" :key="item.id" class="menu-row">
                <div class="menu-row-left">
                  <img :src="item.photo_url || MENU_PHOTOS[item.id % MENU_PHOTOS.length]" :alt="item.name" class="menu-photo" />
                  <div class="menu-info">
                    <div class="menu-name-row">
                      <strong>{{ item.name }}</strong>
                      <span v-if="item._recommended" class="menu-rec-badge">推荐</span>
                    </div>
                    <span class="menu-desc">{{ tagLabels(item.tags) }}</span>
                    <div class="menu-icons">
                      <span v-if="item._spicy" class="menu-icon-tag spicy" :title="`辣度 ${'🌶️'.repeat(item._spicy)}`">
                        {{ '🌶️'.repeat(item._spicy) }}
                      </span>
                      <span v-if="item._allergen" class="menu-icon-tag allergen" :title="`过敏原：${item._allergen}`">
                        ⚠️ {{ item._allergen }}
                      </span>
                    </div>
                  </div>
                </div>
                <div class="menu-row-right">
                  <span class="menu-price">{{ cents(item.price_cents) }}</span>
                  <span class="menu-sales">月售 {{ 38 + item.id * 12 }}</span>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>

    <!-- ═══ 性格弹窗 ═══ -->
    <el-dialog v-model="personalityDialog" :title="`${personalityCat?.name} 的性格档案`" width="400px" :show-close="true">
      <template v-if="personalityCat">
        <div class="personality-content">
          <img :src="personalityCat.photo_url || `https://placekitten.com/200/200`" :alt="personalityCat.name" class="personality-photo" />
          <h3>{{ personalityCat.name }} <span class="personality-breed">{{ personalityCat.breed }}</span></h3>
          <div class="personality-tags">
            <el-tag v-for="tag in personalityCat.personality_tags" :key="tag" type="success" effect="plain">{{ tagLabel(tag) }}</el-tag>
          </div>
          <p class="personality-desc">{{ personalityDesc }}</p>
          <div class="personality-stats">
            <div><strong>{{ 4.5 + personalityCat.id * 0.1 }}</strong><span>评分</span></div>
            <div><strong>{{ 15 + personalityCat.id * 8 }}</strong><span>被预约</span></div>
            <div><strong>{{ 3 + personalityCat.id * 2 }}</strong><span>今日互动</span></div>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Cat, CalendarDays, Heart, Info, CheckCircle } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents, tagLabel, tagLabels } from '@/utils/format'
import { fallbackTables, fallbackCats, fallbackMenuItems, fallbackStores } from '@/utils/fallback'
import type { DiningTable, Cat as CatType, MenuItem, Store } from '@/types'

const MENU_PHOTOS = [
  'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=300&h=200&fit=crop',
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300&h=200&fit=crop',
  'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=300&h=200&fit=crop',
  'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=300&h=200&fit=crop',
  'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=300&h=200&fit=crop',
  'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300&h=200&fit=crop',
]

const PERSONALITY_DESCS: Record<string, string> = {
  quiet: '性格安静温顺，喜欢安静地趴在客人腿上，适合喜欢安静阅读的客人。不会主动打扰，但会默默陪伴。',
  gentle: '非常温柔，喜欢被抚摸，对小朋友特别友好。会主动蹭人，是最受欢迎的陪伴型猫咪。',
  active: '精力充沛，喜欢追逐逗猫棒和激光笔。适合喜欢互动玩耍的客人，能带来很多欢乐。',
  photo: '镜头感十足，看到手机会自动摆 pose。是店内最上镜的猫咪，经常出现在顾客的朋友圈里。',
  sweet: '甜美可人，叫声软糯，喜欢撒娇。会在你喝咖啡的时候安静地趴在旁边，治愈感满满。',
  lazy: '典型的"沙发土豆"，大部分时间在睡觉。偶尔醒来会慢悠悠地走过来，适合喜欢慵懒氛围的客人。',
  curious: '好奇心旺盛，对新事物充满探索欲。会围着你的包转来转去，是最有趣的"小侦探"。',
}

const route = useRoute()
const storeId = Number(route.params.id)
const storeName = ref('')
const storePhoto = ref('')
const tables = ref<DiningTable[]>([])
const cats = ref<CatType[]>([])
const menuItems = ref<MenuItem[]>([])
const activeTab = ref('cats')
const selectedTable = ref<DiningTable | null>(null)
const personalityDialog = ref(false)
const personalityCat = ref<CatType | null>(null)
const activeMenuCategory = ref(['drink', 'dessert'])
const loading = ref(true)

// ── 排队 mock ──
const queueCount = ref(3)
const servedToday = ref(18)

const personalityDesc = computed(() => {
  if (!personalityCat.value) return ''
  const tags = personalityCat.value.personality_tags || []
  return tags.map(t => PERSONALITY_DESCS[t] || '').filter(Boolean).join('\n\n') || '这是一只性格独特的猫咪，等你来发现它的可爱之处。'

})

// ── 菜单分类 ──
interface EnrichedMenuItem extends MenuItem {
  _recommended?: boolean
  _spicy?: number
  _allergen?: string
}

const menuCategories = computed(() => {
  const cats_map: Record<string, { key: string; label: string; icon: string; items: EnrichedMenuItem[] }> = {}
  const catLabels: Record<string, { label: string; icon: string }> = {
    drink: { label: '饮品', icon: '☕' },
    meal: { label: '主食', icon: '🍱' },
    dessert: { label: '甜品', icon: '🍰' },
  }
  for (const item of menuItems.value) {
    const key = item.category || 'other'
    if (!cats_map[key]) {
      const meta = catLabels[key] || { label: key, icon: '🍽️' }
      cats_map[key] = { key, label: meta.label, icon: meta.icon, items: [] }
    }
    const enriched: EnrichedMenuItem = { ...item }
    // 推荐标记
    if (['猫爪拿铁', '毛线球芝士蛋糕', '三文鱼能量碗'].includes(item.name)) enriched._recommended = true
    // 辣度
    if (item.tags?.includes('spicy') || item.name.includes('辣')) enriched._spicy = 2
    // 过敏原
    if (item.tags?.includes('salmon') || item.name.includes('三文鱼')) enriched._allergen = '鱼类'
    if (item.tags?.includes('cheese') || item.name.includes('芝士')) enriched._allergen = '乳制品'
    cats_map[key].items.push(enriched)
  }
  return Object.values(cats_map)
})

function floorTableClass(table: DiningTable) {
  return {
    'ft-occupied': table.available_for_slot === false,
    'ft-selected': selectedTable.value?.id === table.id,
    'ft-cat-zone': table.cat_zone,
  }
}

function selectTable(table: DiningTable) {
  if (table.available_for_slot === false) return
  selectedTable.value = selectedTable.value?.id === table.id ? null : table
}

function showPersonality(cat: CatType) {
  personalityCat.value = cat
  personalityDialog.value = true
}

onMounted(async () => {
  try {
    const [t, c, m, s] = await Promise.all([
      api.get<DiningTable[]>('/tables', { storeId }),
      api.get<CatType[]>('/cats', { storeId }),
      api.get<MenuItem[]>('/menu-items', { storeId }),
      api.get<Store[]>('/stores'),
    ])
    tables.value = t; cats.value = c; menuItems.value = m
    const store = s.find(x => x.id === storeId)
    storeName.value = store?.name || `门店 #${storeId}`
    storePhoto.value = (store as any)?.photo_url || 'https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=1200&h=500&fit=crop'
  } catch {
    tables.value = fallbackTables; cats.value = fallbackCats; menuItems.value = fallbackMenuItems
    const store = fallbackStores.find(x => x.id === storeId)
    storeName.value = store?.name || `门店 #${storeId}`
    storePhoto.value = (store as any)?.photo_url || 'https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=1200&h=500&fit=crop'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ═══ 门店 Hero ═══ */
.store-hero {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--space-lg);
  min-height: 220px;
  display: flex;
  align-items: flex-end;
}
.store-hero-img {
  position: absolute;
  inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  filter: brightness(0.65);
}
.store-hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(0deg, rgba(0,0,0,0.6) 0%, transparent 60%);
}
.store-hero-content {
  position: relative;
  z-index: 1;
  padding: var(--space-lg);
  color: var(--paper);
  width: 100%;
}
.store-hero-content h1 {
  font-size: var(--text-2xl);
  color: var(--paper);
  margin-bottom: var(--space-xs);
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.store-meta-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--text-sm);
  opacity: 0.9;
  margin-bottom: var(--space-xs);
  flex-wrap: wrap;
}
.store-rating { font-weight: 700; }
.store-rating em { font-style: normal; font-weight: 400; opacity: 0.8; }
.store-dot { opacity: 0.5; }
.store-hero-btn { margin-top: var(--space-sm); }
.btn-icon { margin-right: 6px; }
.btn-icon-sm { margin-right: 4px; }

/* ═══ 排队信息条 ═══ */
.queue-bar {
  display: flex;
  align-items: center;
  gap: var(--space-base);
  padding: var(--space-sm) var(--space-lg);
  background: linear-gradient(135deg, #fef3c7, #fef9c3);
  border: 1px solid #fde68a;
  border-radius: var(--radius-md);
  margin-bottom: var(--space-lg);
  font-size: var(--text-sm);
  color: #92400e;
  flex-wrap: wrap;
}
.queue-pulse {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--gold);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}
.queue-bar strong { color: #78350f; font-size: var(--text-base); }
.queue-divider { width: 1px; height: 14px; background: #d97706; opacity: 0.3; }

/* ═══ 猫咪卡片 ═══ */
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: var(--space-base); }
.cat-card {
  background: var(--paper);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  overflow: hidden;
  transition: all var(--transition-fast);
}
.cat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-3px); }
.cat-photo-wrap { position: relative; }
.cat-photo { width: 100%; height: 150px; object-fit: cover; display: block; }
.cat-status-badge {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  padding: 2px var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: 10px;
  font-weight: 700;
  color: var(--paper);
}
.cat-status-badge.healthy { background: var(--success); }
.cat-status-badge.sick { background: var(--danger); }
.cat-interact-badge {
  position: absolute;
  bottom: var(--space-sm);
  left: var(--space-sm);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px var(--space-sm);
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  border-radius: 999px;
  color: var(--paper);
  font-size: 10px;
}
.cat-body { padding: var(--space-base); text-align: center; }
.cat-body h4 { margin-bottom: var(--space-xs); }
.cat-breed { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-xs); }
.cat-rating { display: flex; align-items: center; justify-content: center; gap: 2px; margin-bottom: var(--space-sm); }
.star-filled { color: var(--gold); font-size: 13px; }
.star-empty { color: var(--line); font-size: 13px; }
.rating-count { font-size: 11px; color: var(--muted); margin-left: var(--space-xs); }
.tags { display: flex; gap: var(--space-xs); justify-content: center; flex-wrap: wrap; margin-bottom: var(--space-sm); }
.cat-personality-btn { margin-top: var(--space-xs); }

/* ═══ 桌位平面图 ═══ */
.floor-legend {
  display: flex;
  gap: var(--space-base);
  font-size: var(--text-xs);
  color: var(--muted);
  margin-bottom: var(--space-base);
  flex-wrap: wrap;
}
.floor-legend span { display: flex; align-items: center; gap: var(--space-xs); }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; border: 1px solid transparent; display: inline-block; }

.floor-plan {
  display: flex;
  flex-direction: column;
  gap: var(--space-base);
  background: var(--wash);
  border: 2px solid var(--line);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  position: relative;
}
.floor-zone {
  position: relative;
  border: 2px dashed var(--line);
  border-radius: var(--radius-md);
  padding: var(--space-lg) var(--space-base) var(--space-base);
}
.zone-window { border-color: #93c5fd; background: #f0f9ff; }
.zone-main { border-color: var(--line); background: var(--paper); }
.zone-private { border-color: #c4b5fd; background: #f5f3ff; }
.zone-label {
  position: absolute;
  top: -10px;
  left: var(--space-base);
  background: var(--wash);
  padding: 0 var(--space-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--muted);
}
.zone-tables { display: flex; flex-wrap: wrap; gap: var(--space-sm); }
.zone-empty { font-size: var(--text-sm); color: var(--muted); padding: var(--space-base); text-align: center; }

.floor-table {
  width: 80px;
  padding: var(--space-sm) var(--space-xs);
  border: 2px solid var(--success-light);
  border-radius: var(--radius-md);
  background: var(--paper);
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
  font: inherit;
  color: inherit;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.floor-table:hover:not(:disabled) {
  border-color: var(--teal);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15,118,110,0.15);
}
.floor-table:disabled { cursor: not-allowed; opacity: 0.5; }
.ft-occupied {
  background: var(--wash);
  border-color: var(--line);
  opacity: 0.55;
}
.ft-selected {
  border-color: var(--teal);
  background: #e8f6f1;
  box-shadow: 0 0 0 3px rgba(15,118,110,0.2);
}
.ft-cat-zone { border-color: #86efac; background: var(--success-light); }
.ft-code { font-size: var(--text-base); font-weight: 700; }
.ft-seats { font-size: 10px; color: var(--muted); }
.ft-cat { font-size: 14px; }

.selected-bar {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-base);
  padding: var(--space-sm) var(--space-base);
  background: #e8f6f1;
  border: 1px solid #6ee7b7;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

/* ═══ 菜单折叠 ═══ */
.menu-list { display: flex; flex-direction: column; gap: var(--space-sm); }
.menu-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-base);
  background: var(--paper);
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  transition: all var(--transition-fast);
}
.menu-row:hover { border-color: var(--teal-light); box-shadow: var(--shadow-sm); }
.menu-row-left { display: flex; align-items: center; gap: var(--space-sm); flex: 1; min-width: 0; }
.menu-photo { width: 64px; height: 64px; object-fit: cover; border-radius: var(--radius-md); flex-shrink: 0; }
.menu-info { flex: 1; min-width: 0; }
.menu-name-row { display: flex; align-items: center; gap: var(--space-xs); margin-bottom: 2px; }
.menu-rec-badge {
  display: inline-block;
  padding: 1px 6px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: var(--paper);
  font-size: 10px;
  font-weight: 700;
  border-radius: var(--radius-sm);
}
.menu-desc { display: block; font-size: 12px; color: var(--muted); margin-bottom: var(--space-xs); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-icons { display: flex; gap: var(--space-xs); align-items: center; }
.menu-icon-tag { font-size: 12px; }
.menu-icon-tag.allergen { font-size: 11px; color: var(--gold); }
.menu-row-right { text-align: right; flex-shrink: 0; margin-left: var(--space-sm); }
.menu-price { display: block; font-weight: 700; color: var(--coral); font-size: var(--text-base); }
.menu-sales { display: block; font-size: var(--text-xs); color: var(--muted); margin-top: 2px; }

/* ═══ 性格弹窗 ═══ */
.personality-content { text-align: center; }
.personality-photo {
  width: 120px; height: 120px;
  object-fit: cover;
  border-radius: 50%;
  margin-bottom: var(--space-base);
  box-shadow: 0 4px 16px rgba(15, 118, 110, 0.15);
}
.personality-content h3 { margin-bottom: var(--space-sm); }
.personality-breed { font-weight: 400; color: var(--muted); font-size: var(--text-sm); }
.personality-tags { display: flex; gap: var(--space-xs); justify-content: center; margin-bottom: var(--space-base); }
.personality-desc {
  text-align: left;
  font-size: var(--text-sm);
  color: var(--ink);
  line-height: 1.7;
  white-space: pre-line;
  margin-bottom: var(--space-lg);
  padding: var(--space-base);
  background: var(--wash);
  border-radius: var(--radius-md);
}
.personality-stats {
  display: flex;
  justify-content: center;
  gap: var(--space-xl);
}
.personality-stats div { text-align: center; }
.personality-stats strong { display: block; font-size: var(--text-xl); color: var(--ink); }
.personality-stats span { font-size: var(--text-xs); color: var(--muted); }

/* ═══ 响应式 ═══ */
@media (max-width: 640px) {
  .store-hero { min-height: 180px; }
  .store-hero-content h1 { font-size: var(--text-xl); }
  .cat-grid { grid-template-columns: repeat(2, 1fr); gap: var(--space-sm); }
  .cat-photo { height: 100px; }
  .floor-table { width: 64px; }
  .menu-row { flex-direction: column; align-items: flex-start; gap: var(--space-sm); }
  .menu-photo { width: 100%; height: 100px; border-radius: var(--radius-sm); }
  .menu-row-right { text-align: left; }
  .queue-bar { font-size: var(--text-xs); padding: var(--space-sm) var(--space-base); }
}
</style>
