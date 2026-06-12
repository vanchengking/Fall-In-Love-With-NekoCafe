<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h2>总部运营看板</h2>
        <p>查看跨门店经营数据、趋势和预警。</p>
      </div>
      <el-select
        v-model="selectedStoreId"
        placeholder="选择门店"
        clearable
        style="width: 240px"
        @change="loadAll"
      >
        <el-option label="全部门店" :value="undefined" />
        <el-option
          v-for="store in stores"
          :key="store.id"
          :label="store.name"
          :value="store.id"
        />
      </el-select>
    </div>

    <div class="kpi-grid">
      <section class="kpi-card accent-teal">
        <span>今日预约</span>
        <strong>{{ summary.today_reservations }}</strong>
      </section>
      <section class="kpi-card accent-amber">
        <span>翻台率</span>
        <strong>{{ summary.turnover_rate }}</strong>
      </section>
      <section class="kpi-card accent-blue">
        <span>复购率</span>
        <strong>{{ formatPercent(summary.repeat_rate) }}</strong>
      </section>
      <section class="kpi-card accent-rose">
        <span>总收入</span>
        <strong>{{ cents(summary.revenue_cents) }}</strong>
      </section>
      <section class="kpi-card accent-slate">
        <span>到店中</span>
        <strong>{{ summary.seated_count + summary.dining_count }}</strong>
      </section>
      <section class="kpi-card accent-gold">
        <span>独立顾客</span>
        <strong>{{ summary.unique_customers }}</strong>
      </section>
    </div>

    <div class="chart-row">
      <section class="panel">
        <div class="panel-head">
          <h3>预约与收入趋势</h3>
          <el-tag size="small" type="info">最近 7 天</el-tag>
        </div>
        <v-chart :option="trendOption" autoresize style="height: 320px" />
      </section>
      <section class="panel">
        <div class="panel-head">
          <h3>预约状态分布</h3>
          <el-tag size="small" type="success">实时汇总</el-tag>
        </div>
        <v-chart :option="statusOption" autoresize style="height: 320px" />
      </section>
    </div>

    <div class="chart-row">
      <section class="panel">
        <div class="panel-head">
          <h3>门店收入对比</h3>
          <el-tag size="small" type="warning">今日</el-tag>
        </div>
        <v-chart :option="overviewOption" autoresize style="height: 320px" />
      </section>
      <section class="panel">
        <div class="panel-head">
          <h3>运营预警</h3>
          <el-tag size="small" type="danger">{{ alerts.length }} 条</el-tag>
        </div>
        <div class="alert-list">
          <div
            v-for="alert in alerts"
            :key="alert.id"
            class="alert-item"
            :class="`level-${alert.level}`"
          >
            <strong>{{ alert.title }}</strong>
            <p>{{ alert.detail }}</p>
          </div>
          <el-empty
            v-if="alerts.length === 0"
            description="暂无运营预警"
            :image-size="72"
          />
        </div>
      </section>
    </div>

    <div class="table-row">
      <section class="panel">
        <div class="panel-head">
          <h3>门店经营概览</h3>
          <el-tag size="small" type="info">{{ storeOverview.length }} 家门店</el-tag>
        </div>
        <el-table :data="storeOverview" stripe style="width: 100%">
          <el-table-column prop="store_name" label="门店" min-width="180" />
          <el-table-column prop="table_count" label="桌位数" width="90" />
          <el-table-column prop="total_seats" label="座位数" width="90" />
          <el-table-column prop="today_reservations" label="今日预约" width="100" />
          <el-table-column prop="finished_count" label="已完成" width="90" />
          <el-table-column label="今日收入" min-width="120">
            <template #default="{ row }">
              {{ cents(row.revenue_cents) }}
            </template>
          </el-table-column>
          <el-table-column prop="turnover_rate" label="翻台率" width="90" />
        </el-table>
      </section>
    </div>

    <div class="table-row">
      <section class="panel">
        <div class="panel-head">
          <h3>最新评价</h3>
          <el-tag size="small" type="info">
            {{ selectedStoreId ? '当前门店' : '先选择门店' }}
          </el-tag>
        </div>
        <div class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-head">
              <el-rate :model-value="review.rating" disabled size="small" />
              <span>{{ formatDate(review.created_at) }}</span>
            </div>
            <p>{{ review.content || '该评价未填写文字内容。' }}</p>
          </div>
          <el-empty
            v-if="reviews.length === 0"
            :description="selectedStoreId ? '当前门店暂无评价' : '选择门店后查看评价'"
            :image-size="72"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { cents } from '@/utils/format'
import http from '@/utils/http'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

interface StoreOption {
  id: number
  name: string
}

interface DashboardSummary {
  today_reservations: number
  seated_count: number
  dining_count: number
  finished_count: number
  unique_customers: number
  revenue_cents: number
  turnover_rate: number
  repeat_rate: number
}

interface DashboardAlert {
  id: number
  level: string
  title: string
  detail: string
}

interface ReservationTrendPoint {
  biz_date: string
  reservation_count: number
  finished_count: number
  active_count: number
}

interface RevenueTrendPoint {
  biz_date: string
  order_count: number
  revenue_cents: number
}

interface StoreOverviewRow {
  store_id: number
  store_name: string
  table_count: number
  total_seats: number
  today_reservations: number
  finished_count: number
  revenue_cents: number
  turnover_rate: number
}

interface ReviewRow {
  id: number
  rating: number
  content: string | null
  created_at?: string
}

const stores = ref<StoreOption[]>([])
const selectedStoreId = ref<number | undefined>(undefined)
const summary = ref<DashboardSummary>({
  today_reservations: 0,
  seated_count: 0,
  dining_count: 0,
  finished_count: 0,
  unique_customers: 0,
  revenue_cents: 0,
  turnover_rate: 0,
  repeat_rate: 0,
})
const alerts = ref<DashboardAlert[]>([])
const reservationTrend = ref<ReservationTrendPoint[]>([])
const revenueTrend = ref<RevenueTrendPoint[]>([])
const storeOverview = ref<StoreOverviewRow[]>([])
const reviews = ref<ReviewRow[]>([])

function formatPercent(value: number) {
  return `${Math.round(value * 100)}%`
}

function formatDate(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function buildTrendLabels() {
  const labels = new Set<string>()
  for (const row of reservationTrend.value) labels.add(row.biz_date)
  for (const row of revenueTrend.value) labels.add(row.biz_date)
  return Array.from(labels).sort()
}

const trendOption = computed(() => {
  const labels = buildTrendLabels()
  const reservationMap = new Map(reservationTrend.value.map((row) => [row.biz_date, row.reservation_count]))
  const revenueMap = new Map(revenueTrend.value.map((row) => [row.biz_date, row.revenue_cents / 100]))

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约量', '收入'], bottom: 0 },
    grid: { left: 48, right: 40, top: 18, bottom: 48 },
    xAxis: { type: 'category', data: labels },
    yAxis: [
      { type: 'value', name: '预约量' },
      { type: 'value', name: '收入(元)' },
    ],
    series: [
      {
        name: '预约量',
        type: 'line',
        smooth: true,
        data: labels.map((label) => reservationMap.get(label) ?? 0),
        itemStyle: { color: '#0f766e' },
        areaStyle: { opacity: 0.12 },
      },
      {
        name: '收入',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: labels.map((label) => revenueMap.get(label) ?? 0),
        itemStyle: { color: '#d97706' },
      },
    ],
  }
})

const statusOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '42%'],
      data: [
        { name: '已入座', value: summary.value.seated_count, itemStyle: { color: '#0f766e' } },
        { name: '用餐中', value: summary.value.dining_count, itemStyle: { color: '#2563eb' } },
        { name: '已完成', value: summary.value.finished_count, itemStyle: { color: '#d97706' } },
        {
          name: '待处理',
          value: Math.max(
            0,
            summary.value.today_reservations
              - summary.value.seated_count
              - summary.value.dining_count
              - summary.value.finished_count,
          ),
          itemStyle: { color: '#e86f51' },
        },
      ].filter((item) => item.value > 0),
    },
  ],
}))

const overviewOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 24, top: 18, bottom: 48 },
  xAxis: {
    type: 'category',
    data: storeOverview.value.map((row) => row.store_name),
    axisLabel: { interval: 0, rotate: 12 },
  },
  yAxis: { type: 'value', name: '收入(元)' },
  series: [
    {
      type: 'bar',
      data: storeOverview.value.map((row) => row.revenue_cents / 100),
      barWidth: 26,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: '#475569',
      },
    },
  ],
}))

async function loadStores() {
  stores.value = await http.get('/admin/stores')
}

async function loadSummary() {
  summary.value = await http.get('/admin/dashboard/summary', {
    params: { storeId: selectedStoreId.value },
  })
}

async function loadAlerts() {
  alerts.value = await http.get('/admin/dashboard/alerts', {
    params: { storeId: selectedStoreId.value },
  })
}

async function loadReservationTrend() {
  reservationTrend.value = await http.get('/admin/dashboard/trends/reservations', {
    params: { storeId: selectedStoreId.value, days: 7 },
  })
}

async function loadRevenueTrend() {
  revenueTrend.value = await http.get('/admin/dashboard/trends/revenue', {
    params: { storeId: selectedStoreId.value, days: 7 },
  })
}

async function loadStoreOverview() {
  storeOverview.value = await http.get('/admin/dashboard/stores-overview', {
    params: { storeId: selectedStoreId.value },
  })
}

async function loadReviews() {
  if (!selectedStoreId.value) {
    reviews.value = []
    return
  }
  reviews.value = await http.get(`/reviews/store/${selectedStoreId.value}`, {
    params: { limit: 5 },
  })
}

async function loadAll() {
  await Promise.all([
    loadSummary(),
    loadAlerts(),
    loadReservationTrend(),
    loadRevenueTrend(),
    loadStoreOverview(),
    loadReviews(),
  ])
}

onMounted(async () => {
  await loadStores()
  await loadAll()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  color: #172033;
}

.page-header p {
  margin: 6px 0 0;
  color: #667085;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  padding: 18px;
  border-radius: 16px;
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 118px;
  justify-content: center;
}

.kpi-card span {
  font-size: 13px;
  opacity: 0.88;
}

.kpi-card strong {
  font-size: 28px;
  line-height: 1;
}

.accent-teal { background: linear-gradient(135deg, #0f766e, #14b8a6); }
.accent-amber { background: linear-gradient(135deg, #b45309, #f59e0b); }
.accent-blue { background: linear-gradient(135deg, #1d4ed8, #60a5fa); }
.accent-rose { background: linear-gradient(135deg, #be123c, #fb7185); }
.accent-slate { background: linear-gradient(135deg, #334155, #64748b); }
.accent-gold { background: linear-gradient(135deg, #92400e, #fbbf24); }

.chart-row,
.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.table-row {
  grid-template-columns: 1fr;
}

.panel {
  background: #fff;
  border: 1px solid #e8e5df;
  border-radius: 16px;
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 16px;
  color: #172033;
}

.alert-list,
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item,
.review-item {
  border-radius: 12px;
  padding: 14px;
}

.alert-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.alert-item strong,
.review-item p {
  color: #172033;
}

.alert-item p,
.review-head span {
  color: #667085;
  font-size: 13px;
  margin: 6px 0 0;
}

.level-warning {
  background: #fff7ed;
  border-color: #fed7aa;
}

.level-info {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.level-danger {
  background: #fff1f2;
  border-color: #fecdd3;
}

.review-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.review-item p {
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-header,
  .chart-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .page-header {
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
