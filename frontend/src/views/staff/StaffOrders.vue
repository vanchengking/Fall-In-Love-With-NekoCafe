<template>
  <div class="page-enter-active">
    <!-- 顶部工具栏 -->
    <div class="staff-toolbar">
      <h2>订单履约管理</h2>
      <div class="toolbar-right">
        <el-button :icon="RefreshCw" circle size="small" @click="loadOrders" />
        <span v-if="lastUpdated" class="refresh-hint">{{ lastUpdated.toLocaleTimeString() }} 刷新</span>
      </div>
    </div>

    <!-- 状态卡片区（4 个，点击筛选） -->
    <div class="kpi-strip">
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'pending' }" @click="filterStatus = filterStatus === 'pending' ? null : 'pending'">
        <div class="kpi-icon ki-pending"><Clock :size="16" /></div>
        <div><span>待出餐</span><strong>{{ stats.pending }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'paid' }" @click="filterStatus = filterStatus === 'paid' ? null : 'paid'">
        <div class="kpi-icon ki-paid"><CheckCircle2 :size="16" /></div>
        <div><span>已支付</span><strong>{{ stats.paid }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'exception' }" @click="filterStatus = filterStatus === 'exception' ? null : 'exception'">
        <div class="kpi-icon ki-exception"><AlertTriangle :size="16" /></div>
        <div><span>异常</span><strong>{{ stats.exception }}</strong></div>
      </div>
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'all' }" @click="filterStatus = null">
        <div class="kpi-icon ki-revenue"><Banknote :size="16" /></div>
        <div><span>今日收入</span><strong>{{ cents(stats.revenue) }}</strong></div>
      </div>
    </div>

    <!-- 待履约数量提示 -->
    <div v-if="pendingCount > 0" class="pending-banner">
      <Bell :size="14" /> 待履约数量：<strong>{{ pendingCount }}</strong>
    </div>

    <!-- 异常告警面板 -->
    <section class="alert-panel" v-if="exceptionOrders.length || alerts.length">
      <div class="alert-header" @click="alertExpanded = !alertExpanded">
        <div class="alert-header-left">
          <AlertTriangle :size="16" class="alert-icon-danger" />
          <strong>异常告警</strong>
          <el-tag type="danger" size="small" effect="dark">{{ exceptionOrders.length + alerts.length }}</el-tag>
        </div>
        <el-icon class="alert-chevron" :class="{ open: alertExpanded }"><ArrowDown /></el-icon>
      </div>
      <div v-show="alertExpanded" class="alert-body">
        <!-- 异常订单 -->
        <div v-for="o in exceptionOrders" :key="'eo-' + o.id" class="alert-item alert-exception" @click="selectedId = o.id">
          <div class="alert-icon-wrap exception"><AlertTriangle :size="14" /></div>
          <div class="alert-content">
            <div class="alert-title">订单异常 #{{ o.id }}</div>
            <div class="alert-desc">{{ o.customer_name || '-' }} · {{ o.table_code || '-' }} · {{ cents(o.total_cents) }}</div>
          </div>
          <el-tag type="danger" size="small">异常</el-tag>
        </div>
        <!-- 运营告警 -->
        <div v-for="a in alerts" :key="'al-' + a.id" class="alert-item" :class="'alert-' + a.level">
          <div class="alert-icon-wrap" :class="a.level">
            <Info v-if="a.level === 'info'" :size="14" />
            <AlertTriangle v-else :size="14" />
          </div>
          <div class="alert-content">
            <div class="alert-title">{{ a.title }}</div>
            <div class="alert-desc">{{ a.detail }}</div>
          </div>
          <el-tag :type="a.level === 'info' ? 'info' : 'warning'" size="small" effect="plain">
            {{ a.level === 'info' ? '提示' : '警告' }}
          </el-tag>
        </div>
      </div>
    </section>

    <!-- 两栏布局：左侧订单列表 + 右侧详情 -->
    <div class="order-grid">
      <!-- 左侧：订单列表 -->
      <div class="order-list">
        <div
          v-for="o in filteredOrders"
          :key="o.id"
          class="order-card"
          :class="{ selected: selectedId === o.id, 'oc-exception': o.status === 'exception' }"
          role="button"
          tabindex="0"
          :aria-label="`订单 #${o.id} ${orderStatusLabel(o.status)}`"
          @click="selectedId = o.id"
          @keydown.enter="selectedId = o.id"
        >
          <div class="oc-head">
            <strong>#{{ o.id }}</strong>
            <el-tag :type="orderStatusType(o.status)" size="small">{{ orderStatusLabel(o.status) }}</el-tag>
          </div>
          <div class="oc-info">
            <span>{{ o.table_code || '-' }}</span>
            <span>{{ o.customer_name || '-' }}</span>
            <span>{{ o.party_size || '-' }}人</span>
          </div>
          <div class="oc-meta">
            <span class="oc-price">{{ cents(o.total_cents) }}</span>
            <span v-if="o.reservation_status" class="oc-flow">{{ reservationFlowLabel(o.reservation_status) }}</span>
          </div>
        </div>
        <el-empty v-if="!filteredOrders.length" :description="filterStatus ? '当前筛选无订单' : '暂无订单'" />
      </div>

      <!-- 右侧：订单详情/操作区 -->
      <div class="detail-panel">
        <template v-if="selected">
          <div class="dp-header">
            <h3>订单#{{ selected.id }} · {{ selected.table_code || '-' }} · {{ selected.customer_name || '-' }}</h3>
            <el-tag :type="orderStatusType(selected.status)" size="small">{{ orderStatusLabel(selected.status) }}</el-tag>
          </div>

          <!-- 菜品表格 -->
          <h4>菜品明细</h4>
          <el-table :data="selected.items || []" size="small" stripe style="margin-bottom: var(--space-base)">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="quantity" label="数量" width="60" align="center" />
            <el-table-column label="单价" width="80" align="right">
              <template #default="{ row }">{{ cents(row.price_cents) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="80" align="right">
              <template #default="{ row }">{{ cents(row.subtotal_cents) }}</template>
            </el-table-column>
            <el-table-column label="出餐状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.served ? 'success' : 'warning'" size="small">{{ row.served ? '已出餐' : '待出餐' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="dp-total">合计：<strong>{{ cents(selected.total_cents) }}</strong></div>

          <!-- 操作按钮区（按状态严格控制） -->
          <div class="dp-actions">
            <!-- pending: 接单 / 备餐 → preparing -->
            <el-button v-if="selected.status === 'pending'" type="success" size="small" :loading="actionLoading" @click="updateStatus(selected, 'preparing')">接单</el-button>
            <el-button v-if="selected.status === 'pending'" type="primary" size="small" :loading="actionLoading" @click="updateStatus(selected, 'preparing')">备餐</el-button>
            <!-- preparing: 出餐 → served -->
            <el-button v-if="selected.status === 'preparing'" type="primary" size="small" :loading="actionLoading" @click="updateStatus(selected, 'served')">出餐</el-button>
            <el-button v-if="selected.status === 'preparing'" type="success" size="small" :loading="actionLoading" @click="updateStatus(selected, 'served')">标记出餐</el-button>
            <!-- served: 通知服务 / 退款登记 -->
            <el-button v-if="selected.status === 'served'" type="warning" plain size="small" @click="ElMessage.info('通知服务功能开发中')">通知服务</el-button>
            <el-button v-if="selected.status === 'served'" type="danger" plain size="small" @click="ElMessage.info('退款登记功能开发中')">退款登记</el-button>
            <!-- paid: 退款登记 -->
            <el-button v-if="selected.status === 'paid'" type="danger" plain size="small" @click="ElMessage.info('退款登记功能开发中')">退款登记</el-button>
            <!-- 通用: 关联预约 / 报告异常 -->
            <el-button type="info" plain size="small" @click="ElMessage.info('关联预约功能开发中')">关联预约</el-button>
            <el-button v-if="selected.status !== 'exception'" type="danger" size="small" :loading="actionLoading" @click="updateStatus(selected, 'exception')">报告异常</el-button>
            <el-button v-if="selected.status === 'exception'" type="danger" size="small" @click="ElMessage.info('异常详情开发中')">查看异常</el-button>
          </div>

          <!-- 支付流水 -->
          <div v-if="selected.payment_status === 'paid'" class="dp-section">
            <h4>支付流水</h4>
            <div class="payment-row">
              <span>{{ selected.payment_method || '微信支付' }}</span>
              <span>{{ cents(selected.total_cents) }}</span>
              <span>{{ selected.paid_at || selected.created_at || '-' }}</span>
            </div>
          </div>

          <!-- 顾客备注 + 猫区提醒 -->
          <div v-if="selected.note || selected.cat_reminder" class="dp-section">
            <div v-if="selected.note" class="note-box">
              <strong>顾客备注：</strong>{{ selected.note }}
            </div>
            <div v-if="selected.cat_reminder" class="note-box cat-note">
              <strong>猫区提醒：</strong>{{ selected.cat_reminder }}
            </div>
          </div>
        </template>
        <el-empty v-else description="点击左侧订单查看详情" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, CheckCircle2, AlertTriangle, Banknote, RefreshCw, Bell, Info } from '@lucide/vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { api } from '@/utils/http'
import { cents, orderStatusLabel, orderStatusType } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import { fallbackOrders, fallbackDashboard } from '@/utils/fallback'
import type { Order, OrderStatus, ReservationStatus } from '@/types'

const orders = ref<Order[]>([])
const selectedId = ref<number | null>(null)
const filterStatus = ref<string | null>(null)
const alertExpanded = ref(true)
const actionLoading = ref(false)

// ── 告警数据 ──
const alerts = ref(fallbackDashboard.alerts || [])
const exceptionOrders = computed(() => orders.value.filter(o => o.status === 'exception'))

const filteredOrders = computed(() => {
  if (!filterStatus.value) return orders.value
  if (filterStatus.value === 'paid') return orders.value.filter(o => o.payment_status === 'paid')
  return orders.value.filter(o => o.status === filterStatus.value)
})

// ── 状态卡片统计（实时计算） ──
const stats = computed(() => ({
  pending:   orders.value.filter(o => o.status === 'pending').length,
  paid:      orders.value.filter(o => o.payment_status === 'paid').length,
  exception: orders.value.filter(o => o.status === 'exception').length,
  revenue:   orders.value.filter(o => o.payment_status === 'paid').reduce((s, o) => s + o.total_cents, 0),
}))

// 待履约 = pending + preparing（跟随筛选）
const pendingCount = computed(() =>
  filteredOrders.value.filter(o => o.status === 'pending' || o.status === 'preparing').length
)

const selected = computed(() => orders.value.find(o => o.id === selectedId.value) || null)

// ── 预约状态流转标签 ──
function reservationFlowLabel(status: ReservationStatus): string {
  const map: Record<string, string> = {
    booked:   '已预约 → 已入座',
    seated:   '已入座 → 用餐中',
    dining:   '用餐中 → 已完成',
    finished: '已完成',
    cancelled: '已取消',
    no_show:  '未到店',
  }
  return map[status] || ''
}

// ── 更新订单状态 ──
async function updateStatus(o: Order, status: OrderStatus) {
  actionLoading.value = true
  try {
    await api.patch(`/orders/${o.id}/status`, { status })
    ElMessage.success('操作成功')
    await loadOrders()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { actionLoading.value = false }
}

// ── 数据加载 ──
async function loadOrders() {
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { orders.value = fallbackOrders }
}

const { lastUpdated, startWithChangeDetection } = usePolling(loadOrders, 10000)
startWithChangeDetection(() => orders.value.map(o => o.id).sort().join(','))
loadOrders()
</script>

<style scoped>
h4 { margin: var(--space-base) 0 var(--space-sm); font-size: var(--text-base); }
.staff-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.staff-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.refresh-hint { font-size: var(--text-xs); color: var(--muted); }

/* ── KPI 状态卡片 ── */
.kpi-strip {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm);
  margin-bottom: var(--space-base); padding: var(--space-base);
  background: linear-gradient(135deg, var(--teal-light), var(--gold-light)); border-radius: var(--radius-lg);
}
.kpi-card { display: flex; align-items: center; gap: var(--space-sm); padding: var(--space-sm) var(--space-base); border-radius: var(--radius-md); cursor: pointer; transition: all 0.15s; border: 2px solid transparent; }
.kpi-card:hover { transform: translateY(-2px); }
.kpi-card.kpi-active { background: #fff; border-color: var(--teal); box-shadow: 0 0 0 2px rgba(15,118,110,0.15), var(--shadow-sm); }
.kpi-icon { width: 32px; height: 32px; border-radius: var(--radius-sm); display: grid; place-items: center; color: #fff; flex-shrink: 0; }
.ki-pending { background: linear-gradient(135deg, var(--gold), #f97316); }
.ki-paid { background: linear-gradient(135deg, var(--success), #059669); }
.ki-exception { background: linear-gradient(135deg, var(--danger), #dc2626); }
.ki-revenue { background: linear-gradient(135deg, var(--teal-bright), var(--blue)); }
.kpi-card span { display: block; font-size: var(--text-xs); color: var(--muted); }
.kpi-card strong { display: block; font-size: var(--text-2xl); font-weight: 800; line-height: 1.1; }

/* ── 待履约提示 ── */
.pending-banner {
  display: flex; align-items: center; gap: var(--space-xs);
  padding: var(--space-sm) var(--space-base); margin-bottom: var(--space-base);
  background: var(--gold-light); border-radius: var(--radius-md); font-size: var(--text-sm); color: #92400e;
}
.pending-banner strong { color: #b45309; }

/* ── 异常告警面板 ── */
.alert-panel {
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-lg);
  margin-bottom: var(--space-base); overflow: hidden;
}
.alert-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--space-sm) var(--space-base); cursor: pointer; user-select: none;
  transition: background 0.15s;
}
.alert-header:hover { background: #fafafa; }
.alert-header-left { display: flex; align-items: center; gap: var(--space-sm); }
.alert-header-left strong { font-size: var(--text-sm); }
.alert-icon-danger { color: var(--danger); }
.alert-chevron { transition: transform 0.2s; color: var(--muted); }
.alert-chevron.open { transform: rotate(180deg); }

.alert-body { border-top: 1px solid var(--line); }
.alert-item {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-sm) var(--space-base); cursor: pointer;
  transition: background 0.15s; border-bottom: 1px solid #f3f4f6;
}
.alert-item:last-child { border-bottom: none; }
.alert-item:hover { background: #fafafa; }

.alert-icon-wrap {
  width: 28px; height: 28px; border-radius: 50%;
  display: grid; place-items: center; flex-shrink: 0;
}
.alert-icon-wrap.exception { background: #fee2e2; color: #EF4444; }
.alert-icon-wrap.warning { background: #fef3c7; color: #F59E0B; }
.alert-icon-wrap.info { background: #dbeafe; color: #3B82F6; }

.alert-content { flex: 1; min-width: 0; }
.alert-title { font-size: var(--text-sm); font-weight: 600; line-height: 1.3; }
.alert-desc { font-size: var(--text-xs); color: var(--muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ── 两栏布局 ── */
.order-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-base); }
@media (max-width: 1024px) { .order-grid { grid-template-columns: 1fr; } }

/* ── 左侧订单列表 ── */
.order-list { display: flex; flex-direction: column; gap: var(--space-sm); max-height: 600px; overflow-y: auto; }
.order-card {
  padding: var(--space-base); border-radius: var(--radius-md); cursor: pointer;
  transition: all var(--transition-fast); border: 1px solid var(--line); background: var(--paper);
  box-shadow: var(--shadow-sm);
}
.order-card:hover { border-color: var(--teal); box-shadow: var(--shadow-md); transform: translateY(-1px); }
.order-card.selected { border-color: var(--teal); background: var(--teal-light); }
.order-card.oc-exception { border-left: 3px solid #EF4444; }
.oc-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-xs); }
.oc-head strong { font-size: var(--text-base); }
.oc-info { display: flex; gap: var(--space-sm); font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-xs); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.oc-meta { display: flex; align-items: center; justify-content: space-between; }
.oc-price { font-weight: 700; color: var(--coral); font-size: var(--text-base); }
.oc-flow { font-size: var(--text-xs); color: var(--teal); }

/* ── 右侧详情面板 ── */
.detail-panel {
  background: var(--paper); border-radius: var(--radius-lg); border: 1px solid var(--line);
  padding: var(--space-lg); position: sticky; top: 72px; align-self: start;
}
.dp-header { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-base); flex-wrap: wrap; }
.dp-header h3 { margin: 0; font-size: var(--text-lg); }
.dp-total { text-align: right; font-size: var(--text-lg); margin-bottom: var(--space-base); }
.dp-total strong { color: var(--coral); }
.dp-actions { display: flex; gap: var(--space-sm); flex-wrap: wrap; margin-bottom: var(--space-base); }
.dp-section { margin-top: var(--space-base); padding-top: var(--space-base); border-top: 1px solid var(--line); }

/* ── 支付流水 ── */
.payment-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-sm) var(--space-base); background: #f0fdf4; border-radius: var(--radius-sm);
  font-size: var(--text-sm);
}

/* ── 顾客备注 / 猫区提醒 ── */
.note-box {
  padding: var(--space-sm) var(--space-base); background: #eff6ff; border-radius: var(--radius-sm);
  font-size: var(--text-sm); margin-bottom: var(--space-sm); line-height: 1.6;
}
.note-box.cat-note { background: #fef3c7; color: #92400e; }
</style>
