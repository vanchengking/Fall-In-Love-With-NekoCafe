<template>
  <section class="fp-plan">
    <!-- 窗户装饰条 -->
    <div class="fp-windows" v-if="showDecorations">
      <div class="fp-window-pane" v-for="i in 8" :key="i">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="fp-window-icon">
          <rect x="2" y="3" width="20" height="18" rx="2"/>
          <line x1="12" y1="3" x2="12" y2="21"/>
          <line x1="2" y1="12" x2="22" y2="12"/>
        </svg>
      </div>
    </div>

    <!-- 靠窗猫区 -->
    <div class="fp-zone fp-zone-window">
      <div class="fp-zone-header">
        <span class="fp-zone-icon">🐱</span>
        <span class="fp-zone-title">靠窗猫区</span>
        <span class="fp-zone-count">{{ windowTables.length }} 桌</span>
      </div>
      <div class="fp-zone-tables">
        <FloorPlanTable
          v-for="t in windowTables" :key="t.id"
          :table="t"
          :selected="selectedId === t.id"
          :show-status-tag="mode === 'staff'"
          :show-status-dot="mode === 'staff'"
          :reservation="getReservation(t)"
          :mode="mode"
          @select="$emit('select', $event)"
        />
      </div>
    </div>

    <!-- 过道 -->
    <div class="fp-aisle" v-if="showDecorations">
      <div class="fp-aisle-line"></div>
      <span class="fp-aisle-text">过 道</span>
      <div class="fp-aisle-line"></div>
    </div>

    <!-- 主厅 -->
    <div class="fp-zone fp-zone-main">
      <div class="fp-zone-header">
        <span class="fp-zone-icon">🏠</span>
        <span class="fp-zone-title">主厅</span>
        <span class="fp-zone-count">{{ mainTables.length }} 桌</span>
      </div>
      <div class="fp-zone-tables fp-zone-tables-main">
        <FloorPlanTable
          v-for="t in mainTables" :key="t.id"
          :table="t"
          :selected="selectedId === t.id"
          :show-status-tag="mode === 'staff'"
          :show-status-dot="mode === 'staff'"
          :reservation="getReservation(t)"
          :mode="mode"
          @select="$emit('select', $event)"
        />
      </div>
    </div>

    <!-- 中央走道 -->
    <div class="fp-aisle fp-aisle-wide" v-if="showDecorations">
      <div class="fp-aisle-line"></div>
      <span class="fp-aisle-text">中央走道</span>
      <div class="fp-aisle-line"></div>
    </div>

    <!-- 底部区域：入口 + 聚会区 -->
    <div class="fp-bottom" v-if="showDecorations">
      <!-- 入口 + 前台 -->
      <div class="fp-entrance-section">
        <div class="fp-entrance-block">
          <div class="fp-entrance-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="fp-arrow-icon">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
              <polyline points="10 17 15 12 10 7"/>
              <line x1="15" y1="12" x2="3" y2="12"/>
            </svg>
          </div>
          <div class="fp-entrance-label">入 口</div>
        </div>
        <div class="fp-counter-block">
          <div class="fp-counter-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="fp-counter-svg">
              <path d="M3 7v4a1 1 0 0 0 1 1h3"/>
              <path d="M21 7v4a1 1 0 0 1-1 1h-3"/>
              <path d="M3 11a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v2a8 8 0 0 1-8 8h-2a8 8 0 0 1-8-8v-2z"/>
              <path d="M10 11V7a2 2 0 0 1 4 0v4"/>
            </svg>
          </div>
          <div class="fp-counter-label">前台 / 取餐区</div>
        </div>
        <div class="fp-plant-deco">
          <svg viewBox="0 0 40 50" class="fp-plant-svg">
            <rect x="14" y="35" width="12" height="14" rx="2" fill="#a3e635" opacity="0.5"/>
            <path d="M20 38 C20 25, 10 20, 8 12 C8 12, 18 18, 20 28" fill="#4ade80" opacity="0.7"/>
            <path d="M20 35 C20 22, 30 18, 32 10 C32 10, 22 16, 20 26" fill="#22c55e" opacity="0.6"/>
            <path d="M20 32 C18 22, 12 24, 6 20 C6 20, 14 20, 20 30" fill="#16a34a" opacity="0.5"/>
          </svg>
        </div>
      </div>

      <!-- 聚会区 -->
      <div class="fp-zone fp-zone-party">
        <div class="fp-zone-header">
          <span class="fp-zone-icon">🎉</span>
          <span class="fp-zone-title">聚会区</span>
          <span class="fp-zone-count">{{ partyTables.length }} 桌</span>
        </div>
        <div class="fp-zone-tables">
          <FloorPlanTable
            v-for="t in partyTables" :key="t.id"
            :table="t"
            :selected="selectedId === t.id"
            :show-status-tag="mode === 'staff'"
            :show-status-dot="mode === 'staff'"
            :reservation="getReservation(t)"
            :mode="mode"
            @select="$emit('select', $event)"
          />
        </div>
      </div>
    </div>

    <!-- 顾客端无装饰时也显示聚会区 -->
    <div class="fp-zone fp-zone-party" v-if="!showDecorations && partyTables.length">
      <div class="fp-zone-header">
        <span class="fp-zone-icon">🎉</span>
        <span class="fp-zone-title">聚会区</span>
        <span class="fp-zone-count">{{ partyTables.length }} 桌</span>
      </div>
      <div class="fp-zone-tables">
        <FloorPlanTable
          v-for="t in partyTables" :key="t.id"
          :table="t"
          :selected="selectedId === t.id"
          :show-status-tag="mode === 'staff'"
          :show-status-dot="mode === 'staff'"
          :reservation="getReservation(t)"
          :mode="mode"
          @select="$emit('select', $event)"
        />
      </div>
    </div>

    <!-- 底部绿植 -->
    <div class="fp-plants" v-if="showDecorations">
      <svg viewBox="0 0 40 50" class="fp-plant-sm" v-for="i in 3" :key="i">
        <rect x="14" y="35" width="12" height="14" rx="2" fill="#a3e635" opacity="0.4"/>
        <path d="M20 38 C20 25, 10 20, 8 12 C8 12, 18 18, 20 28" fill="#4ade80" opacity="0.6"/>
        <path d="M20 35 C20 22, 30 18, 32 10 C32 10, 22 16, 20 26" fill="#22c55e" opacity="0.5"/>
      </svg>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FloorPlanTable from './FloorPlanTable.vue'
import type { DiningTable, Reservation } from '@/types'

const props = withDefaults(defineProps<{
  tables: DiningTable[]
  selectedId?: number | null
  reservations?: Reservation[]
  mode?: 'staff' | 'customer'
  showDecorations?: boolean
}>(), {
  selectedId: null,
  reservations: () => [],
  mode: 'staff',
  showDecorations: true,
})

defineEmits<{
  select: [table: DiningTable]
}>()

const windowTables = computed(() => props.tables.filter(t => t.area === 'window'))
const mainTables = computed(() => props.tables.filter(t => t.area === 'main'))
const partyTables = computed(() => props.tables.filter(t => t.area === 'party'))

function getReservation(t: DiningTable): Reservation | null {
  if (!t.current_reservation_id) return null
  return props.reservations.find(r => r.id === t.current_reservation_id) || null
}
</script>

<style>
/* ═══════════════════════════════════════
   平面图容器
   ═══════════════════════════════════════ */
.fp-plan {
  background: #f0eeeb;
  border: 2px solid #d9d5ce;
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  overflow-x: auto;
  position: relative;
  min-height: 520px;
}

/* ── 窗户装饰条 ── */
.fp-windows {
  display: flex; gap: 6px;
  padding: 0 var(--space-sm); margin-bottom: var(--space-xs);
}
.fp-window-pane {
  flex: 1; display: flex; align-items: center; justify-content: center;
  height: 28px;
  background: linear-gradient(180deg, #e0f2fe 0%, #bae6fd 100%);
  border-radius: 4px 4px 0 0;
  border: 1px solid #7dd3fc; border-bottom: none;
}
.fp-window-icon { width: 16px; height: 16px; color: #0ea5e9; }

/* ═══════════════════════════════════════
   分区
   ═══════════════════════════════════════ */
.fp-zone {
  border-radius: var(--radius-md);
  padding: var(--space-base);
  position: relative;
}
.fp-zone-header {
  display: flex; align-items: center; gap: var(--space-xs);
  margin-bottom: var(--space-base);
}
.fp-zone-icon { font-size: 16px; }
.fp-zone-title {
  font-size: 13px; font-weight: 700;
  color: #667085; letter-spacing: 0.5px;
}
.fp-zone-count {
  font-size: var(--text-xs); color: #9ca3af;
  margin-left: auto;
}

/* 靠窗猫区 */
.fp-zone-window {
  background: linear-gradient(180deg, #f0fdfa 0%, #e0f5f0 100%);
  border: 1.5px dashed #5eead4;
}
.fp-zone-window .fp-zone-tables {
  display: flex; justify-content: center;
  gap: 28px; flex-wrap: wrap;
  padding-bottom: 36px;
}

/* 主厅 */
.fp-zone-main {
  background: linear-gradient(180deg, #fefce8 0%, #fef9c3 100%);
  border: 1.5px dashed #facc15;
}
.fp-zone-tables-main {
  display: flex; justify-content: center;
  gap: var(--space-lg); flex-wrap: wrap;
  align-items: flex-start;
  padding-bottom: 36px;
}

/* 聚会区 */
.fp-zone-party {
  flex: 1;
  background: linear-gradient(135deg, var(--purple-light) 0%, #f3e8ff 100%);
  border: 1.5px dashed #c4b5fd;
}
.fp-zone-party .fp-zone-tables {
  display: flex; gap: 28px;
  justify-content: center; flex-wrap: wrap;
  align-items: flex-start;
  padding-bottom: 36px;
}

/* ── 过道 ── */
.fp-aisle {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-xs) 0;
}
.fp-aisle-line {
  flex: 1; height: 1px;
  background: repeating-linear-gradient(90deg, #d1d5db 0, #d1d5db 6px, transparent 6px, transparent 12px);
}
.fp-aisle-text {
  font-size: 10px; color: #9ca3af;
  letter-spacing: 4px; white-space: nowrap;
}
.fp-aisle-wide { padding: var(--space-sm) 0; }

/* ── 底部区域 ── */
.fp-bottom {
  display: flex; gap: var(--space-base); align-items: stretch;
}

/* ── 入口区域 ── */
.fp-entrance-section {
  width: 140px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: var(--space-sm);
  align-items: center; justify-content: flex-end;
}
.fp-entrance-block {
  width: 100%; padding: var(--space-base);
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border: 2px dashed #94a3b8; border-radius: var(--radius-md);
  display: flex; flex-direction: column; align-items: center; gap: var(--space-xs);
}
.fp-entrance-arrow { color: #475569; }
.fp-arrow-icon { width: 28px; height: 28px; }
.fp-entrance-label {
  font-size: var(--text-sm); font-weight: 700;
  color: #475569; letter-spacing: 2px;
}

.fp-counter-block {
  width: 100%; padding: var(--space-sm) var(--space-base);
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1.5px solid #93c5fd; border-radius: var(--radius-md);
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.fp-counter-icon { color: #3b82f6; }
.fp-counter-svg { width: 22px; height: 22px; }
.fp-counter-label { font-size: var(--text-xs); font-weight: 600; color: #3b82f6; }

.fp-plant-deco { margin-top: auto; }
.fp-plant-svg { width: 36px; height: 44px; }

/* ── 底部绿植 ── */
.fp-plants {
  display: flex; justify-content: space-around;
  padding: 4px 24px 0;
}
.fp-plant-sm { width: 28px; height: 36px; opacity: 0.6; }
</style>
