<template>
  <div class="page-enter-active">
    <!-- 顶部工具栏 -->
    <div class="staff-toolbar">
      <h2>预约管理与状态机</h2>
      <div class="toolbar-right">
        <el-button :icon="RefreshCw" circle size="small" @click="loadData" />
        <span v-if="lastUpdated" class="refresh-hint">{{ lastUpdated.toLocaleTimeString() }} 刷新</span>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-input v-model="filter.phone" placeholder="手机号（模糊搜索）" clearable style="width: 170px" />
      <el-date-picker v-model="filter.date" type="date" placeholder="日期" value-format="YYYY-MM-DD" style="width: 150px" />
      <el-select v-model="filter.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="全部" value="" />
        <el-option label="已预约" value="booked" />
        <el-option label="已入座" value="seated" />
        <el-option label="用餐中" value="dining" />
        <el-option label="已完成" value="finished" />
        <el-option label="已取消" value="cancelled" />
        <el-option label="未到店" value="no_show" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
      <span class="filter-hint">预约状态严格流转，禁止跳转</span>
    </div>

    <!-- 状态流程图（含"用餐中"，高亮当前筛选状态） -->
    <div class="flow-diagram">
      <div class="flow-main">
        <span
          v-for="node in flowNodes" :key="node.key"
          class="flow-node"
          :class="[node.cls, { 'fn-highlight': filter.status === node.key }]"
        >{{ node.label }}</span>
      </div>
      <div class="flow-branches">
        <span class="flow-branch">
          <span class="flow-arrow-sm">↓</span> 已取消
        </span>
        <span class="flow-branch">
          <span class="flow-arrow-sm">↓</span> 未到店
        </span>
        <span class="flow-forbidden">✗ 禁止 seated → finished</span>
      </div>
    </div>

    <!-- 预约表格 -->
    <div class="table-wrap">
      <el-table :data="filteredReservations" stripe style="width: 100%" @row-click="selectRow" highlight-current-row>
        <el-table-column prop="reservation_time" label="时间" width="80" />
        <el-table-column label="顾客" width="100">
          <template #default="{ row }">
            <div class="tbl-customer-cell">
              <span class="tbl-avatar" :style="{ background: avatarBg(row.customer_name) }">{{ (row.customer_name||'?')[0] }}</span>
              {{ row.customer_name || row.customerName || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile_number" label="手机号" width="120" />
        <el-table-column prop="party_size" label="人数" width="60" align="center" />
        <el-table-column prop="table_code" label="桌位" width="80">
          <template #default="{ row }">{{ row.table_code || '待分配' }}</template>
        </el-table-column>
        <el-table-column prop="cat_name" label="猫咪" width="80">
          <template #default="{ row }">{{ row.cat_name || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下一步" width="110">
          <template #default="{ row }">
            <span class="next-step" :class="'ns-' + row.status">{{ nextStepLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="tbl-actions">
              <!-- booked: 入座（主操作） + 更多（次操作） -->
              <template v-if="row.status === 'booked'">
                <el-button size="small" type="success" @click.stop="confirmAction(row, 'seated')">入座</el-button>
                <el-dropdown trigger="click" @command="(cmd: string) => confirmAction(row, cmd)">
                  <el-button size="small" :icon="MoreFilled" circle plain @click.stop />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="no_show">标记未到</el-dropdown-item>
                      <el-dropdown-item command="cancelled" divided>取消预约</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <!-- seated: 开始用餐 -->
              <el-button v-else-if="row.status === 'seated'" size="small" type="primary" @click.stop="confirmAction(row, 'dining')">开始用餐</el-button>
              <!-- dining: 完桌 -->
              <el-button v-else-if="row.status === 'dining'" size="small" type="primary" @click.stop="confirmAction(row, 'finished')">完桌</el-button>
              <!-- finished / cancelled / no_show: 无操作 -->
              <span v-else class="tbl-no-action">-</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 事件记录区（表格下方，高度约 80px 可滚动） -->
    <section class="event-log">
      <div class="el-header">
        <h3>事件记录</h3>
        <span v-if="selectedReservation" class="el-customer">
          {{ selectedReservation.customer_name || selectedReservation.customerName || '' }}
        </span>
      </div>
      <div class="el-scroll">
        <el-table v-if="selectedReservation" :data="eventLogs" size="small" stripe style="width: 100%">
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="action" label="动作" />
          <el-table-column prop="operator" label="操作人" width="120" />
        </el-table>
        <div v-else class="el-empty">点击上方表格行查看事件流水</div>
      </div>
    </section>

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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshCw } from '@lucide/vue'
import { MoreFilled } from '@element-plus/icons-vue'
import { api } from '@/utils/http'
import { statusLabel, statusType, avatarBg } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import { fallbackReservations } from '@/utils/fallback'
import type { Reservation } from '@/types'

const reservations = ref<Reservation[]>([])
const today = new Date().toISOString().slice(0, 10)
const filter = ref({ phone: '', date: today, status: '' })
const selectedReservation = ref<Reservation | null>(null)

const confirmDialog = ref(false)
const confirmLoading = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmBtnType = ref<'primary' | 'success' | 'danger'>('primary')
let pendingTarget: Reservation | null = null
let pendingAction = ''

// ── 流程图节点（含"用餐中"） ──
const flowNodes = [
  { key: 'booked',   label: '已预约', cls: 'fn-booked' },
  { key: 'seated',   label: '已入座', cls: 'fn-seated' },
  { key: 'dining',   label: '用餐中', cls: 'fn-dining' },
  { key: 'finished', label: '已完成', cls: 'fn-finished' },
]

// ── 下一步标签 ──
function nextStepLabel(status: string): string {
  const map: Record<string, string> = {
    booked:   '→ 已入座',
    seated:   '→ 用餐中',
    dining:   '→ 已完成',
    finished: '已完成',
    cancelled: '已取消',
    no_show:  '未到店',
  }
  return map[status] || '-'
}
}

// ── 筛选（手机号模糊搜索，日期默认今天） ──
const filteredReservations = computed(() => {
  return reservations.value.filter(r => {
    if (filter.value.phone) {
      const phone = r.mobile_number || ''
      if (!phone.includes(filter.value.phone)) return false
    }
    if (filter.value.date && r.reservation_date !== filter.value.date) return false
    if (filter.value.status && r.status !== filter.value.status) return false
    return true
  })
})

// ── 事件记录 ──
const eventLogs = computed(() => {
  const r = selectedReservation.value
  if (!r) return []
  const logs: { time: string; action: string; operator: string }[] = []
  const baseDate = r.reservation_date || ''
  const baseTime = r.reservation_time || ''

  logs.push({ time: `${baseDate} ${baseTime}`, action: '预约创建', operator: '系统自动' })

  if (r.created_at && r.created_at !== `${baseDate} ${baseTime}`) {
    logs.push({ time: r.created_at, action: '预约确认', operator: '系统自动' })
  }
  if (['seated', 'dining', 'finished'].includes(r.status)) {
    logs.push({ time: `${baseDate} ${baseTime}`, action: '顾客入座', operator: '高店员' })
  }
  if (['dining', 'finished'].includes(r.status)) {
    logs.push({ time: `${baseDate} ${baseTime}`, action: '开始用餐', operator: '高店员' })
  }
  if (r.status === 'finished') {
    logs.push({ time: `${baseDate} ${baseTime}`, action: '完桌', operator: '高店员' })
  }
  if (r.status === 'cancelled') {
    logs.push({ time: `${baseDate} ${baseTime}`, action: '取消预约', operator: '高店员' })
  }
  if (r.status === 'no_show') {
    logs.push({ time: `${baseDate} ${baseTime}`, action: '标记未到', operator: '高店员' })
  }
  return logs
})

// ── 工具函数 ──
function selectRow(row: Reservation) {
  selectedReservation.value = row
}

function resetFilter() {
  filter.value = { phone: '', date: today, status: '' }
}

// ── 操作确认 ──
function confirmAction(r: Reservation, action: string) {
  pendingTarget = r; pendingAction = action
  const name = r.customer_name || r.customerName || '顾客'
  const map: Record<string, { t: string; m: string; b: 'primary' | 'success' | 'danger' }> = {
    seated:    { t: '确认入座', m: `${name}（${r.party_size}人）→ ${r.table_code || '待分配'}`, b: 'success' },
    dining:    { t: '开始用餐', m: `确认 ${name} 开始用餐？`, b: 'primary' },
    finished:  { t: '确认完桌', m: `确认 ${name} 完桌？桌位将释放。`, b: 'primary' },
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
    selectedReservation.value = null
    await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { confirmLoading.value = false }
}

// ── 数据加载 ──
async function loadData() {
  try { reservations.value = await api.get<Reservation[]>('/reservations') }
  catch { reservations.value = fallbackReservations }
}

const { lastUpdated, startWithChangeDetection } = usePolling(loadData, 10000)
startWithChangeDetection(() => reservations.value.map(r => r.id).sort().join(','))
loadData()
</script>

<style scoped>
.staff-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.staff-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.refresh-hint { font-size: var(--text-xs); color: var(--muted); }

/* ── 筛选区 ── */
.filter-bar {
  display: flex; gap: var(--space-sm); margin-bottom: var(--space-base); flex-wrap: wrap; align-items: center;
}
.filter-hint { font-size: var(--text-xs); color: var(--coral); margin-left: auto; }

/* ── 状态流程图 ── */
.flow-diagram {
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-lg);
  padding: var(--space-lg) var(--space-xl); margin-bottom: var(--space-base);
}
.flow-main {
  display: flex; align-items: center; gap: var(--space-base); flex-wrap: wrap; margin-bottom: var(--space-base);
}
.flow-node {
  padding: var(--space-sm) var(--space-lg); border-radius: var(--radius-md);
  font-size: var(--text-base); font-weight: 700; border: 2px solid;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}
.flow-main .flow-node:not(:last-child)::after {
  content: '→'; margin: 0 var(--space-sm); color: var(--muted); font-size: var(--text-xl); font-weight: 400;
}
.fn-booked   { background: var(--status-booked-bg); border-color: var(--status-booked); color: #1e40af; }
.fn-seated   { background: var(--status-seated-bg); border-color: var(--status-seated); color: #115e59; }
.fn-dining   { background: var(--status-dining-bg); border-color: var(--status-dining); color: #991b1b; }
.fn-finished { background: var(--status-finished-bg); border-color: var(--status-finished); color: #166534; }
.fn-highlight {
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.35);
  transform: scale(1.08);
}
.flow-branches { display: flex; gap: var(--space-lg); font-size: var(--text-sm); color: var(--muted); }
.flow-branch { display: flex; align-items: center; gap: var(--space-xs); }
.flow-arrow-sm { font-size: var(--text-lg); }
.flow-forbidden { color: var(--status-dining); font-weight: 600; }

/* ── 表格 ── */
.table-wrap { background: var(--paper); border-radius: var(--radius-lg); border: 1px solid var(--line); overflow: hidden; margin-bottom: var(--space-base); }
:deep(.el-table th.el-table__cell) { padding: 14px 0; }
:deep(.el-table td.el-table__cell) { padding: 12px 0; }
.tbl-customer-cell { display: flex; align-items: center; gap: 6px; }
.tbl-avatar { width: 28px; height: 28px; border-radius: 50%; display: inline-grid; place-items: center; font-size: 12px; font-weight: 700; color: var(--ink); flex-shrink: 0; }
.next-step { font-size: var(--text-xs); font-weight: 600; }
.ns-booked   { color: var(--status-booked); }
.ns-seated   { color: var(--status-seated); }
.ns-dining   { color: var(--status-dining); }
.ns-finished { color: var(--status-finished); }
.ns-cancelled { color: var(--status-cancelled); }
.ns-no_show  { color: var(--status-noshow); }
.tbl-actions { display: flex; gap: var(--space-xs); align-items: center; }
.tbl-no-action { font-size: var(--text-xs); color: var(--muted); }

/* ── 事件记录区（紧凑） ── */
.event-log {
  background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius-lg);
  padding: var(--space-xs) var(--space-sm);
}
.el-header {
  display: flex; align-items: center; gap: var(--space-sm); margin-bottom: 2px;
}
.el-header h3 { font-size: var(--text-sm); margin: 0; }
.el-customer { font-size: var(--text-xs); color: var(--teal); font-weight: 600; }
.el-scroll {
  max-height: 100px; overflow-y: auto;
}
:deep(.event-log .el-table td.el-table__cell) { padding: 4px 0; }
:deep(.event-log .el-table th.el-table__cell) { padding: 6px 0; font-size: var(--text-xs); }
.el-empty {
  text-align: center; padding: var(--space-xs); font-size: var(--text-xs); color: var(--muted);
}
</style>
