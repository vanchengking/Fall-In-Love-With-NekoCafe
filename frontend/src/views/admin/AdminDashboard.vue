<template>
  <div>
    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 24px">
      <h2 style="margin: 0">数据看板</h2>
      <el-select v-model="storeId" @change="loadAll" style="width: 200px">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-row">
      <div class="kpi-card gradient-1">
        <div class="kpi-icon"><CalendarDays :size="22" /></div>
        <div><span>今日预约</span><strong>{{ dashboard.today_reservations }}</strong></div>
      </div>
      <div class="kpi-card gradient-2">
        <div class="kpi-icon"><RotateCw :size="22" /></div>
        <div><span>翻台率</span><strong>{{ dashboard.turnover_rate }}</strong></div>
      </div>
      <div class="kpi-card gradient-3">
        <div class="kpi-icon"><Users :size="22" /></div>
        <div><span>复购率</span><strong>{{ Math.round(dashboard.repeat_rate * 100) }}%</strong></div>
      </div>
      <div class="kpi-card gradient-4">
        <div class="kpi-icon"><Banknote :size="22" /></div>
        <div><span>总收入</span><strong>{{ cents(dashboard.revenue_cents) }}</strong></div>
      </div>
      <div class="kpi-card gradient-5">
        <div class="kpi-icon"><Utensils :size="22" /></div>
        <div><span>总订单</span><strong>{{ orders.length }}</strong></div>
      </div>
      <div class="kpi-card gradient-6">
        <div class="kpi-icon"><Star :size="22" /></div>
        <div><span>平均评分</span><strong>{{ avgRating }}</strong></div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="chart-panel">
        <h3>预约与收入趋势</h3>
        <v-chart :option="lineOption" autoresize style="height: 300px" />
      </div>
      <div class="chart-panel">
        <h3>预约状态分布</h3>
        <v-chart :option="pieOption" autoresize style="height: 300px" />
      </div>
    </div>

    <div class="chart-row" style="margin-top: 16px">
      <div class="chart-panel">
        <h3>菜品销量排行</h3>
        <v-chart :option="dishOption" autoresize style="height: 300px" />
      </div>
      <div class="chart-panel">
        <h3>猫咪人气评分</h3>
        <v-chart :option="catRatingOption" autoresize style="height: 300px" />
      </div>
    </div>

    <!-- 告警 + 评价 -->
    <div class="bottom-row">
      <div class="chart-panel" style="flex: 1">
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
      <div class="chart-panel" style="flex: 1">
        <h3>最新评价</h3>
        <div class="review-list">
          <div v-for="r in reviews.slice(0, 5)" :key="r.id" class="review-item">
            <div class="review-head">
              <el-rate :model-value="r.rating" disabled size="small" />
              <span class="review-date">{{ r.created_at?.slice(0, 10) }}</span>
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
import { CalendarDays, RotateCw, Users, Banknote, Utensils, Star } from '@lucide/vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { fallbackDashboard, fallbackStores } from '@/utils/fallback'
import type { DashboardSummary, Store, Order, Review, Cat } from '@/types'

use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const stores = ref<Store[]>([])
const storeId = ref(1)
const dashboard = ref<DashboardSummary>(fallbackDashboard)
const orders = ref<Order[]>([])
const reviews = ref<Review[]>([])
const cats = ref<Cat[]>([])

const teal = '#0f766e'
const coral = '#e86f51'
const blue = '#2563eb'
const gold = '#b7791f'
const purple = '#7c3aed'
const slate = '#475569'

const avgRating = computed(() => {
  if (!reviews.value.length) return '-'
  const sum = reviews.value.reduce((s, r) => s + r.rating, 0)
  return (sum / reviews.value.length).toFixed(1)
})

const lineOption = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - (6 - i))
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const resCounts = days.map((_, i) => Math.floor(Math.random() * 4 + 2 + i * 0.3))
  const revCounts = days.map((_, i) => Math.floor(Math.random() * 3000 + 5000 + i * 500))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约数', '收入(元)'], bottom: 0 },
    grid: { left: 50, right: 50, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: days },
    yAxis: [
      { type: 'value', name: '预约数' },
      { type: 'value', name: '收入(元)', axisLabel: { formatter: (v: number) => (v / 100).toFixed(0) } },
    ],
    series: [
      { name: '预约数', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: resCounts, itemStyle: { color: teal } },
      { name: '收入(元)', type: 'line', smooth: true, yAxisIndex: 1, data: revCounts, itemStyle: { color: coral } },
    ],
  }
})

const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['45%', '72%'], center: ['50%', '45%'],
    label: { formatter: '{b}\n{d}%' },
    data: [
      { value: dashboard.value.seated_count, name: '已入座', itemStyle: { color: teal } },
      { value: dashboard.value.dining_count || 0, name: '用餐中', itemStyle: { color: gold } },
      { value: dashboard.value.finished_count, name: '已完成', itemStyle: { color: blue } },
      { value: Math.max(0, dashboard.value.today_reservations - dashboard.value.seated_count - (dashboard.value.dining_count || 0) - dashboard.value.finished_count), name: '待确认', itemStyle: { color: coral } },
    ].filter(d => d.value > 0),
  }],
}))

const dishOption = computed(() => {
  const dishMap: Record<string, number> = {}
  orders.value.forEach(o => { dishMap[`订单#${o.id}`] = o.total_cents / 100 })
  const keys = Object.keys(dishMap).slice(0, 8)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    yAxis: { type: 'category', data: keys.reverse() },
    series: [{
      type: 'bar', data: keys.map(k => dishMap[k]).reverse(), barWidth: 18,
      itemStyle: { borderRadius: [0, 6, 6, 0], color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: teal }, { offset: 1, color: coral }] } },
    }],
  }
})

const catRatingOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 10, bottom: 30 },
  xAxis: { type: 'category', data: cats.value.map(c => c.name) },
  yAxis: { type: 'value', max: 5, min: 0 },
  series: [{
    type: 'bar', barWidth: 40,
    data: cats.value.map(c => {
      const catReviews = reviews.value.filter(r => r.cat_id === c.id)
      return catReviews.length ? +(catReviews.reduce((s, r) => s + r.rating, 0) / catReviews.length).toFixed(1) : 0
    }),
    itemStyle: { borderRadius: [6, 6, 0, 0], color: purple },
    label: { show: true, position: 'top', formatter: '{c}分' },
  }],
}))

async function loadAll() {
  try { dashboard.value = await api.get<DashboardSummary>('/dashboard/summary', { storeId: storeId.value }) }
  catch { /* keep fallback */ }
  try { orders.value = await api.get<Order[]>('/orders', { storeId: storeId.value }) }
  catch { orders.value = [] }
  try { reviews.value = await api.get<Review[]>('/reviews', { storeId: storeId.value }) }
  catch { reviews.value = [] }
  try { cats.value = await api.get<Cat[]>('/cats', { storeId: storeId.value }) }
  catch { cats.value = [] }
}

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
  await loadAll()
})
</script>

<style scoped>
.kpi-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; margin-bottom: 24px; }
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 600px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }
.kpi-card { display: flex; align-items: center; gap: 14px; padding: 18px; border-radius: 14px; color: #fff; }
.kpi-icon { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; background: rgba(255,255,255,0.2); flex-shrink: 0; }
.kpi-card span { display: block; font-size: 12px; opacity: 0.86; }
.kpi-card strong { display: block; font-size: 24px; margin-top: 2px; }
.gradient-1 { background: linear-gradient(135deg, #0f766e, #2563eb); }
.gradient-2 { background: linear-gradient(135deg, #e86f51, #b7791f); }
.gradient-3 { background: linear-gradient(135deg, #475569, #0f766e); }
.gradient-4 { background: linear-gradient(135deg, #7c3aed, #2563eb); }
.gradient-5 { background: linear-gradient(135deg, #059669, #10b981); }
.gradient-6 { background: linear-gradient(135deg, #d97706, #f59e0b); }
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 900px) { .chart-row { grid-template-columns: 1fr; } }
.chart-panel { background: #fff; padding: 20px; border-radius: 14px; border: 1px solid #e8e5df; }
.chart-panel h3 { margin-bottom: 14px; font-size: 15px; font-weight: 600; color: #172033; }
.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
@media (max-width: 900px) { .bottom-row { grid-template-columns: 1fr; } }
.alert-list { display: flex; flex-direction: column; gap: 10px; }
.alert-item { display: flex; align-items: flex-start; gap: 10px; padding: 12px; border-radius: 10px; background: #fff7ed; }
.alert-item.info { background: #eff6ff; }
.alert-dot { width: 8px; height: 8px; border-radius: 50%; background: #f59e0b; margin-top: 6px; flex-shrink: 0; }
.alert-item.info .alert-dot { background: #3b82f6; }
.alert-item strong { display: block; font-size: 14px; color: #172033; }
.alert-item p { font-size: 13px; color: #667085; margin-top: 2px; }
.review-list { display: flex; flex-direction: column; gap: 10px; }
.review-item { padding: 12px; border-radius: 10px; background: #f8f9fa; }
.review-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.review-date { font-size: 12px; color: #667085; }
.review-item p { font-size: 13px; color: #172033; line-height: 1.5; }
</style>
