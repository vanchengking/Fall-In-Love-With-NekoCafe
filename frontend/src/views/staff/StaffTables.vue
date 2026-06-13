<template>
  <div>
    <div class="page-header">
      <h2 style="margin: 0">桌位状态</h2>
      <span v-if="lastUpdated" class="refresh-hint">
        <span v-if="loadFailed" class="refresh-failed">刷新失败，显示上次数据 · </span>
        {{ lastUpdated.toLocaleTimeString() }} 自动刷新中
      </span>
    </div>

    <el-form inline class="filter-bar" @submit.prevent>
      <el-form-item label="门店">
        <el-select v-model="filters.storeId" style="width: 200px" @change="refresh">
          <el-option v-for="s in stores" :key="s.id" :label="`${s.city} · ${s.name}`" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" :clearable="false" style="width: 145px" @change="refresh" />
      </el-form-item>
      <el-form-item label="时间">
        <el-select v-model="filters.time" style="width: 105px" @change="refresh">
          <el-option v-for="slot in timeSlots" :key="slot" :label="slot" :value="slot" />
        </el-select>
      </el-form-item>
      <el-form-item label="区域">
        <el-select v-model="filters.area" style="width: 110px" @change="refresh">
          <el-option label="全部" value="" />
          <el-option label="靠窗区" value="window" />
          <el-option label="主厅" value="main" />
          <el-option label="派对区" value="party" />
          <el-option label="安静区" value="quiet" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.runtimeStatus" style="width: 110px">
          <el-option label="全部" value="" />
          <el-option label="空闲" value="free" />
          <el-option label="已预约" value="reserved" />
          <el-option label="使用中" value="in_use" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>
    </el-form>

    <div class="table-grid">
      <div v-for="table in visibleTables" :key="table.id" class="table-card" :class="`state-${table.runtime_status || 'free'}`">
        <div class="card-top">
          <span class="table-code">{{ table.code }}</span>
          <el-tag :type="runtimeTagType(table.runtime_status)" size="small">{{ table.runtime_status_label || '空闲' }}</el-tag>
        </div>
        <div class="table-info">
          {{ table.seats }}人 · {{ areaLabel(table.area) }}
          <el-tag v-if="table.cat_zone" size="small" type="success">猫区</el-tag>
        </div>
        <div v-if="table.runtime_status === 'disabled'" class="table-sub">{{ baseStatusLabel(table.status) }} · 不可操作</div>

        <div v-if="table.current_reservation_id" class="res-block">
          <div class="res-line">
            预约 #{{ table.current_reservation_id }} ·
            {{ table.current_reservation_status_label || statusLabel(table.current_reservation_status || '') }}
          </div>
          <div class="res-line">
            {{ table.customer_name || '顾客' }}<template v-if="table.mobile_number"> · {{ table.mobile_number }}</template>
          </div>
          <div class="res-line">{{ table.party_size }}人 · {{ filters.date }} {{ table.reservation_time }}</div>
          <!-- 仅渲染状态机允许的跳转（booked→seated/no_show/cancelled, seated→dining, dining→finished, created→cancelled） -->
          <div v-if="table.runtime_status !== 'disabled'" class="res-actions">
            <template v-if="table.current_reservation_status === 'booked'">
              <el-button size="small" type="success" :disabled="acting" @click="updateStatus(table, 'seated')">入座</el-button>
              <el-button size="small" :disabled="acting" @click="updateStatus(table, 'no_show')">标记未到</el-button>
              <el-button size="small" type="danger" plain :disabled="acting" @click="updateStatus(table, 'cancelled')">取消</el-button>
            </template>
            <template v-else-if="table.current_reservation_status === 'seated'">
              <el-button size="small" type="warning" :disabled="acting" @click="updateStatus(table, 'dining')">开始用餐</el-button>
            </template>
            <template v-else-if="table.current_reservation_status === 'dining'">
              <el-button size="small" :disabled="acting" @click="updateStatus(table, 'finished')">完桌</el-button>
            </template>
            <template v-else-if="table.current_reservation_status === 'created'">
              <el-button size="small" type="danger" plain :disabled="acting" @click="updateStatus(table, 'cancelled')">取消</el-button>
            </template>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!visibleTables.length" description="没有符合条件的桌位" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/utils/http'
import { areaLabel, statusLabel } from '@/utils/format'
import { usePolling } from '@/composables/usePolling'
import { fallbackStores, fallbackTables } from '@/utils/fallback'
import type { ReservationStatus, StaffTableStatus, Store, TableRuntimeStatus } from '@/types'

const stores = ref<Store[]>([])
const tables = ref<StaffTableStatus[]>([])
const loadFailed = ref(false)
const acting = ref(false)

/** 常用预约时段：10:00–22:00 每半小时 */
const timeSlots = (() => {
  const slots: string[] = []
  for (let h = 10; h <= 22; h++) {
    slots.push(`${String(h).padStart(2, '0')}:00`)
    if (h < 22) slots.push(`${String(h).padStart(2, '0')}:30`)
  }
  return slots
})()

function localToday(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 当前时间向后取整到半小时，并收敛到常用时段范围内 */
function defaultTimeSlot(): string {
  const now = new Date()
  let h = now.getHours()
  let m: number
  if (now.getMinutes() === 0) m = 0
  else if (now.getMinutes() <= 30) m = 30
  else { h += 1; m = 0 }
  const slot = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
  if (slot < timeSlots[0]) return timeSlots[0]
  if (slot > timeSlots[timeSlots.length - 1]) return timeSlots[timeSlots.length - 1]
  return slot
}

const filters = reactive({
  storeId: 1,
  date: localToday(),
  time: defaultTimeSlot(),
  area: '',
  runtimeStatus: '' as '' | TableRuntimeStatus,
})

const visibleTables = computed(() =>
  filters.runtimeStatus
    ? tables.value.filter(t => (t.runtime_status || 'free') === filters.runtimeStatus)
    : tables.value,
)

async function loadTables() {
  const params: Record<string, unknown> = {
    storeId: filters.storeId,
    date: filters.date,
    time: filters.time,
  }
  if (filters.area) params.area = filters.area
  try {
    // 实时状态以 /admin/tables 单一接口为准（后端按 storeId/date/time 计算），避免多接口拼算口径不一致
    tables.value = await api.get<StaffTableStatus[]>('/admin/tables', params)
    loadFailed.value = false
  } catch {
    loadFailed.value = true
    if (!tables.value.length) {
      // 离线演示降级：仅基础桌位信息，统一按空闲展示
      tables.value = fallbackTables
        .filter(t => t.store_id === filters.storeId && (!filters.area || t.area === filters.area))
        .map(t => ({ ...t, runtime_status: 'free' as TableRuntimeStatus, runtime_status_label: '空闲' }))
    }
  }
}

const { refresh, lastUpdated } = usePolling(loadTables, 10000)

onMounted(async () => {
  try {
    stores.value = await api.get<Store[]>('/stores')
  } catch {
    stores.value = []
  }
  if (!stores.value.length) stores.value = fallbackStores
  // AuthUser 无所属门店字段，默认 1 号店；列表里没有 1 号店时退到第一家
  if (!stores.value.some(s => s.id === filters.storeId)) {
    filters.storeId = stores.value[0].id
    refresh()
  }
})

async function updateStatus(table: StaffTableStatus, status: ReservationStatus) {
  if (!table.current_reservation_id) return
  acting.value = true
  try {
    await api.patch(`/reservations/${table.current_reservation_id}/status`, { status })
    ElMessage.success(`桌位 ${table.code}：预约 #${table.current_reservation_id} → ${statusLabel(status)}`)
    await refresh()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    acting.value = false
  }
}

function runtimeTagType(status?: TableRuntimeStatus): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<TableRuntimeStatus, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    free: 'success',
    reserved: 'warning',
    in_use: 'danger',
    disabled: 'info',
  }
  return map[status || 'free']
}

function baseStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    maintenance: '维护中',
    reserved: '保留',
    occupied: '占用',
  }
  return map[status || ''] || status || '停用'
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.refresh-hint { font-size: 12px; color: #667085; }
.refresh-failed { color: #d92d20; }
.filter-bar { margin-bottom: 8px; }
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
.table-card { background: #fff; padding: 16px; border-radius: 12px; border: 1px solid #e8e5df; border-left-width: 4px; }
.table-card.state-free { border-left-color: #34d399; }
.table-card.state-reserved { border-left-color: #fbbf24; }
.table-card.state-in_use { border-left-color: #f87171; }
.table-card.state-disabled { border-left-color: #c0c4cc; background: #fafafa; }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.table-code { font-size: 22px; font-weight: 700; color: #172033; }
.table-info { font-size: 13px; color: #667085; margin-top: 6px; display: flex; align-items: center; gap: 6px; }
.table-sub { font-size: 12px; color: #98a2b3; margin-top: 6px; }
.res-block { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e8e5df; }
.res-line { font-size: 13px; color: #475467; margin-top: 2px; }
.res-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.res-actions .el-button + .el-button { margin-left: 0; }
</style>
