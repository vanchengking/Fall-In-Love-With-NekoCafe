<template>
  <div class="stores-page">
    <div class="page-header">
      <div>
        <h2>门店管理</h2>
        <p>总部运营维护门店资料和桌位配置。</p>
      </div>
      <el-button type="primary" @click="openStoreDialog()">新建门店</el-button>
    </div>

    <div class="store-list">
      <section v-for="store in stores" :key="store.id" class="store-card">
        <div class="store-head">
          <div>
            <h3>{{ store.name }}</h3>
            <p>{{ store.city }} · {{ store.address }}</p>
          </div>
          <div class="actions">
            <el-button size="small" @click="openStoreDialog(store)">编辑门店</el-button>
            <el-button size="small" type="danger" plain @click="removeStore(store)">删除门店</el-button>
          </div>
        </div>

        <div class="store-meta">
          <span>电话：{{ store.phone }}</span>
          <span>营业时间：{{ store.open_time || '-' }} - {{ store.close_time || '-' }}</span>
          <span>桌位：{{ store.table_count || 0 }}</span>
          <span>座位：{{ store.total_seats || 0 }}</span>
        </div>

        <div class="store-extra">
          <p><strong>营业描述：</strong>{{ store.business_hours_text || '未填写' }}</p>
          <p><strong>设备说明：</strong>{{ store.equipment_desc || '未填写' }}</p>
          <p><strong>区域说明：</strong>{{ store.area_detail || '未填写' }}</p>
          <p><strong>门店图片：</strong>{{ store.photo_url || '未填写' }}</p>
        </div>

        <div class="table-panel">
          <div class="table-panel-head">
            <h4>桌位配置</h4>
            <el-button size="small" type="primary" plain @click="openTableDialog(store.id)">
              新建桌位
            </el-button>
          </div>

          <el-table :data="tablesByStore(store.id)" stripe style="width: 100%">
            <el-table-column prop="code" label="桌号" width="100" />
            <el-table-column prop="seats" label="座位数" width="90" />
            <el-table-column prop="area" label="区域" width="120" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="猫区" width="90">
              <template #default="{ row }">
                <el-tag :type="row.cat_zone ? 'success' : 'info'" size="small">
                  {{ row.cat_zone ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="device_note" label="设备说明" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openTableDialog(store.id, row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="removeTable(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="storeDialogVisible"
      :title="storeForm.id ? '编辑门店' : '新建门店'"
      width="720px"
    >
      <el-form label-width="92px" class="dialog-form">
        <el-form-item label="门店名称">
          <el-input v-model="storeForm.name" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="storeForm.city" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="storeForm.address" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="storeForm.phone" />
        </el-form-item>
        <el-form-item label="营业时间">
          <div class="time-row">
            <el-input v-model="storeForm.openTime" placeholder="10:30" />
            <span>至</span>
            <el-input v-model="storeForm.closeTime" placeholder="22:30" />
          </div>
        </el-form-item>
        <el-form-item label="营业描述">
          <el-input v-model="storeForm.businessHoursText" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="设备说明">
          <el-input v-model="storeForm.equipmentDesc" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="区域说明">
          <el-input v-model="storeForm.areaDetail" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="门店图片">
          <el-input v-model="storeForm.photoUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="storeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStore">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="tableDialogVisible"
      :title="tableForm.id ? '编辑桌位' : '新建桌位'"
      width="680px"
    >
      <el-form label-width="92px" class="dialog-form">
        <el-form-item label="桌号">
          <el-input v-model="tableForm.code" />
        </el-form-item>
        <el-form-item label="座位数">
          <el-input-number v-model="tableForm.seats" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="tableForm.area" placeholder="main / window / party" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="tableForm.status" style="width: 100%">
            <el-option label="available" value="available" />
            <el-option label="reserved" value="reserved" />
            <el-option label="maintenance" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="猫区">
          <el-switch v-model="tableForm.catZone" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="tableForm.photoUrl" />
        </el-form-item>
        <el-form-item label="区域说明">
          <el-input v-model="tableForm.areaDetail" />
        </el-form-item>
        <el-form-item label="设备说明">
          <el-input v-model="tableForm.deviceNote" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTable">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/utils/http'

interface StoreRow {
  id: number
  name: string
  city: string
  address: string
  phone: string
  open_time?: string
  close_time?: string
  table_count?: number
  total_seats?: number
  business_hours_text?: string
  photo_url?: string
  equipment_desc?: string
  area_detail?: string
}

interface TableRow {
  id: number
  store_id: number
  code: string
  seats: number
  area: string
  cat_zone: boolean
  status: string
  photo_url?: string
  area_detail?: string
  device_note?: string
}

interface StoreForm {
  id?: number
  name: string
  city: string
  address: string
  phone: string
  openTime: string
  closeTime: string
  businessHoursText: string
  photoUrl: string
  equipmentDesc: string
  areaDetail: string
}

interface TableForm {
  id?: number
  storeId: number
  code: string
  seats: number
  area: string
  catZone: boolean
  status: string
  photoUrl: string
  areaDetail: string
  deviceNote: string
}

const stores = ref<StoreRow[]>([])
const tables = ref<TableRow[]>([])
const storeDialogVisible = ref(false)
const tableDialogVisible = ref(false)

const storeForm = reactive<StoreForm>({
  name: '',
  city: '',
  address: '',
  phone: '',
  openTime: '10:30',
  closeTime: '22:30',
  businessHoursText: '',
  photoUrl: '',
  equipmentDesc: '',
  areaDetail: '',
})

const tableForm = reactive<TableForm>({
  storeId: 0,
  code: '',
  seats: 2,
  area: 'main',
  catZone: false,
  status: 'available',
  photoUrl: '',
  areaDetail: '',
  deviceNote: '',
})

const tablesByStore = computed(() => (storeId: number) =>
  tables.value.filter((table) => table.store_id === storeId))

function resetStoreForm() {
  storeForm.id = undefined
  storeForm.name = ''
  storeForm.city = ''
  storeForm.address = ''
  storeForm.phone = ''
  storeForm.openTime = '10:30'
  storeForm.closeTime = '22:30'
  storeForm.businessHoursText = ''
  storeForm.photoUrl = ''
  storeForm.equipmentDesc = ''
  storeForm.areaDetail = ''
}

function resetTableForm(storeId = 0) {
  tableForm.id = undefined
  tableForm.storeId = storeId
  tableForm.code = ''
  tableForm.seats = 2
  tableForm.area = 'main'
  tableForm.catZone = false
  tableForm.status = 'available'
  tableForm.photoUrl = ''
  tableForm.areaDetail = ''
  tableForm.deviceNote = ''
}

function openStoreDialog(store?: StoreRow) {
  resetStoreForm()
  if (store) {
    storeForm.id = store.id
    storeForm.name = store.name
    storeForm.city = store.city
    storeForm.address = store.address
    storeForm.phone = store.phone
    storeForm.openTime = store.open_time || '10:30'
    storeForm.closeTime = store.close_time || '22:30'
    storeForm.businessHoursText = store.business_hours_text || ''
    storeForm.photoUrl = store.photo_url || ''
    storeForm.equipmentDesc = store.equipment_desc || ''
    storeForm.areaDetail = store.area_detail || ''
  }
  storeDialogVisible.value = true
}

function openTableDialog(storeId: number, table?: TableRow) {
  resetTableForm(storeId)
  if (table) {
    tableForm.id = table.id
    tableForm.storeId = table.store_id
    tableForm.code = table.code
    tableForm.seats = table.seats
    tableForm.area = table.area
    tableForm.catZone = table.cat_zone
    tableForm.status = table.status
    tableForm.photoUrl = table.photo_url || ''
    tableForm.areaDetail = table.area_detail || ''
    tableForm.deviceNote = table.device_note || ''
  }
  tableDialogVisible.value = true
}

async function loadStores() {
  stores.value = await http.get('/admin/stores')
}

async function loadTables() {
  tables.value = await http.get('/admin/tables')
}

async function reloadAll() {
  await Promise.all([loadStores(), loadTables()])
}

async function submitStore() {
  const payload = {
    name: storeForm.name,
    city: storeForm.city,
    address: storeForm.address,
    phone: storeForm.phone,
    openTime: storeForm.openTime,
    closeTime: storeForm.closeTime,
    businessHoursText: storeForm.businessHoursText,
    photoUrl: storeForm.photoUrl,
    equipmentDesc: storeForm.equipmentDesc,
    areaDetail: storeForm.areaDetail,
  }

  if (storeForm.id) {
    await http.put(`/admin/stores/${storeForm.id}`, payload)
    ElMessage.success('门店信息已更新')
  } else {
    await http.post('/admin/stores', payload)
    ElMessage.success('门店已创建')
  }

  storeDialogVisible.value = false
  await loadStores()
}

async function submitTable() {
  const payload = {
    storeId: tableForm.storeId,
    code: tableForm.code,
    seats: tableForm.seats,
    area: tableForm.area,
    catZone: tableForm.catZone,
    status: tableForm.status,
    photoUrl: tableForm.photoUrl,
    areaDetail: tableForm.areaDetail,
    deviceNote: tableForm.deviceNote,
  }

  if (tableForm.id) {
    await http.put(`/admin/tables/${tableForm.id}`, payload)
    ElMessage.success('桌位信息已更新')
  } else {
    await http.post('/admin/tables', payload)
    ElMessage.success('桌位已创建')
  }

  tableDialogVisible.value = false
  await reloadAll()
}

async function removeStore(store: StoreRow) {
  await ElMessageBox.confirm(`确定删除门店“${store.name}”吗？`, '删除确认', {
    type: 'warning',
  })
  await http.delete(`/admin/stores/${store.id}`)
  ElMessage.success('门店已删除')
  await reloadAll()
}

async function removeTable(table: TableRow) {
  await ElMessageBox.confirm(`确定删除桌位“${table.code}”吗？`, '删除确认', {
    type: 'warning',
  })
  await http.delete(`/admin/tables/${table.id}`)
  ElMessage.success('桌位已删除')
  await reloadAll()
}

onMounted(async () => {
  await reloadAll()
})
</script>

<style scoped>
.stores-page {
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
  color: #172033;
}

.page-header p {
  margin: 6px 0 0;
  color: #667085;
}

.store-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.store-card {
  background: #fff;
  border: 1px solid #e8e5df;
  border-radius: 16px;
  padding: 22px;
}

.store-head,
.table-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.store-head h3,
.table-panel-head h4 {
  margin: 0;
  color: #172033;
}

.store-head p {
  margin: 6px 0 0;
  color: #667085;
}

.actions {
  display: flex;
  gap: 8px;
}

.store-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin: 16px 0 12px;
  color: #475569;
  font-size: 14px;
}

.store-extra {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.store-extra p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.table-panel {
  border-top: 1px solid #ece8df;
  padding-top: 18px;
}

.table-panel-head {
  margin-bottom: 14px;
}

.dialog-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.time-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  width: 100%;
}

@media (max-width: 900px) {
  .page-header,
  .store-head,
  .table-panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    justify-content: flex-start;
  }

  .dialog-form {
    grid-template-columns: 1fr;
  }
}
</style>
