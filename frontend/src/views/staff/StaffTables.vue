<template>
  <div class="page-enter-active">
    <!-- 顶部栏 -->
    <div class="staff-toolbar">
      <h2>实时桌位</h2>
      <div class="toolbar-right">
        <el-button :icon="RefreshCw" circle size="small" :loading="refreshing" @click="manualRefresh" />
        <span class="refresh-badge">10秒刷新</span>
        <span v-if="lastUpdated" class="refresh-time">{{ lastUpdated.toLocaleTimeString() }}</span>
      </div>
    </div>

    <!-- 主体：左平面图 + 右排队列表 -->
    <div class="main-grid">
      <!-- 左侧：桌位网格 -->
      <div class="table-overview-panel">
        <h3>桌位概览</h3>
        <div class="table-grid">
          <div
            v-for="t in tables" :key="t.id"
            class="table-block"
            :class="['tb-' + (t.status || 'available'), { 'tb-selected': selectedId === Number(t.id) }]"
            @click="selectTable(t)"
          >
            <div class="tb-code">{{ t.code }}</div>
            <div class="tb-seats">{{ t.seats }}人 · {{ areaLabel(t.area) }}</div>
            <div class="tb-status">{{ statusLabel(t.status) }}</div>
            <div v-if="t.cat_name" class="tb-cat">🐱 {{ t.cat_name }}</div>
          </div>
        </div>
        <div class="table-legend">
          <span><span class="lg-dot" style="background:#10B981"></span>空闲</span>
          <span><span class="lg-dot" style="background:#3B82F6"></span>已预约</span>
          <span><span class="lg-dot" style="background:#0F766E"></span>已入座</span>
          <span><span class="lg-dot" style="background:#EF4444"></span>用餐中</span>
          <span><span class="lg-dot" style="background:#F59E0B"></span>待清台</span>
        </div>
      </div>

      <!-- 右侧：排队列表 + 图例 -->
      <aside class="right-col">
        <section class="panel queue-panel">
          <div class="panel-head">
            <h3>排队与即将到店</h3>
            <span class="queue-count">{{ queueList.length }} 组</span>
          </div>
          <div class="queue-scroll">
            <div v-for="r in visibleQueue" :key="r.id" class="q-item">
              <div class="q-main">
                <strong>{{ r.customer_name || r.customerName }}</strong>
                <span class="q-meta">{{ r.party_size }}人 · {{ r.reservation_time }}</span>
              </div>
              <div class="q-sub">
                <span class="q-table">建议 {{ r.table_code || '待分配' }}</span>
                <span class="q-status" :class="'qs-' + r.status">{{ resStatusLabel(r.status) }}</span>
              </div>
              <div class="q-actions">
                <el-button size="small" type="success" @click="queueAction(r, '安排')">安排</el-button>
                <el-button size="small" plain @click="queueAction(r, '转桌')">转桌</el-button>
                <el-button size="small" plain @click="queueAction(r, '联系')">联系</el-button>
              </div>
            </div>
            <div v-if="!queueList.length" class="q-empty">暂无排队顾客</div>
          </div>
          <div v-if="queueList.length > queueShowCount" class="q-footer">
            <el-button type="primary" link size="small" @click="queueShowCount += 5">查看全部（{{ queueList.length }}）</el-button>
          </div>
        </section>
      </aside>
    </div>

    <!-- 底部：选中桌位详情面板 -->
    <section v-if="sel" class="detail-bar">
        <div class="db-close"><el-button :icon="X" circle size="small" @click="selectedId = null" /></div>
        <div class="db-grid">
          <div class="db-left">
            <div class="db-title">{{ sel.code }} · {{ areaLabel(sel.area) }}</div>
            <div class="db-status" :style="{ color: statusColor(sel.status) }">{{ statusLabel(sel.status) }}</div>
            <div class="db-flow">
              <span class="flow-step" :class="{ on: sel.status === 'occupied' }">已入座</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step" :class="{ on: sel.status === 'dining' }">用餐中</span>
              <span class="flow-arrow">→</span>
              <span class="flow-step" :class="{ on: sel.status === 'cleaning' }">清台中</span>
            </div>
            <div class="db-meta">
              <span class="db-seats">{{ sel.seats }}人桌</span>
              <span v-if="sel.cat_name" class="db-cat">推荐猫咪 <strong>{{ sel.cat_name }}</strong></span>
            </div>
          </div>
          <div class="db-right">
            <template v-if="selRes">
              <div class="db-res-info">
                <strong>{{ selRes.customer_name }}</strong> · {{ selRes.party_size }}人 · {{ selRes.reservation_time }}
              </div>
              <div v-if="selRes.note" class="db-note">{{ selRes.note }}</div>
              <div v-if="selOrder" class="db-order">订单#{{ selOrder.id }} <el-tag :type="selOrder.payment_status === 'paid' ? 'success' : 'warning'" size="small">{{ selOrder.payment_status === 'paid' ? '已支付' : '未支付' }}</el-tag></div>
            </template>
            <div v-else class="db-no-res">当前无预约</div>
          </div>
        </div>
        <div class="db-actions">
          <template v-if="sel.status === 'reserved'">
            <el-button type="success" :loading="actionLoading" @click="tableAction(sel, 'occupied')">确认入座</el-button>
            <el-button type="danger" plain :loading="actionLoading" @click="tableAction(sel, 'free')">取消预约</el-button>
          </template>
          <template v-else-if="sel.status === 'occupied'">
            <el-button type="primary" :loading="actionLoading" @click="tableAction(sel, 'dining')">开始用餐</el-button>
          </template>
          <template v-else-if="sel.status === 'dining'">
            <el-button type="success" :loading="actionLoading" @click="tableAction(sel, 'free')">完桌</el-button>
            <el-button type="warning" :loading="actionLoading" @click="tableAction(sel, 'cleaning')">清台</el-button>
          </template>
          <template v-else-if="sel.status === 'cleaning'">
            <el-button type="success" :loading="actionLoading" @click="tableAction(sel, 'free')">完成清洁</el-button>
          </template>
          <template v-else-if="sel.status === 'maintenance'">
            <el-button type="primary" :loading="actionLoading" @click="tableAction(sel, 'free')">恢复使用</el-button>
          </template>
          <template v-else-if="sel.status === 'free'">
            <span class="db-idle">桌位空闲中</span>
          </template>
        </div>
    </section>

    <!-- 操作模拟弹窗 -->
    <el-dialog v-model="dlgVisible" :title="dlgTitle" width="400px">
      <p>{{ dlgMessage }}</p>
      <template #footer>
        <el-button @click="dlgVisible = false">关闭</el-button>
        <el-button type="primary" @click="dlgVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshCw, X } from '@lucide/vue'
import { api } from '@/utils/http'
import { usePolling } from '@/composables/usePolling'
import { fallbackTables, fallbackReservations, fallbackOrders } from '@/utils/fallback'
import type { DiningTable, Reservation, Order, TableStatus } from '@/types'

const tables = ref<DiningTable[]>([])
const reservations = ref<Reservation[]>([])
const orders = ref<Order[]>([])
const selectedId = ref<number | null>(null)
const refreshing = ref(false)
const queueShowCount = ref(5)

const dlgVisible = ref(false)
const dlgTitle = ref('')
const dlgMessage = ref('')
const actionLoading = ref(false)

// ── 排队列表 ──
const today = new Date().toISOString().slice(0, 10)
const queueList = computed(() =>
  reservations.value
    .filter(r => r.status === 'booked' && r.reservation_date === today)
    .sort((a, b) => (a.reservation_time || '').localeCompare(b.reservation_time || ''))
)
const visibleQueue = computed(() => queueList.value.slice(0, queueShowCount.value))

// ── 选中桌位 ──
const sel = computed(() => {
  if (selectedId.value == null) return null
  return tables.value.find(t => Number(t.id) === selectedId.value) || null
})
const selRes = computed(() => {
  if (!sel.value?.current_reservation_id) return null
  return reservations.value.find(r => r.id === sel.value!.current_reservation_id) || null
})
const selOrder = computed(() => {
  if (!selRes.value) return null
  return orders.value.find(o => o.reservation_id === selRes.value!.id) || null
})

// ── 图例 ──

// ── 工具函数 ──
function statusLabel(s?: string): string {
  const map: Record<string, string> = { free: '空闲', reserved: '已预约', occupied: '已入座', dining: '用餐中', cleaning: '待清台', maintenance: '维护中', available: '空闲' }
  return map[s || 'free'] || '空闲'
}

function statusColor(s?: string): string {
  const map: Record<string, string> = { free: '#10B981', available: '#10B981', reserved: '#3B82F6', occupied: '#0F766E', dining: '#EF4444', cleaning: '#F59E0B', maintenance: '#9CA3AF' }
  return map[s || 'free'] || '#10B981'
}

function resStatusLabel(s: string): string {
  const map: Record<string, string> = { booked: '待确认', seated: '已入座', dining: '用餐中', finished: '已完成', cancelled: '已取消', no_show: '未到' }
  return map[s] || s
}

function areaLabel(a: string): string {
  const map: Record<string, string> = { window: '靠窗猫区', main: '主厅', party: '聚会区' }
  return map[a] || a
}

function selectTable(t: DiningTable) {
  const id = Number(t.id)
  selectedId.value = selectedId.value === id ? null : id
}

// ── 排队操作 ──
function queueAction(r: Reservation, action: string) {
  const name = r.customer_name || '顾客'
  if (action === '安排') {
    ElMessageBox.confirm(`确认将 ${name}（${r.party_size}人）安排入座到 ${r.table_code || '待分配'}？`, '安排入座', { type: 'success' })
      .then(async () => {
        try {
          await api.patch(`/reservations/${r.id}/status`, { status: 'seated' })
          ElMessage.success(`${name} 已安排入座`)
          await loadData()
        } catch (e) { ElMessage.error((e as Error).message) }
      })
      .catch(() => {})
  } else {
    dlgTitle.value = action
    dlgMessage.value = action === '转桌'
      ? `已为 ${name} 发起转桌请求，等待分配新桌位。`
      : `已通过 ${r.mobile_number || '预留号码'} 联系 ${name}。`
    dlgVisible.value = true
  }
}

// ── 桌位操作 ──
function tableAction(t: DiningTable, newStatus: TableStatus) {
  const actionMap: Record<string, string> = {
    occupied: '确认入座',
    dining: '开始用餐',
    free: t.status === 'dining' ? '完桌' : t.status === 'cleaning' ? '完成清洁' : '取消预约',
    cleaning: '清台',
  }
  const title = actionMap[newStatus] || '操作'
  ElMessageBox.confirm(`确认对桌位 ${t.code} 执行「${title}」？`, title, { type: 'warning' })
    .then(async () => {
      actionLoading.value = true
      try {
        await api.patch(`/tables/${t.id}/status`, { status: newStatus })
        if (t.current_reservation_id) {
          const resStatusMap: Record<string, string> = { occupied: 'seated', dining: 'dining', free: 'finished', cleaning: 'finished' }
          const rs = resStatusMap[newStatus]
          if (rs) {
            await api.patch(`/reservations/${t.current_reservation_id}/status`, { status: rs })
          }
        }
        ElMessage.success(`${t.code} ${title}成功`)
        await loadData()
      } catch (e) { ElMessage.error((e as Error).message) }
      finally { actionLoading.value = false }
    })
    .catch(() => {})
}

// ── 数据加载 ──
async function loadData() {
  try { tables.value = await api.get<DiningTable[]>('/tables', { storeId: 1 }) }
  catch { tables.value = fallbackTables }
  try { reservations.value = await api.get<Reservation[]>('/reservations') }
  catch { reservations.value = fallbackReservations }
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { orders.value = fallbackOrders }
}

async function manualRefresh() {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

const { lastUpdated, startWithChangeDetection } = usePolling(loadData, 10000)
startWithChangeDetection(() => tables.value.map(t => `${t.id}:${t.status}`).sort().join(','))
loadData()
</script>

<style scoped>
.staff-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-base); }
.staff-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.refresh-badge { font-size: var(--text-xs); color: var(--teal); font-weight: 600; background: var(--teal-light); padding: 2px 8px; border-radius: 99px; }
.refresh-time { font-size: var(--text-xs); color: var(--muted); }

/* ── 主体两栏 ── */
.main-grid { display: grid; grid-template-columns: 1fr 300px; gap: var(--space-base); margin-bottom: var(--space-base); }
@media (max-width: 1024px) { .main-grid { grid-template-columns: 1fr; } }

/* ── 桌位概览面板 ── */
.table-overview-panel {
  background: var(--paper); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: var(--space-lg);
}
.table-overview-panel h3 { margin: 0 0 var(--space-base); font-size: var(--text-base); font-weight: 700; }
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: var(--space-sm); margin-bottom: var(--space-base); }
.table-block {
  padding: var(--space-sm); border-radius: var(--radius-md); text-align: center;
  transition: all var(--transition-fast); box-shadow: var(--shadow-sm); cursor: pointer;
  border: 2px solid transparent;
}
.table-block:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.table-block.tb-selected { border-color: var(--teal); box-shadow: 0 0 0 2px rgba(15,118,110,0.25); }
.tb-code { font-weight: 800; font-size: var(--text-lg); }
.tb-seats { font-size: var(--text-xs); color: var(--muted); margin-top: 2px; }
.tb-status { font-size: var(--text-xs); margin-top: 2px; font-weight: 600; }
.tb-cat { font-size: var(--text-xs); color: var(--teal); margin-top: 2px; }
.tb-available { background: #dcfce7; color: #166534; }
.tb-free { background: #dcfce7; color: #166534; }
.tb-reserved { background: #dbeafe; color: #1e40af; }
.tb-occupied { background: #d4f0ed; color: #115e59; }
.tb-dining { background: #fee2e2; color: #991b1b; }
.tb-cleaning { background: #fef3c7; color: #92400e; }
.tb-maintenance { background: var(--wash); color: var(--muted); opacity: 0.6; }
.table-legend { display: flex; gap: var(--space-base); font-size: var(--text-xs); color: var(--muted); flex-wrap: wrap; }
.table-legend span { display: flex; align-items: center; gap: var(--space-xs); }
.lg-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }

/* ══════════════════════════════════════
   右侧栏
   ══════════════════════════════════════ */
.right-col { display: flex; flex-direction: column; gap: var(--space-base); }
.panel { background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: var(--space-base); }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-sm); }
.panel-head h3 { margin: 0; font-size: var(--text-base); }
.queue-count { font-size: var(--text-xs); color: var(--muted); }
.queue-scroll { max-height: 420px; overflow-y: auto; display: flex; flex-direction: column; gap: var(--space-xs); }

.q-item { padding: var(--space-sm); border-radius: var(--radius-md); border: 1px solid var(--line); transition: all var(--transition-fast); }
.q-item:hover { background: var(--wash); box-shadow: var(--shadow-sm); }
.q-main { display: flex; align-items: baseline; gap: var(--space-sm); margin-bottom: 2px; }
.q-main strong { font-size: var(--text-sm); }
.q-meta { font-size: var(--text-xs); color: var(--muted); }
.q-sub { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-xs); }
.q-table { font-size: var(--text-xs); color: var(--teal); }
.q-status { font-size: 10px; padding: 1px 6px; border-radius: 99px; }
.qs-booked { background: #dbeafe; color: #1e40af; }
.q-actions { display: flex; gap: var(--space-xs); }
.q-empty { text-align: center; padding: var(--space-lg); font-size: var(--text-sm); color: var(--muted); }
.q-footer { text-align: center; margin-top: var(--space-xs); }

/* ── 图例 ── */
.legend {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-base);
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
}
/* ══════════════════════════════════════
   底部详情面板
   ══════════════════════════════════════ */
.detail-bar {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: var(--space-base) var(--space-lg);
  position: relative;
  box-shadow: var(--shadow-md);
}
.db-close { position: absolute; top: var(--space-sm); right: var(--space-sm); }
.db-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-lg); margin-bottom: var(--space-base); }
@media (max-width: 768px) { .db-grid { grid-template-columns: 1fr; } }

.db-title { font-size: var(--text-lg); font-weight: 700; margin-bottom: var(--space-xs); }
.db-status { font-size: var(--text-sm); font-weight: 600; margin-bottom: var(--space-sm); }
.db-flow { display: flex; align-items: center; gap: var(--space-xs); margin-bottom: var(--space-sm); }
.flow-step { padding: 2px 10px; border-radius: var(--radius-sm); font-size: var(--text-xs); color: var(--muted); border: 1px solid var(--line); }
.flow-step.on { background: var(--teal-light); color: var(--teal); border-color: var(--teal); font-weight: 600; }
.flow-arrow { color: var(--line); font-size: var(--text-sm); }
.db-meta { display: flex; gap: var(--space-base); font-size: var(--text-sm); color: var(--muted); }
.db-cat strong { color: var(--ink); }

.db-res-info { font-size: var(--text-base); margin-bottom: var(--space-xs); }
.db-note { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-xs); font-style: italic; }
.db-order { font-size: var(--text-sm); display: flex; align-items: center; gap: var(--space-xs); }
.db-no-res { font-size: var(--text-sm); color: var(--muted); padding: var(--space-base) 0; }

.db-actions { display: flex; gap: var(--space-sm); align-items: center; }
.db-idle { font-size: var(--text-xs); color: var(--muted); }

.slide-up-enter-active { transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-up-leave-active { transition: all 0.25s ease-in; }
.slide-up-enter-from { opacity: 0; transform: translateY(24px); }
.slide-up-leave-to { opacity: 0; transform: translateY(12px); }
</style>
