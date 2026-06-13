<template>
  <div class="page-enter-active">
    <!-- 顶部工具栏 -->
    <div class="admin-toolbar">
      <h2>审计日志</h2>
      <div class="toolbar-right">
        <el-button :icon="RefreshCw" circle size="small" :loading="loading" @click="loadLogs" />
        <span v-if="lastUpdated" class="refresh-hint">{{ lastUpdated.toLocaleTimeString() }} 刷新</span>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select v-model="filter.action" placeholder="操作类型" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="CREATE" value="CREATE" />
        <el-option label="UPDATE" value="UPDATE" />
        <el-option label="DELETE" value="DELETE" />
        <el-option label="STATUS_CHANGE" value="STATUS_CHANGE" />
      </el-select>
      <el-select v-model="filter.targetTable" placeholder="目标表" clearable style="width: 150px">
        <el-option label="全部" value="" />
        <el-option label="reservations" value="reservations" />
        <el-option label="orders" value="orders" />
        <el-option label="stores" value="stores" />
        <el-option label="dining_tables" value="dining_tables" />
        <el-option label="cats" value="cats" />
        <el-option label="menu_items" value="menu_items" />
        <el-option label="users" value="users" />
      </el-select>
      <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" />
      <el-button type="primary" @click="loadLogs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
      <span class="filter-count">共 {{ logs.length }} 条</span>
    </div>

    <!-- 日志表格 -->
    <div class="table-wrap">
      <el-table :data="logs" stripe style="width: 100%" v-loading="loading" row-key="id">
        <el-table-column prop="created_at" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column prop="user_name" label="操作人" width="120">
          <template #default="{ row }">
            <span v-if="row.user_name">{{ row.user_name }}</span>
            <span v-else class="text-muted">系统</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="130">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small" effect="plain">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target_table" label="目标表" width="140">
          <template #default="{ row }">
            <code class="target-table">{{ row.target_table }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="target_id" label="目标ID" width="80" align="center" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="isSuccess(row)" type="success" size="small" effect="plain">成功</el-tag>
            <el-tag v-else type="danger" size="small" effect="plain">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="200">
          <template #default="{ row }">
            <div class="detail-cell">
              <span v-if="row.detail?.method" class="detail-method">{{ row.detail.method }}</span>
              <span v-if="row.detail?.path" class="detail-path">{{ row.detail.path }}</span>
              <span v-if="row.detail?.status" class="detail-status">→ {{ row.detail.status }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ip_address" label="IP" width="130" />
        <el-table-column label="完整数据" width="80" align="center">
          <template #default="{ row }">
            <el-popover trigger="click" width="400" :show-after="200">
              <template #reference>
                <el-button size="small" link type="primary">查看</el-button>
              </template>
              <div class="json-preview">
                <pre>{{ JSON.stringify(row.detail, null, 2) }}</pre>
              </div>
            </el-popover>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!logs.length && !loading" description="暂无审计日志" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { RefreshCw } from '@lucide/vue'
import { api } from '@/utils/http'
import { usePolling } from '@/composables/usePolling'

interface AuditLog {
  id: number
  user_id: number | null
  user_name: string | null
  action: string
  target_table: string
  target_id: number | null
  detail: Record<string, unknown> | null
  ip_address: string | null
  created_at: string
}

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const filter = reactive({
  action: '',
  targetTable: '',
  dateRange: null as [string, string] | null,
})

function formatTime(t: string): string {
  if (!t) return '-'
  return t.replace('T', ' ').replace(/\.\d+Z?$/, '')
}

function actionTagType(action: string): string {
  const map: Record<string, string> = { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', STATUS_CHANGE: 'info' }
  return map[action] || 'info'
}

function isSuccess(row: { detail?: { status?: number } }): boolean {
  const status = row.detail?.status
  if (!status) return true
  return status >= 200 && status < 300
}

function resetFilter() {
  filter.action = ''
  filter.targetTable = ''
  filter.dateRange = null
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (filter.action) params.action = filter.action
    if (filter.targetTable) params.targetTable = filter.targetTable
    if (filter.dateRange?.[0]) params.startDate = filter.dateRange[0]
    if (filter.dateRange?.[1]) params.endDate = filter.dateRange[1] + ' 23:59:59'
    logs.value = await api.get<AuditLog[]>('/audit-logs', params)
  } catch {
    // API 不存在时使用 fallback 数据
    const { fallbackAuditLogs } = await import('@/utils/fallback')
    logs.value = fallbackAuditLogs as AuditLog[]
  } finally {
    loading.value = false
  }
}

const { lastUpdated, startWithChangeDetection } = usePolling(loadLogs, 10000)
startWithChangeDetection(() => logs.value.map(l => l.id).sort().join(','))
loadLogs()
</script>

<style scoped>
.admin-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.admin-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; align-items: center; gap: var(--space-sm); }
.refresh-hint { font-size: var(--text-xs); color: var(--muted); }

.filter-bar {
  display: flex; gap: var(--space-sm); margin-bottom: var(--space-base);
  flex-wrap: wrap; align-items: center;
}
.filter-count { font-size: var(--text-xs); color: var(--muted); margin-left: auto; }

.table-wrap {
  background: var(--paper); border-radius: var(--radius-lg);
  border: 1px solid var(--line); overflow: hidden;
}

.text-muted { color: var(--muted); font-size: var(--text-xs); }
.target-table {
  font-size: var(--text-xs); padding: 2px 6px; border-radius: 4px;
  background: #f3f4f6; color: #374151;
}
.detail-cell { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-xs); }
.detail-method { font-weight: 700; color: var(--teal); }
.detail-path { color: var(--muted); font-family: var(--font-mono); }
.detail-status { color: var(--success); }

.json-preview {
  max-height: 300px; overflow-y: auto;
}
.json-preview pre {
  font-size: 11px; line-height: 1.5; margin: 0;
  white-space: pre-wrap; word-break: break-all;
  font-family: var(--font-mono);
}
</style>
