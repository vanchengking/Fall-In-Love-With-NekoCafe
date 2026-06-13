<template>
  <div class="page-enter-active">
    <!-- 顶部工具栏 -->
    <div class="staff-toolbar">
      <h2>今日预约运营工作台</h2>
      <div class="toolbar-right">
        <el-badge v-if="newCount > 0" :value="newCount" :max="99">
          <el-button size="small" type="warning" @click="clearNewCount">
            <Bell :size="14" class="btn-icon" />{{ newCount }} 条新预约
          </el-button>
        </el-badge>
        <el-button :icon="RefreshCw" circle size="small" @click="loadData" />
        <span v-if="lastUpdated" class="refresh-hint">{{ lastUpdated.toLocaleTimeString() }} 刷新</span>
      </div>
    </div>

    <!-- 6个统计卡片（点击筛选） -->
    <div class="kpi-strip">
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === null }" @click="filterStatus = null">
        <div class="kpi-icon kpi-icon-total"><CalendarDays :size="20" /></div>
        <div><span>今日预约</span><strong>{{ stats.total }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'booked' }" @click="filterStatus = 'booked'">
        <div class="kpi-icon kpi-icon-booked"><Clock :size="20" /></div>
        <div><span>待处理</span><strong>{{ stats.booked }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'seated' }" @click="filterStatus = 'seated'">
        <div class="kpi-icon kpi-icon-seated"><UserCheck :size="20" /></div>
        <div><span>已入座</span><strong>{{ stats.seated }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'dining' }" @click="filterStatus = 'dining'">
        <div class="kpi-icon kpi-icon-dining"><Utensils :size="20" /></div>
        <div><span>用餐中</span><strong>{{ stats.dining }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'cleaning' }" @click="filterStatus = 'cleaning'">
        <div class="kpi-icon kpi-icon-cleaning"><Sparkles :size="20" /></div>
        <div><span>待清台</span><strong>{{ stats.cleaning }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'revenue' }" @click="filterStatus = 'revenue'">
        <div class="kpi-icon kpi-icon-revenue"><Banknote :size="20" /></div>
        <div><span>营业额</span><strong>{{ cents(stats.revenue) }}</strong></div>
      </div>
    </div>


    <!-- 预约时间线 -->
    <section class="panel timeline-section">
      <h3>预约时间线</h3>
      <div v-if="timeSlots.length" class="timeline-scroll">
        <div class="timeline-axis">
          <div v-for="slot in timeSlots" :key="slot.time" class="timeline-slot">
            <div class="ts-time">{{ slot.time }}</div>
            <div class="ts-cards">
              <div
                v-for="r in slot.items"
                :key="r.id"
                class="ts-card"
                :class="{ active: selectedId === r.id, ['ts-' + r.status]: true }"
                @click="selectedId = r.id"
              >
                <div class="tsc-name">{{ r.customer_name || r.customerName }}</div>
                <div class="tsc-info">{{ r.table_code || '待分配' }} · {{ r.party_size }}人</div>
                <el-tag :type="statusType(r.status)" size="small" effect="plain">{{ statusLabel(r.status) }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="今日暂无预约" :image-size="60" />
    </section>

    <!-- 待处理订单 -->
    <section class="panel pending-orders-section">
      <div class="section-head">
        <h3>待处理订单</h3>
        <el-tag v-if="pendingOrders.length" type="warning" size="small" effect="dark">{{ pendingOrders.length }}</el-tag>
      </div>
      <div v-if="pendingOrders.length" class="pending-orders-grid">
        <div v-for="o in pendingOrders.slice(0, 6)" :key="o.id" class="po-card">
          <div class="po-head">
            <strong>#{{ o.id }}</strong>
            <el-tag :type="o.status === 'pending' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ o.status === 'pending' ? '待出餐' : '备餐中' }}
            </el-tag>
          </div>
          <div class="po-info">{{ o.table_code || '待分配' }} · {{ o.customer_name || '-' }}</div>
          <div class="po-items">{{ (o.items || []).length }} 道菜品</div>
        </div>
      </div>
      <el-empty v-else description="暂无待处理订单" :image-size="40" />
    </section>

    <!-- 下方两栏：当前处理 + 桌位概览 -->
    <div class="bottom-grid">
      <!-- 当前处理面板 -->
      <section class="panel process-panel">
        <h3>当前处理</h3>
        <template v-if="selected">
          <div class="process-customer">
            <div class="pc-avatar" :style="{ background: avatarBg(selected.customer_name) }">
              {{ (selected.customer_name || '?')[0] }}
            </div>
            <div>
              <strong>{{ selected.customer_name || selected.customerName }}{{ actionVerb(selected.status) }}</strong>
              <span>{{ selected.reservation_time }} · {{ selected.party_size }}人 · {{ selected.table_code || '待分配' }}</span>
              <span v-if="selected.mobile_number" class="pc-phone">{{ selected.mobile_number }}</span>
            </div>
          </div>

          <!-- 状态流转进度条 -->
          <div class="flow-bar">
            <div v-for="step in flowSteps" :key="step.key" class="flow-step" :class="flowStepClass(step.key)">
              <div class="fs-dot"></div>
              <span>{{ step.label }}</span>
            </div>
          </div>

          <!-- 运营提醒（动态计算） -->
          <div class="alerts-area">
            <div v-for="alert in dynamicAlerts" :key="alert.id" class="alert-item" :class="alert.level">
              <AlertTriangle :size="14" />
              <div><strong>{{ alert.title }}</strong><p>{{ alert.detail }}</p></div>
            </div>
            <div v-if="!dynamicAlerts.length" class="alert-ok">
              <CheckCircle2 :size="14" /> 当前无异常告警
            </div>
          </div>

          <!-- 操作按钮（按预约状态严格控制） -->
          <div class="process-actions">
            <!-- booked: 确认入座 / 标记未到 / 取消 -->
            <el-button v-if="selected.status === 'booked'" type="success" @click="confirmAction(selected, 'seated')">
              <UserCheck :size="14" class="btn-icon" />确认入座
            </el-button>
            <el-button v-if="selected.status === 'booked'" type="warning" plain @click="confirmAction(selected, 'no_show')">
              标记未到
            </el-button>
            <el-button v-if="selected.status === 'booked'" type="danger" plain @click="confirmAction(selected, 'cancelled')">
              取消预约
            </el-button>
            <!-- seated: 开始用餐 -->
            <el-button v-if="selected.status === 'seated'" type="primary" @click="confirmAction(selected, 'dining')">
              <Utensils :size="14" class="btn-icon" />开始用餐
            </el-button>
            <!-- dining: 完桌 -->
            <el-button v-if="selected.status === 'dining'" type="primary" @click="confirmAction(selected, 'finished')">
              <CheckCircle2 :size="14" class="btn-icon" />完桌
            </el-button>
            <!-- finished / cancelled / no_show: 无操作 -->
            <span v-if="['finished','cancelled','no_show'].includes(selected.status)" class="no-action-hint">该预约已结束</span>
          </div>
        </template>
        <el-empty v-else description="点击时间线预约查看详情" :image-size="60" />
      </section>

      <!-- 桌位快速概览 -->
      <section class="panel table-overview">
        <h3>桌位快速概览</h3>
        <div class="table-grid">
          <el-tooltip
            v-for="t in tables"
            :key="t.id"
            :content="`${t.code} · ${t.seats}人 · ${tableStatusLabel(t.status)}${t.cat_name ? ' · 猫区' : ''}`"
            placement="top"
            :show-after="300"
          >
            <div
              class="table-block"
              :class="'tb-' + (t.status || 'free')"
              role="button"
              tabindex="0"
              :aria-label="`桌位 ${t.code} ${tableStatusLabel(t.status)}`"
            >
              <div class="tb-code">{{ t.code }}</div>
              <div class="tb-cat" v-if="t.cat_name">{{ t.cat_name }}</div>
              <div class="tb-status">{{ tableStatusLabel(t.status) }}</div>
            </div>
          </el-tooltip>
        </div>
        <div class="table-legend">
          <span><span class="lg-dot lg-dot-booked"></span>已预约</span>
          <span><span class="lg-dot lg-dot-seated"></span>已入座</span>
          <span><span class="lg-dot lg-dot-dining"></span>用餐中</span>
          <span><span class="lg-dot lg-dot-cleaning"></span>待清台</span>
          <span><span class="lg-dot lg-dot-free"></span>空闲</span>
        </div>
      </section>
    </div>

    <!-- 二次确认弹窗 -->
    <el-dialog v-model="confirmDialog" :title="confirmTitle" width="400px">
      <p>{{ confirmMessage }}</p>
      <template #footer>
        <el-button @click="confirmDialog = false">取消</el-button>
        <el-button :type="confirmBtnType" :loading="confirmLoading" @click="executeConfirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import {
  CalendarDays, Clock, UserCheck, Utensils, Banknote, Sparkles,
  Bell, RefreshCw, CheckCircle2, AlertTriangle,
} from '@lucide/vue'
import { api } from '@/utils/http'
import { cents, statusLabel, statusType, avatarBg } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import { fallbackReservations, fallbackTables, fallbackOrders, fallbackDashboard } from '@/utils/fallback'
import type { Reservation, DiningTable, OperationAlert, Order } from '@/types'

const reservations = ref<Reservation[]>([])
const tables = ref<DiningTable[]>([])
const orders = ref<Order[]>([])
const alerts = ref<OperationAlert[]>(fallbackDashboard.alerts || [])
const selectedId = ref<number | null>(null)
const filterStatus = ref<string | null>(null)

const confirmDialog = ref(false)
const confirmLoading = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmBtnType = ref<'primary' | 'success' | 'danger'>('primary')
let pendingTarget: Reservation | null = null
let pendingAction = ''

const flowSteps = [
  { key: 'booked', label: '已预约' },
  { key: 'seated', label: '已入座' },
  { key: 'dining', label: '用餐中' },
  { key: 'finished', label: '已完成' },
]

// ── 统计卡片（实时计算） ──
const stats = computed(() => ({
  total:    reservations.value.length,
  booked:   reservations.value.filter(r => r.status === 'booked').length,
  seated:   reservations.value.filter(r => r.status === 'seated').length,
  dining:   reservations.value.filter(r => r.status === 'dining').length,
  cleaning: tables.value.filter(t => t.status === 'cleaning').length,
  revenue:  orders.value.filter(o => o.payment_status === 'paid').reduce((s, o) => s + o.total_cents, 0),
}))

// ── 待处理订单（pending + preparing） ──
const pendingOrders = computed(() =>
  orders.value.filter(o => o.status === 'pending' || o.status === 'preparing')
    .sort((a, b) => (a.created_at || '').localeCompare(b.created_at || ''))
)

// ── 动态告警（实时计算） ──
const dynamicAlerts = computed(() => {
  const result: OperationAlert[] = []
  const now = new Date()
  const currentHour = now.getHours()

  // 1. 高峰时段桌位紧张：当前时段预约数 > 空闲桌位数
  const busySlots = timeSlots.value.filter(s => {
    const slotHour = parseInt(s.time.split(':')[0])
    return slotHour >= 11 && slotHour <= 13 || slotHour >= 17 && slotHour <= 19
  })
  const peakBookings = busySlots.reduce((sum, s) => sum + s.items.length, 0)
  const freeTables = tables.value.filter(t => t.status === 'free').length
  if (peakBookings > freeTables && freeTables < 3) {
    result.push({
      id: 'peak-shortage',
      level: 'warning',
      title: '桌位紧张',
      detail: `当前时段 ${peakBookings} 组预约，仅剩 ${freeTables} 张空桌`,
    })
  }

  // 2. 超时未到：booked 状态且预约时间已过 30 分钟
  const timeStr = `${String(currentHour).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  for (const r of reservations.value) {
    if (r.status === 'booked' && r.reservation_time && r.reservation_time < timeStr) {
      const [rh, rm] = r.reservation_time.split(':').map(Number)
      const [ch, cm] = [currentHour, now.getMinutes()]
      const diffMin = (ch * 60 + cm) - (rh * 60 + rm)
      if (diffMin > 30) {
        result.push({
          id: `timeout-${r.id}`,
          level: 'warning',
          title: '预约超时未到',
          detail: `${r.customer_name || '顾客'}（${r.reservation_time}）已超时 ${diffMin} 分钟`,
        })
      }
    }
  }

  // 3. 清台积压：cleaning 状态桌位 > 2
  const cleaningCount = tables.value.filter(t => t.status === 'cleaning').length
  if (cleaningCount > 2) {
    result.push({
      id: 'cleaning-backlog',
      level: 'info',
      title: '清台积压',
      detail: `当前 ${cleaningCount} 张桌位待清洁，建议加快清台速度`,
    })
  }

  return result
})

// ── 排序 + 时间线分组（支持筛选） ──
const sortedReservations = computed(() => {
  const list = filterStatus.value && ['booked', 'seated', 'dining', 'finished', 'cancelled', 'no_show'].includes(filterStatus.value)
    ? reservations.value.filter(r => r.status === filterStatus.value)
    : reservations.value
  return [...list].sort((a, b) => (a.reservation_time || '').localeCompare(b.reservation_time || ''))
})

const timeSlots = computed(() => {
  const map = new Map<string, Reservation[]>()
  for (const r of sortedReservations.value) {
    const time = r.reservation_time || '未知'
    if (!map.has(time)) map.set(time, [])
    map.get(time)!.push(r)
  }
  return Array.from(map.entries()).map(([time, items]) => ({ time, items }))
})

const selected = computed(() => reservations.value.find(r => r.id === selectedId.value) || null)

// ── 工具函数 ──
function actionVerb(status: string): string {
  const map: Record<string, string> = { booked: ' — 待入座', seated: ' — 用餐中', dining: ' — 待完桌' }
  return map[status] || ''
}

function flowStepClass(key: string): string {
  if (!selected.value) return ''
  const order = ['booked', 'seated', 'dining', 'finished']
  const cur = order.indexOf(selected.value.status)
  const idx = order.indexOf(key)
  if (idx < cur) return 'step-done'
  if (idx === cur) return 'step-current'
  return ''
}

function tableStatusLabel(s?: string): string {
  return { free: '空闲', occupied: '已入座', reserved: '已预约', dining: '用餐中', cleaning: '待清台', maintenance: '不可用' }[s || 'free'] || '空闲'
}

// ── 操作确认 ──
function confirmAction(r: Reservation, action: string) {
  pendingTarget = r; pendingAction = action
  const name = r.customer_name || r.customerName || '顾客'
  const map: Record<string, { t: string; m: string; b: 'primary' | 'success' | 'danger' }> = {
    seated:    { t: '确认入座', m: `${name}（${r.party_size}人）→ ${r.table_code || '待分配'}`, b: 'success' },
    dining:    { t: '开始用餐', m: `确认 ${name} 开始用餐？`, b: 'primary' },
    finished:  { t: '确认完桌', m: `确认 ${name} 用餐完毕？桌位将释放。`, b: 'primary' },
    cancelled: { t: '取消预约', m: `确认取消 ${name} 的预约？`, b: 'danger' },
    no_show:   { t: '标记未到', m: `确认将 ${name} 标记为未到店？`, b: 'danger' },
  }
  const cfg = map[action] || map.cancelled
  confirmTitle.value = cfg.t; confirmMessage.value = cfg.m; confirmBtnType.value = cfg.b
  confirmDialog.value = true
}

async function executeConfirm() {
  if (!pendingTarget) return
  confirmLoading.value = true
  try {
    await api.patch(`/reservations/${pendingTarget.id}/status`, { status: pendingAction })
    ElMessage.success('操作成功')
    confirmDialog.value = false
    selectedId.value = null
    await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { confirmLoading.value = false }
}

// ── 数据加载（同时拉取预约、桌位和订单） ──
async function loadData() {
  try { reservations.value = await api.get<Reservation[]>('/reservations', { date: new Date().toISOString().slice(0, 10) }) }
  catch { reservations.value = fallbackReservations }
  try { tables.value = await api.get<DiningTable[]>('/tables', { storeId: 1 }) }
  catch { tables.value = fallbackTables }
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { orders.value = fallbackOrders }
}

// 10 秒自动刷新，同时更新时间线和桌位概览
const { lastUpdated, newCount, clearNewCount, startWithChangeDetection } = usePolling(loadData, 10000)
startWithChangeDetection(() => reservations.value.map(r => r.id).sort().join(','))

watch(newCount, (c) => {
  if (c > 0) ElNotification({ title: '新预约到达', message: `${c} 条新预约`, type: 'success', position: 'bottom-right', duration: 5000 })
})

loadData()
</script>

<style scoped>
.staff-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.staff-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.refresh-hint { font-size: var(--text-xs); color: var(--muted); }

/* ── KPI 一行 6 个 ── */
.kpi-strip {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: var(--space-sm);
  margin-bottom: var(--space-lg); padding: var(--space-base);
  background: linear-gradient(135deg, var(--teal-light), var(--gold-light)); border-radius: var(--radius-lg);
}
.kpi-card { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-sm) var(--space-base); border-radius: var(--radius-md); transition: all 0.15s; cursor: pointer; border: 2px solid transparent; }
.kpi-card:hover { transform: translateY(-2px); }
.kpi-card.kpi-active { background: var(--paper); border-color: var(--teal); box-shadow: 0 0 0 2px rgba(15,118,110,0.15), var(--shadow-sm); }
.kpi-icon { width: 40px; height: 40px; border-radius: var(--radius-md); display: grid; place-items: center; color: #fff; flex-shrink: 0; }
.kpi-icon-total { background: linear-gradient(135deg, var(--teal), var(--blue)); }
.kpi-icon-booked { background: linear-gradient(135deg, var(--gold), #f97316); }
.kpi-icon-seated { background: linear-gradient(135deg, var(--teal), var(--success)); }
.kpi-icon-dining { background: linear-gradient(135deg, var(--danger), #dc2626); }
.kpi-icon-cleaning { background: linear-gradient(135deg, var(--purple), #818cf8); }
.kpi-icon-revenue { background: linear-gradient(135deg, var(--success), #059669); }
.kpi-card span { display: block; font-size: var(--text-xs); color: var(--muted); }
.kpi-card strong { display: block; font-size: var(--text-3xl); font-weight: 800; margin-top: 2px; line-height: 1.1; }

/* ── 面板通用 ── */
.panel { background: var(--paper); padding: var(--space-base); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.panel h3 { font-size: var(--text-base); margin-bottom: var(--space-sm); }

/* ── 时间线区域 ── */
.timeline-section { margin-bottom: var(--space-lg); }
.timeline-scroll { overflow-x: auto; padding-bottom: var(--space-sm); scrollbar-width: thin; scrollbar-color: var(--line) transparent; }
.timeline-scroll::-webkit-scrollbar { height: 6px; }
.timeline-scroll::-webkit-scrollbar-track { background: transparent; }
.timeline-scroll::-webkit-scrollbar-thumb { background: var(--line); border-radius: 3px; }
.timeline-axis { display: flex; gap: var(--space-lg); min-width: max-content; }
.timeline-slot { display: flex; flex-direction: column; align-items: center; min-width: 140px; }
.ts-time {
  font-size: var(--text-sm); font-weight: 700; color: var(--ink); margin-bottom: var(--space-sm);
  padding: var(--space-xs) var(--space-sm); background: var(--teal-light); border-radius: var(--radius-sm);
}
.ts-cards { display: flex; flex-direction: column; gap: var(--space-xs); width: 100%; }
.ts-card {
  padding: var(--space-sm); border-radius: var(--radius-md); border: 1px solid var(--line);
  cursor: pointer; transition: all 0.15s; text-align: center;
}
.ts-card:hover { border-color: var(--teal); box-shadow: var(--shadow-sm); }
.ts-card.active { border-color: var(--teal); background: var(--teal-light); }
.ts-card.ts-booked { border-left: 3px solid var(--status-booked); }
.ts-card.ts-seated { border-left: 3px solid var(--status-seated); }
.ts-card.ts-dining { border-left: 3px solid var(--status-dining); }
.ts-card.ts-finished { border-left: 3px solid var(--status-finished); }
.tsc-name { font-size: var(--text-sm); font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tsc-info { font-size: var(--text-xs); color: var(--muted); margin-bottom: var(--space-xs); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── 下方两栏 ── */
.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-base); }
@media (max-width: 1024px) { .bottom-grid { grid-template-columns: 1fr; } }

/* ── 当前处理 ── */
.process-customer { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-base); }
.pc-avatar { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; font-size: 16px; font-weight: 700; color: var(--ink); flex-shrink: 0; }
.process-customer strong { display: block; }
.process-customer span { font-size: var(--text-sm); color: var(--muted); display: block; }
.pc-phone { font-family: var(--font-mono); font-size: var(--text-xs) !important; }

.flow-bar { display: flex; align-items: center; gap: var(--space-xs); margin-bottom: var(--space-base); padding: var(--space-sm) 0; }
.flow-step { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-xs); color: var(--muted); }
.flow-step:not(:last-child)::after { content: '→'; margin: 0 var(--space-xs); color: var(--line); }
.fs-dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid var(--line); }
.step-done .fs-dot { background: var(--status-finished); border-color: var(--status-finished); }
.step-done { color: var(--status-finished); }
.step-current .fs-dot { background: var(--status-seated); border-color: var(--status-seated); box-shadow: 0 0 0 3px rgba(15,118,110,0.2); }
.step-current { color: var(--status-seated); font-weight: 600; }

.alerts-area { display: flex; flex-direction: column; gap: var(--space-xs); margin-bottom: var(--space-base); }
.alert-item { display: flex; align-items: flex-start; gap: var(--space-xs); padding: var(--space-sm); border-radius: var(--radius-sm); background: var(--gold-light); font-size: var(--text-xs); }
.alert-item.info { background: var(--blue-light); }
.alert-item strong { display: block; font-size: var(--text-sm); }
.alert-item p { color: var(--muted); margin-top: 2px; }
.alert-ok { display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-sm); font-size: var(--text-xs); color: var(--success); }

/* ── 待处理订单 ── */
.pending-orders-section { margin-bottom: var(--space-lg); }
.section-head { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-base); }
.section-head h3 { margin: 0; }
.pending-orders-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: var(--space-sm); }
.po-card { padding: var(--space-sm); background: var(--wash); border-radius: var(--radius-md); border: 1px solid var(--line); transition: all var(--transition-fast); }
.po-card:hover { border-color: var(--teal-light); box-shadow: var(--shadow-sm); }
.po-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-xs); }
.po-head strong { font-size: var(--text-base); }
.po-info { font-size: var(--text-xs); color: var(--muted); margin-bottom: 2px; }
.po-items { font-size: var(--text-xs); color: var(--teal); font-weight: 600; }

.process-actions { display: flex; flex-wrap: wrap; gap: var(--space-sm); align-items: center; }
.no-action-hint { font-size: var(--text-xs); color: var(--muted); }
.btn-icon { margin-right: 4px; }

/* ── 桌位概览 ── */
.table-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm); margin-bottom: var(--space-sm); }
.table-block {
  padding: var(--space-sm); border-radius: var(--radius-md); text-align: center;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
  cursor: default;
}
.table-block:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.tb-code { font-weight: 700; font-size: var(--text-base); }
.tb-cat { font-size: var(--text-xs); color: var(--muted); }
.tb-status { font-size: var(--text-xs); margin-top: 2px; }
.tb-free { background: var(--status-finished-bg); color: #166534; }
.tb-reserved { background: var(--status-booked-bg); color: #1e40af; }
.tb-occupied { background: var(--status-seated-bg); color: #115e59; }
.tb-dining { background: var(--status-dining-bg); color: #991b1b; }
.tb-cleaning { background: var(--status-noshow-bg); color: #92400e; }
.tb-maintenance { background: var(--wash); color: var(--muted); opacity: 0.6; }

.table-legend { display: flex; gap: var(--space-base); font-size: var(--text-xs); color: var(--muted); flex-wrap: wrap; }
.table-legend span { display: flex; align-items: center; gap: var(--space-xs); }
.lg-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.lg-dot-free { background: var(--success); }
.lg-dot-booked { background: var(--status-booked); }
.lg-dot-seated { background: var(--status-seated-bg); border: 1px solid var(--status-seated); }
.lg-dot-dining { background: var(--status-dining); }
.lg-dot-cleaning { background: var(--status-noshow); }

@media (max-width: 640px) {
  .kpi-strip { grid-template-columns: repeat(2, 1fr); }
  .table-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
