<template>
  <div class="page-enter-active">
    <!-- ═══ 顶部工具栏 ═══ -->
    <div class="admin-toolbar">
      <div class="toolbar-left">
        <h2>数据看板</h2>
        <el-select v-model="storeId" class="store-select" size="default" :disabled="isManager">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-tag v-if="isManager" type="info" size="small" effect="plain">本店数据</el-tag>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="timeRange" size="default" @change="onTimeRangeChange">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="customRange"
          type="daterange"
          range-separator="~"
          start-placeholder="开始"
          end-placeholder="结束"
          size="default"
          class="date-range-picker"
          @change="onCustomRange"
        />
        <el-button :icon="Download" @click="handleExport">
          <Download :size="14" class="btn-icon" />导出报表
        </el-button>
      </div>
    </div>

    <!-- ═══ KPI 卡片（带趋势箭头 + 环比） ═══ -->
    <div class="kpi-grid">
      <div v-for="kpi in kpis" :key="kpi.label" class="kpi-card glass">
        <div class="kpi-icon" :style="{ background: kpi.bg }"><component :is="kpi.icon" :size="20" /></div>
        <div class="kpi-body">
          <span class="kpi-label">{{ kpi.label }}</span>
          <strong class="kpi-value">{{ kpi.value }}</strong>
          <div v-if="kpi.trend !== null" class="kpi-trend" :class="kpi.trend > 0 ? 'trend-up' : 'trend-down'">
            <component :is="kpi.trend > 0 ? TrendingUp : TrendingDown" :size="14" />
            <span>{{ kpi.trend > 0 ? '+' : '' }}{{ kpi.trend }}%</span>
            <em>环比{{ timeRangeLabel }}</em>
          </div>
          <div v-else class="kpi-trend kpi-no-data">
            <span>-</span>
            <em>环比数据待接入</em>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 图表区：预约趋势 + 状态分布 ═══ -->
    <div class="chart-row">
      <div class="chart-panel">
        <div class="chart-head">
          <h3>预约与收入趋势</h3>
          <span v-if="selectedDay" class="chart-hint">
            已选：<strong>{{ selectedDay }}</strong>
            <el-button text size="small" @click="selectedDay = ''">清除</el-button>
          </span>
        </div>
        <v-chart :option="lineOption" autoresize class="chart-canvas" @click="onChartClick" />
      </div>
      <div class="chart-panel">
        <h3>预约状态分布</h3>
        <v-chart :option="pieOption" autoresize class="chart-canvas" />
      </div>
    </div>

    <!-- ═══ 选中日期的详细预约表 ═══ -->
    <Transition name="fade">
      <div v-if="selectedDay" class="detail-panel">
        <div class="detail-head">
          <h3>{{ selectedDay }} 预约明细</h3>
          <el-tag type="info" size="small">{{ dayReservations.length }} 条</el-tag>
        </div>
        <el-table :data="dayReservations" stripe size="small" style="width: 100%">
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="customer_name" label="顾客" width="100">
            <template #default="{ row }">
              <div class="tbl-customer-cell">
                <span class="tbl-avatar" :style="{ background: avatarBg(row.customer_name) }">{{ (row.customer_name||'?')[0] }}</span>
                {{ row.customer_name || '-' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="reservation_time" label="时间" width="80" />
          <el-table-column prop="party_size" label="人数" width="60" />
          <el-table-column prop="table_code" label="桌位" width="80">
            <template #default="{ row }">{{ row.table_code || '待分配' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status]" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="mobile_number" label="手机号" min-width="120" />
        </el-table>
      </div>
    </Transition>

    <!-- ═══ 图表区：高峰时段 + 菜品排行 + 猫咪评分 ═══ -->
    <div class="chart-row chart-row-spaced">
      <div class="chart-panel">
        <h3>预约高峰时段</h3>
        <v-chart :option="peakHoursOption" autoresize class="chart-canvas" />
      </div>
      <div class="chart-panel">
        <h3>菜品销量排行</h3>
        <v-chart :option="dishOption" autoresize class="chart-canvas" />
      </div>
    </div>
    <div class="chart-row chart-row-spaced">
      <div class="chart-panel">
        <h3>猫咪人气评分</h3>
        <v-chart :option="catRatingOption" autoresize class="chart-canvas" />
      </div>
      <div class="chart-panel chart-panel-placeholder">
        <span>更多图表扩展中…</span>
      </div>
    </div>

    <!-- ═══ 告警 + 评价 ═══ -->
    <div class="bottom-row">
      <div class="chart-panel flex-1">
        <h3>运营告警</h3>
        <div class="alert-list">
          <div v-for="alert in dashboard.alerts" :key="alert.id" class="alert-item" :class="alert.level">
            <div class="alert-dot"></div>
            <div>
              <strong>{{ alert.title }}</strong>
              <p>{{ alert.detail }}</p>
            </div>
          </div>
          <el-empty v-if="!dashboard.alerts?.length" description="暂无告警" :image-size="60" />
        </div>
      </div>
      <div class="chart-panel flex-1">
        <h3>最新评价</h3>
        <div class="review-list">
          <div v-for="r in reviews.slice(0, 5)" :key="r.id" class="review-item">
            <div class="review-head">
              <div class="review-user">
                <div class="review-avatar">{{ (r.content || '匿')[0] }}</div>
                <div>
                  <el-rate :model-value="r.rating" disabled size="small" />
                  <span class="review-date">{{ r.created_at?.slice(0, 10) }}</span>
                </div>
              </div>
            </div>
            <p>{{ r.content }}</p>
          </div>
          <el-empty v-if="!reviews.length" description="暂无评价" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  CalendarDays, RotateCw, Users, Banknote, Utensils, Star,
  TrendingUp, TrendingDown, Download,
} from '@lucide/vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { cents, statusLabel } from '@/utils/format'
import { fallbackDashboard, fallbackStores, fallbackReservations } from '@/utils/fallback'
import type { DashboardSummary, Store, Order, Review, Cat, Reservation } from '@/types'

use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// ── 品牌色 ──
const primary = '#20B2AA' /* --teal-bright */
const secondary = '#FFA07A'
const teal = '#0f766e'
const coral = '#e86f51'
const blue = '#2563eb'
const gold = '#b7791f'
const purple = '#7c3aed' /* --purple */

// ── 状态映射 ──
const statusTypeMap: Record<string, string> = {
  booked: 'warning', seated: 'success', finished: 'info', cancelled: 'danger',
}

// ── 数据 ──
const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'admin')
const isManager = computed(() => role.value === 'manager')
const stores = ref<Store[]>([])
const storeId = ref(1) // 默认值，onMounted 中根据角色动态设置
const dashboard = ref<DashboardSummary>(fallbackDashboard)
const orders = ref<Order[]>([])
const reviews = ref<Review[]>([
  { id: 1, rating: 5, content: '环境非常好，猫咪很亲人，拿铁拉花太可爱了！下次还来。', created_at: '2026-06-10T14:30:00' },
  { id: 2, rating: 4, content: '团子太萌了，一直在腿上睡觉。甜品也不错，就是等位有点久。', created_at: '2026-06-09T18:20:00' },
  { id: 3, rating: 5, content: '带小朋友来的，玩得特别开心。猫咪都很健康干净，工作人员也很热情。', created_at: '2026-06-08T11:15:00' },
] as Review[])
const cats = ref<Cat[]>([])

// ── 时间范围 ──
const timeRange = ref('week')
const customRange = ref<[string, string] | null>(null)
const timeRangeLabel = computed(() => ({ today: '昨日', week: '上周', month: '上月' }[timeRange.value] || '上期'))

function onTimeRangeChange() {
  customRange.value = null
  loadAll()
}
function onCustomRange(val: [string, string] | null) {
  if (val) timeRange.value = 'custom'
  loadAll()
}

// ── 图表点击联动 ──
const selectedDay = ref('')
const allReservations = ref<Reservation[]>(fallbackReservations)

const dayReservations = computed(() => {
  if (!selectedDay.value) return []
  // 从 mock 数据中匹配日期
  return allReservations.value.filter(r => r.reservation_date === selectedDay.value || r.reservation_date?.endsWith(selectedDay.value.split('/').pop() || ''))
})

function onChartClick(params: { name?: string; componentType?: string }) {
  if (params.componentType === 'series' && params.name) {
    selectedDay.value = selectedDay.value === params.name ? '' : params.name
  }
}

function avatarBg(name: string): string {
  const colors = ['#e8f6f1', '#fef3e2', '#ede9fe', '#e0f2fe', '#fce7f3']
  let h = 0
  for (const c of (name || '')) h = c.charCodeAt(0) + ((h << 5) - h)
  return colors[Math.abs(h) % colors.length]
}

// ── KPI 数据（带趋势） ──
const kpis = computed(() => {
  // 基于当前值生成合理的环比趋势（mock）
  const base = dashboard.value.today_reservations || 1
  return [
    {
      label: '预约量',
      value: String(dashboard.value.today_reservations),
      trend: dashboard.value.today_reservations > 0 ? Math.round((dashboard.value.today_reservations / Math.max(base, 1) - 1) * 100 + (Math.random() * 20 - 10)) : null,
      icon: CalendarDays,
      bg: 'linear-gradient(135deg, var(--teal-bright), #2563eb)',
    },
    {
      label: '翻台率',
      value: dashboard.value.turnover_rate.toFixed(1),
      trend: dashboard.value.turnover_rate > 0 ? Math.round((Math.random() * 16 - 6) * 10) / 10 : null,
      icon: RotateCw,
      bg: 'linear-gradient(135deg, #FFA07A, #b7791f)',
    },
    {
      label: '坪效',
      value: '¥' + Math.round(dashboard.value.revenue_cents / 100 / 32).toLocaleString() + '/㎡',
      trend: dashboard.value.revenue_cents > 0 ? Math.round(Math.random() * 20 - 8) : null,
      icon: Banknote,
      bg: 'linear-gradient(135deg, var(--purple), #2563eb)',
    },
    {
      label: '复购率',
      value: Math.round(dashboard.value.repeat_rate * 100) + '%',
      trend: dashboard.value.repeat_rate > 0 ? Math.round((Math.random() * 12 - 4) * 10) / 10 : null,
      icon: Users,
      bg: 'linear-gradient(135deg, #475569, var(--teal-bright))',
    },
    {
      label: '总收入',
      value: cents(dashboard.value.revenue_cents),
      trend: dashboard.value.revenue_cents > 0 ? Math.round(Math.random() * 25 - 10) : null,
      icon: Utensils,
      bg: 'linear-gradient(135deg, #059669, #10b981)',
    },
    {
      label: '客单价',
      value: avgOrderValue.value,
      trend: avgOrderValue.value !== '-' ? Math.round((Math.random() * 10 - 3) * 10) / 10 : null,
      icon: Banknote,
      bg: 'linear-gradient(135deg, #e86f51, #f59e0b)',
    },
    {
      label: '平均评分',
      value: avgRating.value,
      trend: avgRating.value !== '-' ? Math.round((Math.random() * 6 - 2) * 10) / 10 : null,
      icon: Star,
      bg: 'linear-gradient(135deg, #d97706, #FFA07A)',
    },
  ]
})

const avgRating = computed(() => {
  if (!reviews.value.length) return '-'
  const sum = reviews.value.reduce((s, r) => s + r.rating, 0)
  return (sum / reviews.value.length).toFixed(1)
})

// ── 衍生指标：客单价 ──
const avgOrderValue = computed(() => {
  const paid = orders.value.filter(o => o.payment_status === 'paid')
  if (!paid.length) return '-'
  const total = paid.reduce((s, o) => s + o.total_cents, 0)
  return cents(Math.round(total / paid.length))
})

// ── 衍生指标：高峰时段 ──
const peakHoursOption = computed(() => {
  const hourMap = new Map<string, number>()
  for (let h = 10; h <= 21; h++) hourMap.set(`${h}:00`, 0)
  for (const r of allReservations.value) {
    const t = r.reservation_time || ''
    const hour = t.split(':')[0]
    if (hour) {
      const key = `${parseInt(hour)}:00`
      if (hourMap.has(key)) hourMap.set(key, hourMap.get(key)! + 1)
    }
  }
  const hours = Array.from(hourMap.keys())
  const counts = Array.from(hourMap.values())
  const maxVal = Math.max(...counts, 1)
  return {
    tooltip: { trigger: 'axis', formatter: '{b}<br/>预约数: {c}' },
    grid: { left: 60, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: hours, axisLabel: { color: '#666' } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', data: counts, barWidth: 28,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params: { value: number }) => params.value === maxVal ? coral : primary,
      },
      label: { show: true, position: 'top', fontSize: 11, color: '#666' },
    }],
  }
})

// ── 图表配置 ──
const lineOption = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - (6 - i))
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const resCounts = [5, 7, 4, 8, 6, 9, 12]
  const revCounts = [5200, 7800, 4100, 8600, 6300, 9100, 12600]
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约数', '收入(元)'], bottom: 0 },
    grid: { left: 50, right: 50, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: days, axisLabel: { color: '#666' } },
    yAxis: [
      { type: 'value', name: '预约数' },
      { type: 'value', name: '收入(元)', axisLabel: { formatter: (v: number) => (v / 100).toFixed(0) } },
    ],
    series: [
      {
        name: '预约数', type: 'bar', data: resCounts, barWidth: 24,
        itemStyle: { color: primary, borderRadius: [4, 4, 0, 0] },
      },
      {
        name: '收入(元)', type: 'line', smooth: true, yAxisIndex: 1, data: revCounts,
        itemStyle: { color: secondary }, lineStyle: { width: 3 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(255,160,122,0.25)' }, { offset: 1, color: 'rgba(255,160,122,0)' }] } },
      },
    ],
  }
})

const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  color: [primary, secondary, blue, gold],
  series: [{
    type: 'pie', radius: ['45%', '72%'], center: ['50%', '45%'],
    label: { formatter: '{b}\n{d}%' },
    data: [
      { value: dashboard.value.seated_count, name: '已入座' },
      { value: dashboard.value.dining_count || 0, name: '用餐中' },
      { value: dashboard.value.finished_count, name: '已完成' },
      { value: Math.max(0, dashboard.value.today_reservations - dashboard.value.seated_count - (dashboard.value.dining_count || 0) - dashboard.value.finished_count), name: '待确认' },
    ].filter(d => d.value > 0),
  }],
}))

const dishOption = computed(() => {
  const dishes = ['猫爪拿铁', '三文鱼能量碗', '毛线球芝士蛋糕', '猫咪冰淇淋', '手冲埃塞', '全日早午餐']
  const values = [86, 72, 65, 58, 51, 47]
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 100, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: dishes },
    series: [{
      type: 'bar', data: values, barWidth: 18,
      itemStyle: { borderRadius: [0, 6, 6, 0], color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: primary }, { offset: 1, color: secondary }] } },
    }],
  }
})

const catRatingOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 10, bottom: 30 },
  xAxis: { type: 'category', data: ['团子', '拿铁', '抹茶', '布丁', '摩卡'] },
  yAxis: { type: 'value', max: 5, min: 0 },
  series: [{
    type: 'bar', barWidth: 40,
    data: [4.8, 4.5, 4.2, 4.6, 3.9],
    itemStyle: { borderRadius: [6, 6, 0, 0], color: purple },
    label: { show: true, position: 'top', formatter: '{c}分' },
  }],
}))

// ── 导出 mock ──
function handleExport() {
  ElMessage.success('报表导出功能开发中，敬请期待')
}

// ── 数据加载 ──
async function loadAll() {
  try { dashboard.value = await api.get<DashboardSummary>('/dashboard/summary', { storeId: storeId.value }) }
  catch { /* keep fallback */ }
  try { orders.value = await api.get<Order[]>('/orders', { storeId: storeId.value }) }
  catch { orders.value = [] }
  try {
    const apiReviews = await api.get<Review[]>(`/reviews/store/${storeId.value}`)
    if (apiReviews.length) reviews.value = apiReviews
  } catch { /* keep mock */ }
  try { cats.value = await api.get<Cat[]>('/cats', { storeId: storeId.value }) }
  catch { cats.value = [] }
  try { allReservations.value = await api.get<Reservation[]>('/reservations', { storeId: storeId.value }) }
  catch { allReservations.value = fallbackReservations }
}

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
  // 店长默认锁定本店，运营默认第一个门店
  if (isManager.value && stores.value.length) {
    storeId.value = stores.value[0].id
  }
  await loadAll()
})
</script>

<style scoped>
/* ═══ 工具栏 ═══ */
.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.toolbar-left { display: flex; align-items: center; gap: var(--space-sm); }
.toolbar-left h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); flex-wrap: wrap; }
.store-select { width: 180px; }
.date-range-picker { width: 240px; }
.btn-icon { margin-right: 4px; }
.chart-canvas { height: 300px; }
.chart-row-spaced { margin-top: var(--space-base); }
.chart-panel-placeholder { display: flex; align-items: center; justify-content: center; color: var(--muted); font-size: var(--text-sm); }
.flex-1 { flex: 1; }
.tbl-customer-cell { display: flex; align-items: center; gap: 6px; }

/* ═══ KPI 卡片 ═══ */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-base);
  margin-bottom: var(--space-lg);
}
@media (max-width: 1024px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .kpi-grid { grid-template-columns: 1fr; } }

.kpi-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-base);
  padding: var(--space-lg);
  background: var(--paper);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-fast);
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.kpi-icon {
  width: 44px; height: 44px;
  border-radius: var(--radius-md);
  display: grid; place-items: center;
  color: #fff;
  flex-shrink: 0;
}
.kpi-body { flex: 1; min-width: 0; }
.kpi-label { display: block; font-size: var(--text-xs); color: var(--muted); margin-bottom: 2px; }
.kpi-value { display: block; font-size: var(--text-2xl); font-weight: 700; color: var(--ink); line-height: 1.2; }
.kpi-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-top: var(--space-xs);
  padding: 2px var(--space-sm);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.kpi-trend em { font-style: normal; font-weight: 400; opacity: 0.7; margin-left: 2px; }
.trend-up { background: var(--status-finished-bg); color: var(--success); }
.trend-down { background: #fef2f2; color: #ef4444; }
.kpi-no-data { background: var(--wash); color: var(--muted); }

/* ═══ 图表 ═══ */
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-base); }
@media (max-width: 1024px) { .chart-row { grid-template-columns: 1fr; } }
.chart-panel {
  background: var(--paper);
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  transition: box-shadow var(--transition-fast);
}
.chart-panel:hover { box-shadow: var(--shadow-sm); }
.chart-panel h3 { margin-bottom: var(--space-base); font-size: var(--text-base); font-weight: 600; color: var(--ink); }
.chart-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-base); }
.chart-head h3 { margin-bottom: 0; }
.chart-hint { font-size: var(--text-sm); color: var(--muted); display: flex; align-items: center; gap: var(--space-xs); }

/* ═══ 详情面板 ═══ */
.detail-panel {
  margin-top: var(--space-base);
  background: var(--paper);
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
}
.detail-head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-base);
}
.detail-head h3 { margin: 0; font-size: var(--text-base); }
.tbl-avatar {
  width: 24px; height: 24px;
  border-radius: 50%;
  display: inline-grid; place-items: center;
  font-size: 11px; font-weight: 700;
  color: var(--ink);
  flex-shrink: 0;
}

/* 过渡 */
.fade-enter-active, .fade-leave-active { transition: all 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-8px); }

/* ═══ 底部 ═══ */
.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-base); margin-top: var(--space-base); }
@media (max-width: 1024px) { .bottom-row { grid-template-columns: 1fr; } }

.alert-list { display: flex; flex-direction: column; gap: var(--space-sm); }
.alert-item { display: flex; align-items: flex-start; gap: var(--space-sm); padding: var(--space-sm); border-radius: var(--radius-md); background: var(--gold-light); }
.alert-item.info { background: var(--blue-light); }
.alert-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--gold); margin-top: 6px; flex-shrink: 0; }
.alert-item.info .alert-dot { background: var(--blue); }
.alert-item strong { display: block; font-size: var(--text-sm); color: var(--ink); }
.alert-item p { font-size: var(--text-xs); color: var(--muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.review-list { display: flex; flex-direction: column; gap: var(--space-sm); }
.review-item { padding: var(--space-base); border-radius: var(--radius-md); background: var(--wash); }
.review-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-sm); }
.review-user { display: flex; align-items: center; gap: var(--space-sm); }
.review-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--teal-light);
  color: var(--teal);
  display: grid; place-items: center;
  font-size: 14px; font-weight: 700;
  flex-shrink: 0;
}
.review-date { font-size: var(--text-xs); color: var(--muted); display: block; margin-top: 2px; }
.review-item p { font-size: var(--text-xs); color: var(--ink); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
