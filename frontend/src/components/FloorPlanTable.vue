<template>
  <div
    class="fp-table"
    :class="wrapClass"
    @click="$emit('select', table)"
  >
    <!-- 上排座椅 -->
    <div class="fp-seats-row fp-seats-top">
      <span class="fp-seat" v-for="i in seatHalfTop(table.seats)" :key="'t'+i"></span>
    </div>
    <div class="fp-body-row">
      <!-- 左侧座椅 -->
      <div class="fp-seats-col fp-seats-left">
        <span class="fp-seat" v-for="i in seatHalfSide(table.seats)" :key="'l'+i"></span>
      </div>
      <!-- 桌面 -->
      <div class="fp-surface" :class="surfaceClass">
        <div class="fp-paw" v-if="table.cat_zone">
          <svg viewBox="0 0 32 32" class="fp-paw-svg" :style="{ color: pawColor }">
            <ellipse cx="16" cy="22" rx="7" ry="6" fill="currentColor" opacity="0.8"/>
            <circle cx="9" cy="13" r="3.5" fill="currentColor" opacity="0.7"/>
            <circle cx="16" cy="10" r="3.5" fill="currentColor" opacity="0.7"/>
            <circle cx="23" cy="13" r="3.5" fill="currentColor" opacity="0.7"/>
          </svg>
        </div>
        <div class="fp-code">{{ table.code }}</div>
        <div class="fp-seats-label">{{ table.seats }}人</div>
        <div v-if="showStatusDot" class="fp-status-dot" :style="{ background: statusColorHex }"></div>
      </div>
      <!-- 右侧座椅 -->
      <div class="fp-seats-col fp-seats-right">
        <span class="fp-seat" v-for="i in seatHalfSide(table.seats)" :key="'r'+i"></span>
      </div>
    </div>
    <!-- 下排座椅 -->
    <div class="fp-seats-row fp-seats-bottom">
      <span class="fp-seat" v-for="i in seatHalfTop(table.seats)" :key="'b'+i"></span>
    </div>
    <!-- 状态标签 -->
    <div v-if="showStatusTag" class="fp-tag" :class="'fp-tag-' + effectiveStatus">
      {{ statusLabel }}
    </div>
    <!-- 预约信息 -->
    <div v-if="showStatusTag && reservation" class="fp-res">
      {{ reservation.reservation_time }} {{ (reservation.customer_name || '').slice(0, 3) }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DiningTable, Reservation, TableStatus } from '@/types'

const props = withDefaults(defineProps<{
  table: DiningTable
  selected?: boolean
  showStatusTag?: boolean
  showStatusDot?: boolean
  reservation?: Reservation | null
  mode?: 'staff' | 'customer'
}>(), {
  selected: false,
  showStatusTag: false,
  showStatusDot: false,
  reservation: null,
  mode: 'staff',
})

defineEmits<{
  select: [table: DiningTable]
}>()

// ── 桌面尺寸分级 ──
function surfaceSizeClass(seats: number): string {
  if (seats <= 2) return 'fp-sm'
  if (seats <= 4) return 'fp-md'
  if (seats <= 6) return 'fp-lg'
  return 'fp-xl'
}

// ── 座椅分布 ──
function seatHalfTop(seats: number): number { return Math.ceil(seats / 2) }
function seatHalfSide(seats: number): number { return seats >= 2 ? 1 : 0 }

const surfaceClass = computed(() => surfaceSizeClass(props.table.seats))

// ── 状态映射 ──
const statusColorMap: Record<string, string> = {
  free: '#10B981', reserved: '#3B82F6', occupied: '#0F766E',
  dining: '#EF4444', cleaning: '#F59E0B', maintenance: '#9CA3AF',
}

const statusLabelMap: Record<string, string> = {
  free: '空闲', reserved: '已预约', occupied: '已入座',
  dining: '用餐中', cleaning: '待清台', maintenance: '维护中',
}

const pawColorMap: Record<string, string> = {
  free: '#10B981', reserved: '#3B82F6', occupied: '#0F766E',
  dining: '#EF4444', cleaning: '#F59E0B', maintenance: '#D1D5DB',
}

const effectiveStatus = computed(() => {
  if (props.mode === 'customer') {
    if (props.selected) return 'selected'
    if (props.table.available_for_slot === false) return 'occupied'
    return 'free'
  }
  return props.table.status || 'free'
})

const statusColorHex = computed(() => statusColorMap[effectiveStatus.value] || '#10B981')
const pawColor = computed(() => pawColorMap[effectiveStatus.value] || '#10B981')
const statusLabel = computed(() => statusLabelMap[effectiveStatus.value] || '空闲')

const wrapClass = computed(() => {
  const cls: Record<string, boolean> = {}
  if (props.mode === 'customer') {
    cls['fp-occupied'] = props.table.available_for_slot === false
    cls['fp-selected'] = props.selected
  } else {
    cls['fp-status-' + (props.table.status || 'free')] = true
    cls['fp-selected'] = props.selected
    cls['fp-has-paw'] = !!props.table.cat_zone
  }
  return cls
})
</script>

<style>
/* ═══════════════════════════════════════
   桌位 — 建筑平面图风格（全局样式）
   ═══════════════════════════════════════ */

.fp-table {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  padding: var(--space-xs) var(--space-xs);
  border-radius: var(--radius-md);
}
.fp-table:hover {
  transform: translateY(-2px);
  z-index: 2;
}
.fp-table:hover .fp-surface {
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
}
.fp-table.fp-selected .fp-surface {
  box-shadow: 0 0 0 3px rgba(15,118,110,0.5), 0 4px 16px rgba(0,0,0,0.12);
}

/* ── 座椅圆点 ── */
.fp-seats-row { display: flex; gap: 6px; justify-content: center; }
.fp-body-row { display: flex; align-items: center; gap: 2px; }
.fp-seats-col { display: flex; flex-direction: column; gap: 6px; }
.fp-seat {
  width: 10px; height: 10px; border-radius: 50%;
  background: #d1d5db; border: 1.5px solid #b0b8c4;
  flex-shrink: 0; transition: all 0.15s;
}
.fp-table:hover .fp-seat { background: #c8ccd2; }
.fp-table.fp-selected .fp-seat { background: #a7f3d0; border-color: #5eead4; }

/* ── 桌面本体 ── */
.fp-surface {
  position: relative;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 2px solid transparent;
  transition: all 0.2s ease;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1), inset 0 1px 0 rgba(255,255,255,0.6);
}

/* 桌面尺寸 */
.fp-sm { width: 72px;  height: 56px; }
.fp-md { width: 96px;  height: 64px; }
.fp-lg { width: 128px; height: 72px; }
.fp-xl { width: 152px; height: 80px; }

/* 桌面内容 */
.fp-code { font-weight: 800; font-size: 16px; line-height: 1.1; }
.fp-lg .fp-code, .fp-xl .fp-code { font-size: 19px; }
.fp-seats-label { font-size: 10px; opacity: 0.7; font-weight: 600; line-height: 1; }

/* 状态小圆点 */
.fp-status-dot {
  width: 6px; height: 6px; border-radius: 50%;
  position: absolute; top: 4px; right: 4px;
}

/* 猫爪 */
.fp-paw { position: absolute; top: 2px; left: 3px; opacity: 0.25; }
.fp-paw-svg { width: 16px; height: 16px; }
.fp-lg .fp-paw-svg, .fp-xl .fp-paw-svg { width: 20px; height: 20px; }

/* 状态标签 */
.fp-tag {
  font-size: 10px; font-weight: 600;
  padding: 1px 8px; border-radius: 99px;
  line-height: 1.4; white-space: nowrap;
  position: absolute; bottom: -18px;
  left: 50%; transform: translateX(-50%);
  color: #fff;
}

/* 预约信息 */
.fp-res {
  font-size: 9px; text-align: center;
  padding: 1px 4px; background: rgba(0,0,0,0.08);
  border-radius: 4px; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
  max-width: 130px;
  position: absolute; bottom: -32px;
  left: 50%; transform: translateX(-50%);
}

/* ═══════════════════════════════════════
   状态色 — 店员端 (status-*)
   ═══════════════════════════════════════ */
.fp-status-free .fp-surface {
  background: linear-gradient(135deg, #bbf7d0 0%, #86efac 100%);
  color: #14532d; border-color: #4ade80; border-width: 2.5px;
}
.fp-status-free .fp-seat { background: #86efac; border-color: #4ade80; }
.fp-tag-free { background: #16a34a; }

.fp-status-reserved .fp-surface {
  background: linear-gradient(135deg, #bfdbfe 0%, #93c5fd 100%);
  color: #1e3a5f; border-color: #60a5fa; border-width: 2.5px;
}
.fp-status-reserved .fp-seat { background: #93c5fd; border-color: #60a5fa; }
.fp-tag-reserved { background: #2563eb; }

.fp-status-occupied .fp-surface {
  background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
  color: #064e3b; border-color: #34d399; border-width: 2.5px;
}
.fp-status-occupied .fp-seat { background: #6ee7b7; border-color: #34d399; }
.fp-tag-occupied { background: #0f766e; }

.fp-status-dining .fp-surface {
  background: linear-gradient(135deg, #fecaca 0%, #fca5a5 100%);
  color: #7f1d1d; border-color: #f87171; border-width: 2.5px;
}
.fp-status-dining .fp-seat { background: #fca5a5; border-color: #f87171; }
.fp-tag-dining { background: #c2410c; }

.fp-status-cleaning .fp-surface {
  background: linear-gradient(135deg, #fde68a 0%, #fcd34d 100%);
  color: #78350f; border-color: #fbbf24; border-width: 2.5px;
}
.fp-status-cleaning .fp-seat { background: #fcd34d; border-color: #fbbf24; }
.fp-tag-cleaning { background: #b7791f; }

.fp-status-maintenance .fp-surface {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  color: #4b5563; border-color: #9ca3af; opacity: 0.7;
}
.fp-status-maintenance .fp-seat { background: #d1d5db; border-color: #9ca3af; opacity: 0.6; }
.fp-tag-maintenance { background: #6b7280; }

/* ═══════════════════════════════════════
   状态色 — 顾客端 (free/occupied/selected)
   ═══════════════════════════════════════ */
.fp-table:not([class*="fp-status-"]) .fp-surface {
  background: linear-gradient(135deg, #bbf7d0 0%, #86efac 100%);
  color: #14532d; border-color: #4ade80; border-width: 2.5px;
}
.fp-table:not([class*="fp-status-"]) .fp-seat {
  background: #86efac; border-color: #4ade80;
}
.fp-table:not([class*="fp-status-"]):hover .fp-seat { background: #4ade80; }
.fp-table:not([class*="fp-status-"]).fp-selected .fp-surface {
  background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
  border-color: #0f766e; color: #064e3b;
}
.fp-table:not([class*="fp-status-"]).fp-selected .fp-seat {
  background: #0f766e; border-color: #0f766e;
}
.fp-table:not([class*="fp-status-"]).fp-occupied {
  opacity: 0.5; cursor: not-allowed;
}
.fp-table:not([class*="fp-status-"]).fp-occupied .fp-surface {
  background: linear-gradient(135deg, #f5f7f3 0%, #d9e2dc 100%);
  border-color: #d9e2dc; color: #667085;
}
.fp-table:not([class*="fp-status-"]).fp-occupied .fp-seat {
  background: #d9e2dc; border-color: #b0b8c4; opacity: 0.4;
}
</style>
