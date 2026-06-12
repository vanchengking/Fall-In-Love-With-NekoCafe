<template>
  <div>
    <h2 style="margin-bottom: 6px">预约桌位</h2>
    <p style="color: #667085; margin-bottom: 20px">选择门店与日期时段 → 在店内布局中点选桌位 → 填写信息 → 提交预约</p>

    <!-- 门店选择（支持从门店详情"预约此门店"带入） -->
    <div class="store-bar">
      <span class="pref-label">预约门店：</span>
      <el-select v-model="form.storeId" filterable style="width: 260px" @change="onStoreChange">
        <el-option v-for="store in stores" :key="store.id" :label="`${store.city} · ${store.name}`" :value="store.id" />
      </el-select>
      <span v-if="currentStore" class="store-hint">{{ currentStore.address }} · {{ businessHours(currentStore) }}</span>
    </div>

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
              <span class="legend-item"><span class="dot dot-free"></span>可选</span>
              <span class="legend-item"><span class="dot dot-occupied"></span>已占用</span>
              <span class="legend-item"><span class="dot dot-disabled"></span>停用</span>
              <span class="legend-item"><span class="dot dot-selected"></span>已选</span>
              <span class="legend-item"><span class="dot dot-cat"></span>猫区</span>
            </div>
          </div>

          <div class="floor-map">
            <!-- 按区域分组渲染：靠窗区在前，其余区域（主厅/派对区/安静区）依次展示 -->
            <div v-for="zone in zonedTables" :key="zone.area"
              class="zone" :class="zone.area === 'window' ? 'zone-window' : 'zone-main'">
              <div class="zone-label">{{ areaLabel(zone.area) }}</div>
              <div class="zone-tables">
                <div v-for="table in zone.tables" :key="table.id"
                  class="table-seat" :class="tableClass(table)"
                  @click="selectTable(table)">
                  <div class="seat-icon">
                    <component :is="table.cat_zone ? Cat : Armchair" :size="20" />
                  </div>
                  <div class="seat-code">{{ table.code }}</div>
                  <div class="seat-capacity">{{ table.seats }}人</div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="form.tableId" class="selected-table-info">
            已选桌位：<strong>{{ selectedTableObj?.code }}</strong>
            （{{ selectedTableObj?.seats }}人 · {{ areaLabel(selectedTableObj?.area) }}）
            <el-button text type="primary" size="small" @click="form.tableId = ''">取消选择</el-button>
          </div>
        </div>

        <!-- 预约信息表单 -->
        <div class="form-panel">
          <h3 style="margin-bottom: 14px">预约信息</h3>
          <el-form :model="form" label-position="top" @submit.prevent="createReservation">
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
          <img v-if="recommendations.cat.photo_url" :src="recommendations.cat.photo_url" class="rec-cat-photo" />
          <div v-else class="rec-cat-avatar"><Cat :size="28" /></div>
          <div>
            <div class="rec-cat-name">{{ recommendations.cat.name }}</div>
            <div class="rec-cat-desc">{{ recommendations.cat.breed }} · {{ recommendations.cat.personality_tags?.join(' / ') }}</div>
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
              <div class="rec-table-info">{{ table.seats }}人 · {{ areaLabel(table.area) }}</div>
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
              <img v-if="item.photo_url" :src="item.photo_url" class="menu-photo" />
              <div class="menu-info">
                <strong>{{ item.name }}</strong>
                <span class="menu-meta">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
              </div>
              <div class="menu-right">
                <!-- 有折扣时显示原价（划线）和折扣价 -->
                <div v-if="memberDiscount < 1.0" class="price-column">
                  <span class="original-price">{{ cents(item.price_cents) }}</span>
                  <span class="discount-badge" v-if="memberDiscount === 0.85">8.5折</span>
                  <span class="discount-badge" v-else-if="memberDiscount === 0.9">9折</span>
                  <span class="discount-badge" v-else-if="memberDiscount === 0.95">9.5折</span>
                  <span class="discounted-price">{{ cents(getDiscountedPrice(item.price_cents)) }}</span>
                </div>
                <!-- 无折扣时只显示原价 -->
                <div v-else class="price-column">
                  <span class="menu-price">{{ cents(item.price_cents) }}</span>
                </div>
                <el-input-number
                  :model-value="cart.selectedMenu[item.id] || 0"
                  @update:model-value="cart.setQuantity(item.id, $event)"
                  :min="0" :max="9" size="small" controls-position="right" />
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
          <!-- 原价合计 -->
          <div class="cart-row original">
            <span>原价合计</span>
            <span class="original-price">{{ cents(cartOriginalTotal) }}</span>
          </div>
          <!-- 折扣优惠（仅会员有折扣时显示） -->
          <div v-if="memberDiscount < 1.0" class="cart-row discount">
            <span>会员折扣（{{ discountLabel }}）</span>
            <span class="discount-amount">-&ensp;{{ cents(cartDiscountAmount) }}</span>
          </div>
          <!-- 实付金额 -->
          <div class="cart-total">
            <span>实付金额</span>
            <span class="final-total">{{ cents(cartDiscountedTotal) }}</span>
          </div>
          <el-button type="primary" plain style="width: 100%; margin-top: 10px" @click="handleCreateOrder">
            生成点单 {{ cents(cartDiscountedTotal) }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cat, Armchair } from '@lucide/vue'
import PreferenceChips from '@/components/PreferenceChips.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { tomorrow, cents, areaLabel, businessHours } from '@/utils/format'
import { fallbackStores, fallbackTables, fallbackCats, fallbackMenuItems } from '@/utils/fallback'
import type { Store, DiningTable, Cat as CatType, MenuItem, Reservation, Recommendations as RecType } from '@/types'

const route = useRoute()
const cart = useCartStore()
const auth = useAuthStore()
const loading = ref(false)
const stores = ref<Store[]>([])
const tables = ref<DiningTable[]>([])
const cats = ref<CatType[]>([])
const menuItems = ref<MenuItem[]>([])
const recommendations = ref<RecType>({ cat: null, tables: [], menuItems: [] })
const memberDiscount = ref<number>(1.0)  // 会员折扣率
const preferenceOptions = ['quiet', 'window', 'sweet', 'coffee', 'photo', 'healthy']

const form = reactive({
  customerName: auth.user?.name || '',
  mobileNumber: auth.user?.mobileNumber || auth.user?.mobile_number || '',
  // 从门店详情"预约此门店"或分享链接进入时带 ?storeId=，否则默认 1 号店
  storeId: Number(route.query.storeId) || 1,
  reservationDate: tomorrow(), reservationTime: '18:30',
  partySize: 2, tableId: '' as string | number,
  recommendedCatId: '' as string | number,
  preferences: ['quiet', 'window'] as string[],
  note: '',
})

const currentStore = computed(() => stores.value.find(s => s.id === form.storeId))
const AREA_ORDER = ['window', 'main', 'party', 'quiet']
/** 店内布局按区域分组：靠窗区在前，其余按 main/party/quiet 顺序 */
const zonedTables = computed(() => {
  const groups: { area: string; tables: DiningTable[] }[] = []
  for (const table of tables.value) {
    let group = groups.find(g => g.area === table.area)
    if (!group) {
      group = { area: table.area, tables: [] }
      groups.push(group)
    }
    group.tables.push(table)
  }
  return groups.sort((a, b) => {
    const ia = AREA_ORDER.indexOf(a.area), ib = AREA_ORDER.indexOf(b.area)
    return (ia === -1 ? AREA_ORDER.length : ia) - (ib === -1 ? AREA_ORDER.length : ib)
  })
})
const selectedTableObj = computed(() => tables.value.find(t => t.id === form.tableId))

// 购物车价格计算（含会员折扣）
const cartOriginalTotal = computed(() => cart.orderTotal)  // 原价合计
const cartDiscountedTotal = computed(() => Math.round(cart.orderTotal * memberDiscount.value))  // 折后合计
const cartDiscountAmount = computed(() => cartOriginalTotal.value - cartDiscountedTotal.value)  // 优惠金额
const discountLabel = computed(() => {
  if (memberDiscount.value === 0.85) return '8.5折'
  if (memberDiscount.value === 0.9) return '9折'
  if (memberDiscount.value === 0.95) return '9.5折'
  return ''
})

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

function tableClass(table: DiningTable) {
  const stopped = !!table.status && table.status !== 'available'
  return {
    disabled: stopped,
    occupied: !stopped && table.available_for_slot === false,
    selected: form.tableId === table.id,
    'cat-zone': table.cat_zone,
  }
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
      userId: 1, storeId: form.storeId, preferences: form.preferences.join(','),
      date: form.reservationDate, time: form.reservationTime, partySize: form.partySize,
    })
    recommendations.value = rec
    if (rec.cat) form.recommendedCatId = rec.cat.id
  } catch {
    recommendations.value = { cat: cats.value[0] || null, tables: tables.value.filter(isSelectable).slice(0, 3), menuItems: menuItems.value.slice(0, 3) }
  }
}

async function createReservation() {
  if (!form.customerName || !form.mobileNumber) {
    ElMessage.warning('请填写姓名和手机号')
    return
  }
  loading.value = true
  try {
    // 清理空字段，避免空字符串导致后端反序列化失败
    const payload: any = { ...form }
    if (payload.tableId === '' || payload.tableId === null) delete payload.tableId
    if (payload.recommendedCatId === '' || payload.recommendedCatId === null) delete payload.recommendedCatId
    if (Array.isArray(payload.preferences) && payload.preferences.length === 0) delete payload.preferences

    const created = await api.post<Reservation>('/reservations', payload)
    cart.setReservationId(created.id)
    ElMessage.success(`预约成功！桌位：${created.table_code || '自动分配'}`)
    // 刚预约的桌位在当前时段已被占用，同步刷新布局与推荐（并由 prune 清空该选择）
    await Promise.all([refreshTables(), refreshRecommendations()])
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { loading.value = false }
}

async function handleCreateOrder() {
  try {
    const order = await cart.createOrder()
    ElMessage.success(`订单已生成：${cents((order as { total_cents: number }).total_cents)}`)
  } catch (e) { ElMessage.error((e as Error).message) }
}

// 计算折扣价
function getDiscountedPrice(priceCents: number): number {
  return Math.round(priceCents * memberDiscount.value)
}

onMounted(async () => {
  try { stores.value = await api.get<Store[]>('/stores') }
  catch { stores.value = fallbackStores }
  // URL 带入的门店不存在时回退到第一家，避免选择器显示悬空 ID
  if (stores.value.length > 0 && !stores.value.some(s => s.id === form.storeId)) {
    form.storeId = stores.value[0].id
  }
  await loadStoreData()

  // 获取会员折扣率
  if (auth.isAuthenticated) {
    try {
      const memberInfo = await api.get<{discount: number}>('/users/me/member')
      memberDiscount.value = memberInfo.discount || 1.0
    } catch {
      memberDiscount.value = 1.0
    }
  }

  await refreshRecommendations()
})
</script>

<style scoped>
.store-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.store-hint { font-size: 13px; color: #98a2b3; }
.pref-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; flex-wrap: wrap; }
.pref-label { font-size: 14px; color: #667085; flex-shrink: 0; }
.res-layout { display: grid; grid-template-columns: 1fr 380px; gap: 20px; }
@media (max-width: 900px) { .res-layout { grid-template-columns: 1fr; } }

/* 店内布局 */
.floor-plan-panel { background: #fff; padding: 20px; border-radius: 14px; border: 1px solid #e8e5df; margin-bottom: 16px; }
.floor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.floor-header h3 { margin: 0; }
.legend { display: flex; gap: 12px; font-size: 12px; color: #667085; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot-free { background: #d1fae5; border: 1px solid #6ee7b7; }
.dot-occupied { background: #f3f4f6; border: 1px solid #d1d5db; }
.dot-disabled { background: #fafafa; border: 1px dashed #9ca3af; }
.dot-selected { background: #0f766e; }
.dot-cat { background: #bbf7d0; border: 1px solid #4ade80; }

.floor-map { display: flex; flex-direction: column; gap: 16px; }
.zone { border: 2px dashed #e5e7eb; border-radius: 12px; padding: 14px; position: relative; }
.zone-window { border-color: #93c5fd; background: #f0f9ff; }
.zone-main { border-color: #d1d5db; background: #fafafa; }
.zone-label { position: absolute; top: -10px; left: 14px; background: #fff; padding: 0 8px; font-size: 12px; color: #667085; font-weight: 600; }
.zone-tables { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 4px; }

.table-seat { width: 90px; padding: 12px 8px; border: 2px solid #d1fae5; border-radius: 12px; text-align: center; cursor: pointer; transition: all 0.15s; background: #fff; }
.table-seat:hover:not(.occupied):not(.disabled) { border-color: #0f766e; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(15,118,110,0.15); }
.table-seat.selected { border-color: #0f766e; background: #e8f6f1; box-shadow: 0 0 0 3px rgba(15,118,110,0.2); }
.table-seat.occupied { background: #f3f4f6; border-color: #e5e7eb; cursor: not-allowed; opacity: 0.55; }
.table-seat.cat-zone { border-color: #86efac; background: #f0fdf4; }
.table-seat.disabled { background: #fafafa; border-color: #d1d5db; border-style: dashed; cursor: not-allowed; opacity: 0.45; }
.seat-icon { color: #0f766e; margin-bottom: 4px; }
.table-seat.occupied .seat-icon, .table-seat.disabled .seat-icon { color: #9ca3af; }
.seat-code { font-size: 16px; font-weight: 700; color: #172033; }
.seat-capacity { font-size: 11px; color: #667085; }
.selected-table-info { margin-top: 12px; font-size: 13px; color: #0f766e; display: flex; align-items: center; gap: 8px; }

/* 预约表单 */
.form-panel { background: #fff; padding: 20px; border-radius: 14px; border: 1px solid #e8e5df; }

/* 右侧推荐 */
.right-col { display: flex; flex-direction: column; gap: 14px; }
.rec-cat-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: #e8f6f1; border-radius: 14px; cursor: pointer; border: 2px solid transparent; transition: border-color 0.15s; }
.rec-cat-card:hover { border-color: #0f766e; }
.rec-cat-photo { width: 56px; height: 56px; object-fit: cover; border-radius: 10px; }
.rec-cat-avatar { color: #0f766e; }
.rec-cat-name { font-size: 16px; font-weight: 700; color: #172033; }
.rec-cat-desc { font-size: 13px; color: #667085; margin-top: 2px; }

.rec-section { background: #fff; padding: 16px; border-radius: 14px; border: 1px solid #e8e5df; }
.rec-section h4 { margin: 0 0 12px; font-size: 14px; color: #172033; }
.rec-table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 10px; }
.rec-table-card { padding: 12px; border: 2px solid #e8e5df; border-radius: 10px; text-align: center; cursor: pointer; transition: all 0.15s; }
.rec-table-card:hover { border-color: #0f766e; }
.rec-table-card.selected { border-color: #0f766e; background: #e8f6f1; }
.rec-table-card.unavailable { opacity: 0.5; cursor: not-allowed; }
.rec-table-card.unavailable:hover { border-color: #e8e5df; }
.rec-table-code { font-size: 18px; font-weight: 700; }
.rec-table-info { font-size: 12px; color: #667085; margin-top: 2px; }

.menu-list { display: flex; flex-direction: column; gap: 8px; }
.menu-card { display: flex; justify-content: space-between; align-items: center; padding: 12px; background: #f8f9fa; border-radius: 10px; gap: 12px; }
.menu-photo { width: 48px; height: 48px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.menu-info strong { font-size: 14px; color: #172033; }
.menu-meta { display: block; font-size: 12px; color: #667085; margin-top: 2px; }
.menu-right { display: flex; align-items: flex-start; gap: 8px; }
.price-column { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.menu-price { font-weight: 700; color: #e86f51; font-size: 14px; }
.original-price { font-size: 12px; color: #999; text-decoration: line-through; }
.discount-badge { font-size: 11px; background: #e6f7ff; color: #1890ff; padding: 1px 6px; border-radius: 4px; font-weight: 600; display: inline-block; }
.discounted-price { font-weight: 700; color: #e86f51; font-size: 14px; }

.cart-summary { background: #fff; padding: 16px; border-radius: 14px; border: 2px solid #0f766e; }
.cart-row { display: flex; justify-content: space-between; font-size: 13px; padding: 4px 0; }
.cart-row.original { color: #999; }
.cart-row.original .original-price { text-decoration: line-through; font-size: 13px; }
.cart-row.discount { color: #52c41a; }
.discount-amount { color: #52c41a; font-weight: 600; }
.cart-total { display: flex; justify-content: space-between; font-weight: 700; font-size: 16px; padding-top: 10px; margin-top: 8px; border-top: 2px solid #e8e5df; color: #172033; }
.final-total { color: #e86f51; font-size: 18px; }
</style>
