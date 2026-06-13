<template>
  <div class="page-enter-active">
    <h2>预约桌位</h2>
    <p class="subtitle">选择日期时段 → 在店内布局中点选桌位 → 填写信息 → 提交预约</p>

    <!-- 偏好标签 -->
    <div class="pref-bar">
      <span class="pref-label">偏好标签：</span>
      <PreferenceChips v-model="form.preferences" :options="preferenceOptions" @change="refreshRecommendations" />
    </div>

    <div class="res-layout">
      <!-- 左：店内布局 + 预约表单 -->
      <div class="left-col">
        <!-- 店内布局图 -->
        <div class="floor-plan-panel">
          <div class="floor-header">
            <h3>店内布局</h3>
            <div class="legend">
              <span class="legend-item"><span class="fp-dot fp-dot-free"></span>可选</span>
              <span class="legend-item"><span class="fp-dot fp-dot-occupied"></span>已占用</span>
              <span class="legend-item"><span class="fp-dot fp-dot-disabled"></span>停用</span>
              <span class="legend-item"><span class="fp-dot fp-dot-selected"></span>已选</span>
              <span class="legend-item"><span class="fp-dot fp-dot-cat"></span>猫区</span>
            </div>
          </div>

          <FloorPlan
            :tables="tables"
            :selected-id="form.tableId ? Number(form.tableId) : null"
            mode="customer"
            :show-decorations="true"
            @select="selectTable"
          />

          <div v-if="form.tableId" class="selected-table-info">
            已选桌位：<strong>{{ selectedTableObj?.code }}</strong>
            （{{ selectedTableObj?.seats }}人 · {{ tagLabel(selectedTableObj?.area || '') }}）
            <el-button text type="primary" size="small" @click="form.tableId = ''">取消选择</el-button>
          </div>
        </div>

        <!-- 预约信息表单 -->
        <div class="form-panel">
          <h3 class="form-title">预约信息</h3>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="createReservation">
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="姓名"><el-input v-model="form.customerName" placeholder="请输入姓名" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号"><el-input v-model="form.mobileNumber" placeholder="请输入手机号" /></el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="日期">
                  <el-date-picker v-model="form.reservationDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="onSlotChange" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="时间">
                  <el-time-picker v-model="form.reservationTime" format="HH:mm" value-format="HH:mm" style="width: 100%" @change="onSlotChange" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="人数">
                  <el-input-number v-model="form.partySize" :min="1" :max="12" style="width: 100%" @change="onSlotChange" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注"><el-input v-model="form.note" type="textarea" :rows="2" placeholder="特殊需求（可选）" /></el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" size="large" style="width: 100%">
              提交预约
            </el-button>
          </el-form>
        </div>
      </div>

      <!-- 右：推荐 + 菜品 -->
      <div class="right-col">
        <!-- 推荐猫咪 -->
        <div v-if="recommendations.cat" class="rec-cat-card" @click="form.recommendedCatId = recommendations.cat!.id">
          <img v-if="recommendations.cat.photo_url" :src="recommendations.cat.photo_url" :alt="recommendations.cat.name" class="rec-cat-photo" />
          <div v-else class="rec-cat-avatar"><Cat :size="28" /></div>
          <div>
            <div class="rec-cat-name">{{ recommendations.cat.name }}</div>
            <div class="rec-cat-desc">{{ recommendations.cat.breed }} · {{ tagLabels(recommendations.cat.personality_tags) }}</div>
          </div>
          <el-tag v-if="form.recommendedCatId === recommendations.cat.id" type="success" size="small">已选</el-tag>
        </div>

        <!-- 推荐桌位 -->
        <div class="rec-section">
          <h4>推荐桌位</h4>
          <div class="rec-table-grid">
            <div v-for="table in recommendations.tables" :key="table.id"
              class="rec-table-card" :class="{ selected: form.tableId === table.id, unavailable: !isSelectable(table) }"
              @click="selectTable(table)">
              <div class="rec-table-code">{{ table.code }}</div>
              <div class="rec-table-info">{{ table.seats }}人 · {{ tagLabel(table.area) }}</div>
              <el-tag v-if="!isSelectable(table)" size="small" type="info">不可选</el-tag>
              <el-tag v-else-if="table.cat_zone" size="small" type="success">猫区</el-tag>
            </div>
          </div>
        </div>

        <!-- 推荐菜品（可加入购物车） -->
        <div class="rec-section">
          <h4>推荐菜品</h4>
          <div class="menu-list">
            <div v-for="item in recommendations.menuItems" :key="item.id" class="menu-card">
              <img v-if="item.photo_url" :src="item.photo_url" :alt="item.name" class="menu-photo" />
              <div class="menu-info">
                <strong>{{ item.name }}</strong>
                <span class="menu-meta">{{ tagLabels([item.category]) }} · {{ tagLabels(item.tags) }}</span>
              </div>
              <div class="menu-right">
                <span class="menu-price">{{ cents(item.price_cents) }}</span>
                <el-input-number
                  :model-value="cart.selectedMenu[item.id] || 0"
                  @update:model-value="cart.setQuantity(item.id, $event)"
                  :min="0" :max="9" size="small" />
              </div>
            </div>
          </div>
        </div>

        <!-- 购物车汇总 -->
        <div v-if="cart.selectedOrderItems.length > 0" class="cart-summary">
          <div class="cart-row" v-for="item in cart.selectedOrderItems" :key="item.menuItemId">
            <span>{{ item.name }} × {{ item.quantity }}</span>
            <span>{{ cents(item.subtotal) }}</span>
          </div>
          <div class="cart-total">
            <span>合计</span>
            <span>{{ cents(cart.orderTotal) }}</span>
          </div>
          <el-button type="primary" plain class="cart-order-btn" @click="handleCreateOrder">
            生成点单 {{ cents(cart.orderTotal) }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Cat, Armchair } from '@lucide/vue'
import FloorPlan from '@/components/FloorPlan.vue'
import PreferenceChips from '@/components/PreferenceChips.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { api, type ApiError } from '@/utils/http'
import { tomorrow, cents, tagLabel, tagLabels } from '@/utils/format'
import { fallbackTables, fallbackCats, fallbackMenuItems } from '@/utils/fallback'
import type { DiningTable, Cat as CatType, MenuItem, Reservation, Recommendations as RecType } from '@/types'

const cart = useCartStore()
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref()

const rules = {
  customerName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobileNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  reservationDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  reservationTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
}
const tables = ref<DiningTable[]>([])
const cats = ref<CatType[]>([])
const menuItems = ref<MenuItem[]>([])
const recommendations = ref<RecType>({ cat: null, tables: [], menuItems: [] })
const preferenceOptions = ['quiet', 'window', 'sweet', 'coffee', 'photo', 'healthy']

const form = reactive({
  customerName: auth.user?.name || auth.user?.mobileNumber || '',
  mobileNumber: auth.user?.mobileNumber || auth.user?.mobile_number || '',
  storeId: 1,
  reservationDate: tomorrow(), reservationTime: '18:30',
  partySize: 2, tableId: '' as string | number,
  recommendedCatId: '' as string | number,
  preferences: ['quiet', 'window'] as string[],
  note: '',
})

const selectedTableObj = computed(() => tables.value.find(t => t.id === form.tableId))

/** 桌位查询参数：始终携带当前时段与人数，避免无时段列表被当成全部可选 */
function tableQuery() {
  return {
    storeId: form.storeId, date: form.reservationDate,
    time: form.reservationTime, partySize: form.partySize,
  }
}

/** 桌位自身状态正常且当前时段未被占用才可选 */
function isSelectable(table: DiningTable) {
  return table.available_for_slot !== false && (!table.status || table.status === 'available')
}

function selectTable(table: DiningTable) {
  if (!isSelectable(table)) return
  form.tableId = form.tableId === table.id ? '' : table.id
}

/** 已选桌位被过滤（容量不足）或已不可选（占用/停用）时清空选择 */
function pruneStaleSelection() {
  if (form.tableId === '') return
  const selected = tables.value.find(t => t.id === form.tableId)
  if (!selected || !isSelectable(selected)) form.tableId = ''
}

async function refreshTables() {
  try { tables.value = await api.get<DiningTable[]>('/tables', tableQuery()) }
  catch { tables.value = fallbackTables }
  pruneStaleSelection()
}

/** 日期/时间/人数变化：同步刷新桌位可用性与推荐结果 */
async function onSlotChange() {
  await Promise.all([refreshTables(), refreshRecommendations()])
}

/** 加载当前门店的桌位/猫咪/菜单（桌位带当前时段与人数） */
async function loadStoreData() {
  try {
    const [t, c, m] = await Promise.all([
      api.get<DiningTable[]>('/tables', tableQuery()),
      api.get<CatType[]>('/cats', { storeId: form.storeId }),
      api.get<MenuItem[]>('/menu-items', { storeId: form.storeId }),
    ])
    tables.value = t; cats.value = c; menuItems.value = m; cart.menuItems = m
  } catch {
    tables.value = fallbackTables; cats.value = fallbackCats; menuItems.value = fallbackMenuItems
  }
  pruneStaleSelection()
}

/** 切换门店：清空已选桌位/推荐猫咪，重载该店数据与推荐 */
async function onStoreChange() {
  form.tableId = ''
  form.recommendedCatId = ''
  await loadStoreData()
  await refreshRecommendations()
}

async function refreshRecommendations() {
  try {
    const rec = await api.get<RecType>('/recommendations', {
      userId: auth.user?.id || 1, storeId: form.storeId, preferences: form.preferences.join(','),
      date: form.reservationDate, time: form.reservationTime, partySize: form.partySize,
    })
    recommendations.value = rec
    if (rec.cat) form.recommendedCatId = rec.cat.id
  } catch {
    recommendations.value = { cat: cats.value[0] || null, tables: tables.value.filter(isSelectable).slice(0, 3), menuItems: menuItems.value.slice(0, 3) }
  }
}

async function createReservation() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const created = await api.post<Reservation>('/reservations', { ...form })
    cart.setReservationId(created.id)
    ElMessage.success(`预约成功！桌位：${created.table_code || '自动分配'}`)
    // 刚预约的桌位在当前时段已被占用，同步刷新布局与推荐（并由 prune 清空该选择）
    await Promise.all([refreshTables(), refreshRecommendations()])
  } catch (e) {
    const err = e as ApiError
    ElMessage.error(err.message)
    if (err.status === 409) {
      // 并发冲突：该时段桌位已被他人抢占，刷新当前时段可用性，占用桌位立即置灰并清除失效选择
      await Promise.all([refreshTables(), refreshRecommendations()])
    }
  }
  finally { loading.value = false }
}

async function handleCreateOrder() {
  try {
    const order = await cart.createOrder()
    ElMessage.success(`订单已生成：${cents((order as { total_cents: number }).total_cents)}`)
  } catch (e) { ElMessage.error((e as Error).message) }
}

onMounted(async () => {
  try {
    const [t, c, m] = await Promise.all([
      api.get<DiningTable[]>('/tables', { storeId: form.storeId }),
      api.get<CatType[]>('/cats', { storeId: form.storeId }),
      api.get<MenuItem[]>('/menu-items', { storeId: form.storeId }),
    ])
    tables.value = t; cats.value = c; menuItems.value = m; cart.menuItems = m
  } catch {
    tables.value = fallbackTables; cats.value = fallbackCats; menuItems.value = fallbackMenuItems
  }
  await refreshRecommendations()
})
</script>

<style scoped>
.subtitle { color: var(--muted); font-size: var(--text-sm); margin-bottom: var(--space-lg); }
.pref-bar { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-lg); flex-wrap: wrap; }
.pref-label { font-size: var(--text-sm); color: var(--muted); flex-shrink: 0; }
.res-layout { display: grid; grid-template-columns: 1fr 380px; gap: var(--space-lg); }
@media (max-width: 1024px) { .res-layout { grid-template-columns: 1fr; } }

/* 店内布局 */
.floor-plan-panel { margin-bottom: var(--space-base); }
.floor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-sm); }
.floor-header h3 { margin: 0; }
.legend { display: flex; gap: var(--space-sm); font-size: var(--text-xs); color: var(--muted); }
.legend-item { display: flex; align-items: center; gap: var(--space-xs); }
.fp-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.fp-dot-free { background: #86efac; border: 1px solid #4ade80; }
.fp-dot-occupied { background: var(--line); border: 1px solid #d1d5db; }
.fp-dot-disabled { background: var(--wash); border: 1px dashed #9ca3af; }
.fp-dot-selected { background: var(--teal); border: 1px solid var(--teal-dark); }
.fp-dot-cat { background: #86efac; border: 1px solid #4ade80; }

.selected-table-info { margin-top: var(--space-sm); font-size: var(--text-xs); color: var(--teal); display: flex; align-items: center; gap: var(--space-sm); }

/* 预约表单 */
.form-panel { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.form-title { margin-bottom: var(--space-base); }

/* 右侧推荐 */
.right-col { display: flex; flex-direction: column; gap: var(--space-base); }
.rec-cat-card { display: flex; align-items: center; gap: var(--space-base); padding: var(--space-base); background: var(--teal-light); border-radius: var(--radius-lg); cursor: pointer; border: 2px solid transparent; transition: border-color 0.15s; }
.rec-cat-card:hover { border-color: var(--teal); }
.rec-cat-photo { width: 56px; height: 56px; object-fit: cover; border-radius: var(--radius-md); }
.rec-cat-avatar { color: var(--teal); }
.rec-cat-name { font-size: var(--text-base); font-weight: 700; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-cat-desc { font-size: var(--text-xs); color: var(--muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.rec-section { background: var(--paper); padding: var(--space-base); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.rec-section h4 { margin: 0 0 var(--space-sm); font-size: var(--text-sm); color: var(--ink); }
.rec-table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: var(--space-sm); }
.rec-table-card { padding: var(--space-sm); border: 2px solid var(--line); border-radius: var(--radius-md); text-align: center; cursor: pointer; transition: all 0.15s; }
.rec-table-card:hover { border-color: var(--teal); }
.rec-table-card.selected { border-color: var(--teal); background: var(--teal-light); }
.rec-table-card.unavailable { opacity: 0.5; cursor: not-allowed; }
.rec-table-card.unavailable:hover { border-color: var(--line); }
.rec-table-code { font-size: var(--text-lg); font-weight: 700; }
.rec-table-info { font-size: var(--text-xs); color: var(--muted); margin-top: 2px; }

.menu-list { display: flex; flex-direction: column; gap: var(--space-sm); }
.menu-card { display: flex; justify-content: space-between; align-items: center; padding: var(--space-sm); background: var(--wash); border-radius: var(--radius-md); gap: var(--space-sm); }
.menu-photo { width: 48px; height: 48px; object-fit: cover; border-radius: var(--radius-md); flex-shrink: 0; }
.menu-info strong { font-size: var(--text-sm); color: var(--ink); }
.menu-meta { display: block; font-size: var(--text-xs); color: var(--muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-right { display: flex; align-items: center; gap: var(--space-sm); }
.menu-price { font-weight: 700; color: var(--coral); font-size: var(--text-sm); }

.cart-summary { background: var(--paper); padding: var(--space-base); border-radius: var(--radius-lg); border: 2px solid var(--teal); }
.cart-row { display: flex; justify-content: space-between; font-size: var(--text-xs); padding: var(--space-xs) 0; }
.cart-total { display: flex; justify-content: space-between; font-weight: 700; font-size: var(--text-base); padding-top: var(--space-sm); margin-top: var(--space-sm); border-top: 2px solid var(--line); color: var(--ink); }
.cart-order-btn { width: 100%; margin-top: var(--space-sm); }
</style>
