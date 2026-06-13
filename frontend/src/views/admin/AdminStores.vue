<template>
  <div class="page-enter-active">
    <div class="admin-toolbar">
      <h2>门店管理</h2>
      <el-button v-if="canEditStore" type="primary" @click="openStoreDialog(null)"><Plus :size="14" class="btn-icon" />新增门店</el-button>
    </div>

    <div v-for="store in stores" :key="store.id" class="store-section">
      <div class="store-header">
        <div>
          <h3>{{ store.name }}</h3>
          <p>{{ store.city }} · {{ store.address }} · {{ store.phone }}</p>
          <p>{{ store.table_count }} 桌 · {{ store.total_seats }} 座</p>
        </div>
        <div class="store-actions">
          <el-tag v-if="!canEditStore" type="info" size="small" effect="plain">只读</el-tag>
          <el-button v-if="canEditStore" size="small" @click="openStoreDialog(store)"><Edit :size="14" /></el-button>
          <el-button v-if="canEditStore" size="small" type="danger" plain @click="deleteStore(store)"><Trash2 :size="14" /></el-button>
        </div>
      </div>

      <!-- 桌位 -->
      <div class="section-head">
        <h4>桌位列表</h4>
        <el-button size="small" text type="primary" @click="openTableDialog(store.id)"><Plus :size="14" />添加</el-button>
      </div>
      <div class="table-grid">
        <div v-for="table in getTables(store.id)" :key="table.id" class="table-card">
          <strong>{{ table.code }}</strong>
          <span>{{ table.seats }}人 · {{ tagLabel(table.area) }}</span>
          <el-tag v-if="table.cat_zone" size="small" type="success" effect="plain">猫区</el-tag>
          <div class="tc-actions">
            <el-button size="small" text @click="openTableDialog(store.id, table)"><Edit :size="12" /></el-button>
            <el-button size="small" text type="danger" @click="deleteTable(table)"><Trash2 :size="12" /></el-button>
          </div>
        </div>
      </div>

      <!-- 菜品 -->
      <div class="section-head">
        <h4>菜品管理</h4>
        <el-button size="small" text type="primary" @click="openMenuDialog(store.id)"><Plus :size="14" />添加</el-button>
      </div>
      <div class="menu-grid">
        <div v-for="item in getMenus(store.id)" :key="item.id" class="menu-card">
          <div class="menu-photo-area">
            <PhotoUpload :model-value="item.photo_url || ''" @update:model-value="updateMenuPhoto(item.id, $event)" placeholder="菜品图片" />
          </div>
          <div class="menu-detail">
            <strong>{{ item.name }}</strong>
            <span class="menu-meta">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
            <span class="menu-price">{{ cents(item.price_cents) }}</span>
          </div>
          <div class="mc-actions">
            <el-button size="small" text @click="openMenuDialog(store.id, item)"><Edit :size="12" /></el-button>
            <el-button size="small" text type="danger" @click="deleteMenu(item)"><Trash2 :size="12" /></el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 门店弹窗 -->
    <el-dialog v-model="storeDialog" :title="storeForm.id ? '编辑门店' : '新增门店'" width="500px" class="admin-dialog">
      <el-form ref="storeFormRef" :model="storeForm" :rules="storeRules" label-width="80px" label-position="right">
        <el-form-item label="名称"><el-input v-model="storeForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="storeForm.city" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="storeForm.address" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="storeForm.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="storeDialog = false">取消</el-button>
        <el-button type="primary" :loading="storeSaving" @click="saveStore">保存</el-button>
      </template>
    </el-dialog>

    <!-- 桌位弹窗 -->
    <el-dialog v-model="tableDialog" :title="tableForm.id ? '编辑桌位' : '添加桌位'" width="400px" class="admin-dialog">
      <el-form :model="tableForm" label-width="80px" label-position="right">
        <el-form-item label="桌号"><el-input v-model="tableForm.code" placeholder="如 A01" /></el-form-item>
        <el-form-item label="座位数"><el-input-number v-model="tableForm.seats" :min="1" :max="20" /></el-form-item>
        <el-form-item label="区域">
          <el-select v-model="tableForm.area" style="width:100%">
            <el-option label="窗边" value="window" /><el-option label="大厅" value="main" /><el-option label="包厢" value="party" />
          </el-select>
        </el-form-item>
        <el-form-item label="猫区"><el-switch v-model="tableForm.cat_zone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tableDialog = false">取消</el-button>
        <el-button type="primary" :loading="tableSaving" @click="saveTable">保存</el-button>
      </template>
    </el-dialog>

    <!-- 菜品弹窗 -->
    <el-dialog v-model="menuDialog" :title="menuForm.id ? '编辑菜品' : '添加菜品'" width="400px" class="admin-dialog">
      <el-form :model="menuForm" label-width="80px" label-position="right">
        <el-form-item label="名称"><el-input v-model="menuForm.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="menuForm.category" style="width:100%">
            <el-option label="饮品" value="drink" /><el-option label="主食" value="meal" /><el-option label="甜品" value="dessert" /><el-option label="小食" value="snack" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(分)"><el-input-number v-model="menuForm.price_cents" :min="100" :step="100" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="menuForm.tagsStr" placeholder="逗号分隔，如 coffee,signature" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" :loading="menuSaving" @click="saveMenu">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Trash2 } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents, tagLabel } from '@/utils/format'
import { fallbackStores, fallbackTables, fallbackMenuItems } from '@/utils/fallback'
import { useAuthStore } from '@/stores/auth'
import PhotoUpload from '@/components/PhotoUpload.vue'
import type { Store, DiningTable, MenuItem } from '@/types'

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'admin')
const isOperator = computed(() => ['operator', 'admin'].includes(role.value))
const canEditStore = computed(() => isOperator.value) // 只有运营可增删改门店

const allStores = ref<Store[]>([])
const allTables = ref<DiningTable[]>([])
const allMenus = ref<MenuItem[]>([])

// 店长只看到本店，运营看到全部
const stores = computed(() => {
  if (isOperator.value) return allStores.value
  return allStores.value.slice(0, 1) // 店长只显示第一家（实际应按 store_id 过滤）
})

function getTables(storeId: number) { return allTables.value.filter(t => t.store_id === storeId) }
function getMenus(storeId: number) { return allMenus.value.filter(m => m.store_id === storeId) }

// ── 门店 CRUD ──
const storeDialog = ref(false)
const storeSaving = ref(false)
const storeFormRef = ref()
const storeForm = reactive({ id: 0, name: '', city: '', address: '', phone: '' })
const storeRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
}

function openStoreDialog(store: Store | null) {
  if (store) { Object.assign(storeForm, { id: store.id, name: store.name, city: store.city, address: store.address, phone: store.phone }) }
  else { Object.assign(storeForm, { id: 0, name: '', city: '', address: '', phone: '' }) }
  storeDialog.value = true
}

async function saveStore() {
  if (storeFormRef.value) {
    const valid = await storeFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  storeSaving.value = true
  try {
    if (storeForm.id) {
      await api.patch(`/stores/${storeForm.id}`, storeForm)
      ElMessage.success('门店已更新')
    } else {
      await api.post('/stores', storeForm)
      ElMessage.success('门店已创建')
    }
    storeDialog.value = false
    await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { storeSaving.value = false }
}

async function deleteStore(store: Store) {
  await ElMessageBox.confirm(`确认删除门店 ${store.name}？`, '删除门店', { type: 'warning' })
  try { await api.remove(`/stores/${store.id}`); ElMessage.success('已删除'); await loadData() }
  catch (e) { ElMessage.error((e as Error).message) }
}

// ── 桌位 CRUD ──
const tableDialog = ref(false)
const tableSaving = ref(false)
const tableForm = reactive({ id: 0, store_id: 0, code: '', seats: 4, area: 'main', cat_zone: false })

function openTableDialog(storeId: number, table?: DiningTable) {
  if (table) { Object.assign(tableForm, { id: table.id, store_id: table.store_id, code: table.code, seats: table.seats, area: table.area, cat_zone: table.cat_zone }) }
  else { Object.assign(tableForm, { id: 0, store_id: storeId, code: '', seats: 4, area: 'main', cat_zone: false }) }
  tableDialog.value = true
}

async function saveTable() {
  tableSaving.value = true
  try {
    if (tableForm.id) { await api.patch(`/tables/${tableForm.id}`, tableForm); ElMessage.success('桌位已更新') }
    else { await api.post('/tables', tableForm); ElMessage.success('桌位已添加') }
    tableDialog.value = false; await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { tableSaving.value = false }
}

async function deleteTable(table: DiningTable) {
  await ElMessageBox.confirm(`确认删除桌位 ${table.code}？`, '删除桌位', { type: 'warning' })
  try { await api.remove(`/tables/${table.id}`); ElMessage.success('已删除'); await loadData() }
  catch (e) { ElMessage.error((e as Error).message) }
}

// ── 菜品 CRUD ──
const menuDialog = ref(false)
const menuSaving = ref(false)
const menuForm = reactive({ id: 0, store_id: 0, name: '', category: 'drink', price_cents: 3200, tagsStr: '' })

function openMenuDialog(storeId: number, item?: MenuItem) {
  if (item) { Object.assign(menuForm, { id: item.id, store_id: item.store_id || storeId, name: item.name, category: item.category, price_cents: item.price_cents, tagsStr: (item.tags || []).join(',') }) }
  else { Object.assign(menuForm, { id: 0, store_id: storeId, name: '', category: 'drink', price_cents: 3200, tagsStr: '' }) }
  menuDialog.value = true
}

async function saveMenu() {
  menuSaving.value = true
  const payload = { ...menuForm, tags: menuForm.tagsStr.split(',').map(s => s.trim()).filter(Boolean) }
  try {
    if (menuForm.id) { await api.patch(`/menu-items/${menuForm.id}`, payload); ElMessage.success('菜品已更新') }
    else { await api.post('/menu-items', payload); ElMessage.success('菜品已添加') }
    menuDialog.value = false; await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { menuSaving.value = false }
}

async function deleteMenu(item: MenuItem) {
  await ElMessageBox.confirm(`确认删除菜品 ${item.name}？`, '删除菜品', { type: 'warning' })
  try { await api.remove(`/menu-items/${item.id}`); ElMessage.success('已删除'); await loadData() }
  catch (e) { ElMessage.error((e as Error).message) }
}

async function updateMenuPhoto(id: number, photoUrl: string) {
  try {
    await api.patch(`/menu-items/${id}/photo`, { photoUrl })
    const item = allMenus.value.find(m => m.id === id)
    if (item) item.photo_url = photoUrl
    ElMessage.success('图片已更新')
  } catch (e) { ElMessage.error((e as Error).message) }
}

async function loadData() {
  try {
    const [s, t, m] = await Promise.all([api.get<Store[]>('/stores'), api.get<DiningTable[]>('/tables'), api.get<MenuItem[]>('/menu-items')])
    allStores.value = s; allTables.value = t; allMenus.value = m
  } catch { allStores.value = fallbackStores; allTables.value = fallbackTables; allMenus.value = fallbackMenuItems }
}

onMounted(loadData)
</script>

<style scoped>
.admin-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.admin-toolbar h2 { margin: 0; }
.btn-icon { margin-right: 4px; }
.store-section { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); margin-bottom: var(--space-lg); transition: box-shadow var(--transition-fast); }
.store-section:hover { box-shadow: var(--shadow-sm); }
.store-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: var(--space-base); }
.store-header h3 { margin-bottom: var(--space-xs); }
.store-header p { color: var(--muted); font-size: var(--text-sm); }
.store-actions { display: flex; gap: var(--space-xs); }
.section-head { display: flex; justify-content: space-between; align-items: center; margin: var(--space-base) 0 var(--space-sm); }
.section-head h4 { margin: 0; }
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: var(--space-sm); }
.table-card { padding: var(--space-md); background: var(--wash); border-radius: var(--radius-md); text-align: center; display: flex; flex-direction: column; gap: var(--space-xs); position: relative; transition: all var(--transition-fast); }
.table-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.table-card:nth-child(even) { background: var(--paper); border: 1px solid var(--line); }
.tc-actions { display: flex; gap: 2px; justify-content: center; margin-top: var(--space-xs); }
.menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--space-md); }
.menu-card { display: flex; gap: var(--space-md); padding: var(--space-md); background: var(--wash); border-radius: var(--radius-lg); align-items: center; transition: all var(--transition-fast); }
.menu-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.menu-card:nth-child(even) { background: var(--paper); border: 1px solid var(--line); }
.menu-photo-area { flex-shrink: 0; }
.menu-detail { flex: 1; display: flex; flex-direction: column; gap: var(--space-xs); }
.menu-detail strong { font-size: var(--text-base); color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.menu-meta { font-size: var(--text-xs); color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.menu-price { font-size: var(--text-base); font-weight: 700; color: var(--coral); }
.mc-actions { display: flex; flex-direction: column; gap: 2px; }
@media (max-width: 640px) { .store-section { padding: var(--space-base); } .menu-grid { grid-template-columns: 1fr; } .menu-card { flex-direction: column; align-items: flex-start; } }
</style>
