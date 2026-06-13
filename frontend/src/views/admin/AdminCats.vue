<template>
  <div class="page-enter-active">
    <div class="admin-toolbar">
      <h2>猫咪健康档案</h2>
      <div class="toolbar-right">
        <el-select v-model="filterStatus" placeholder="健康状态" clearable class="status-filter-select" @change="loadCats">
          <el-option label="全部" value="" /><el-option label="健康" value="healthy" /><el-option label="观察中" value="observe" /><el-option label="生病" value="sick" /><el-option label="休息" value="resting" />
        </el-select>
        <el-button v-if="canWrite" type="primary" @click="openCatDialog(null)"><Plus :size="14" class="btn-icon" />新增猫咪</el-button>
      </div>
    </div>

    <!-- 疫苗提醒 -->
    <div v-if="vaccineAlerts.length" class="vaccine-alerts">
      <div v-for="cat in vaccineAlerts" :key="cat.id" class="va-item">
        <AlertTriangle :size="14" style="color:#F59E0B" />
        <span><strong>{{ cat.name }}</strong> 距上次疫苗已超 30 天，建议安排接种</span>
      </div>
    </div>

    <div class="cat-grid">
      <div v-for="cat in filteredCats" :key="cat.id" class="cat-card" :class="{ selected: selectedCat?.id === cat.id }" @click="selectCat(cat)">
        <div class="cc-photo-wrap">
          <img v-if="cat.photo_url" :src="cat.photo_url" :alt="cat.name" class="cat-photo" />
          <div v-else class="cat-avatar"><Cat :size="24" /></div>
          <span class="cc-status" :class="'ccs-' + cat.health_status">{{ healthStatusLabel(cat.health_status) }}</span>
        </div>
        <h4>{{ cat.name }}</h4>
        <p>{{ cat.breed }}</p>
        <div class="tags"><el-tag v-for="t in cat.personality_tags" :key="t" size="small" effect="plain">{{ tagLabel(t) }}</el-tag></div>
        <div v-if="canWrite" class="cc-actions">
          <el-button size="small" text @click.stop="openCatDialog(cat)"><Edit :size="12" /></el-button>
          <el-button size="small" text type="danger" @click.stop="deleteCat(cat)"><Trash2 :size="12" /></el-button>
        </div>
      </div>
    </div>

    <!-- 详情面板 -->
    <div v-if="selectedCat" class="detail-panel">
      <div class="dp-header">
        <h3>{{ selectedCat.name }} - 健康记录</h3>
        <div v-if="canWrite" style="display:flex;align-items:center;gap:var(--space-sm)">
          <PhotoUpload :model-value="selectedCat.photo_url || ''" @update:model-value="updateCatPhoto(selectedCat!.id, $event)" placeholder="猫咪照片" />
        </div>
      </div>

      <div v-if="records.length >= 2" class="chart-section">
        <h4>体重趋势</h4>
        <v-chart :option="weightChartOption" autoresize style="height:200px" />
      </div>

      <div class="records">
        <div v-for="rec in records" :key="rec.id" class="record-item">
          <span class="record-time">{{ rec.recorded_at?.slice(0, 16) || '-' }}</span>
          <span>体重: {{ rec.weight_kg }}kg</span>
          <span v-if="rec.vaccine_note" class="rec-tag vaccine">💉 {{ rec.vaccine_note }}</span>
          <span v-if="rec.interaction_note" class="rec-tag interact">🐾 {{ rec.interaction_note }}</span>
        </div>
        <el-empty v-if="!records.length" description="暂无记录" :image-size="40" />
      </div>

      <el-form v-if="canWrite" inline style="margin-top:var(--space-base)" @submit.prevent="addRecord">
        <el-form-item><el-input-number v-model="healthForm.weightKg" :min="1" :step="0.1" placeholder="体重" /></el-form-item>
        <el-form-item><el-input v-model="healthForm.vaccineNote" placeholder="疫苗备注" /></el-form-item>
        <el-form-item><el-input v-model="healthForm.interactionNote" placeholder="互动备注" /></el-form-item>
        <el-form-item><el-button type="primary" @click="addRecord">添加记录</el-button></el-form-item>
      </el-form>
    </div>

    <!-- 新增/编辑猫咪弹窗 -->
    <el-dialog v-model="catDialog" :title="catForm.id ? '编辑猫咪' : '新增猫咪'" width="440px">
      <el-form :model="catForm" label-width="80px">
        <el-form-item label="名字"><el-input v-model="catForm.name" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="catForm.breed" /></el-form-item>
        <el-form-item label="健康状态">
          <el-select v-model="catForm.health_status" style="width:100%">
            <el-option label="健康" value="healthy" /><el-option label="观察中" value="observe" /><el-option label="生病" value="sick" /><el-option label="休息中" value="resting" />
          </el-select>
        </el-form-item>
        <el-form-item label="性格标签"><el-input v-model="catForm.tagsStr" placeholder="逗号分隔，如 quiet,gentle" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialog = false">取消</el-button>
        <el-button type="primary" :loading="catSaving" @click="saveCat">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cat, Plus, Edit, Trash2, AlertTriangle } from '@lucide/vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import PhotoUpload from '@/components/PhotoUpload.vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { tagLabel } from '@/utils/format'
import { fallbackCats } from '@/utils/fallback'
import type { Cat as CatType, CatHealthRecord } from '@/types'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const auth = useAuthStore()
const canWrite = ['cat_keeper', 'manager', 'admin'].includes(auth.user?.role || '')

const cats = ref<CatType[]>([])
const selectedCat = ref<CatType | null>(null)
const records = ref<CatHealthRecord[]>([])
const filterStatus = ref('')
const healthForm = reactive({ weightKg: 4.5, vaccineNote: '', interactionNote: '' })

const filteredCats = computed(() => {
  if (!filterStatus.value) return cats.value
  return cats.value.filter(c => c.health_status === filterStatus.value)
})

const vaccineAlerts = computed(() => {
  const now = Date.now()
  return cats.value.filter(c => {
    if (!c.last_vaccine_at) return true
    return now - new Date(c.last_vaccine_at).getTime() > 30 * 24 * 60 * 60 * 1000
  })
})

function healthStatusLabel(s: string): string {
  return { healthy: '健康', observe: '观察中', sick: '生病', resting: '休息中' }[s] || s
}

const weightChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 10, bottom: 30 },
  xAxis: { type: 'category', data: records.value.map(r => r.recorded_at?.slice(5, 10) || '-').reverse() },
  yAxis: { type: 'value', name: 'kg', min: (v: { min: number }) => Math.floor(v.min - 0.5), max: (v: { max: number }) => Math.ceil(v.max + 0.5) },
  series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: records.value.map(r => Number(r.weight_kg)).reverse(), itemStyle: { color: '#0f766e' } }],
}))

async function selectCat(cat: CatType) {
  selectedCat.value = cat
  try { records.value = await api.get<CatHealthRecord[]>('/cat-health-records', { catId: cat.id }) }
  catch { records.value = [] }
}

async function addRecord() {
  if (!selectedCat.value) return
  try {
    await api.post('/cat-health-records', { catId: selectedCat.value.id, ...healthForm })
    ElMessage.success('记录已添加')
    await selectCat(selectedCat.value)
    healthForm.vaccineNote = ''; healthForm.interactionNote = ''
  } catch (e) { ElMessage.error((e as Error).message) }
}

async function updateCatPhoto(catId: number, photoUrl: string) {
  try {
    await api.patch(`/cats/${catId}/photo`, { photoUrl })
    const cat = cats.value.find(c => c.id === catId)
    if (cat) cat.photo_url = photoUrl
    if (selectedCat.value?.id === catId) selectedCat.value.photo_url = photoUrl
    ElMessage.success('照片已更新')
  } catch (e) { ElMessage.error((e as Error).message) }
}

// ── 猫咪 CRUD ──
const catDialog = ref(false)
const catSaving = ref(false)
const catForm = reactive({ id: 0, name: '', breed: '', health_status: 'healthy', tagsStr: '' })

function openCatDialog(cat: CatType | null) {
  if (cat) { Object.assign(catForm, { id: cat.id, name: cat.name, breed: cat.breed, health_status: cat.health_status, tagsStr: (cat.personality_tags || []).join(',') }) }
  else { Object.assign(catForm, { id: 0, name: '', breed: '', health_status: 'healthy', tagsStr: '' }) }
  catDialog.value = true
}

async function saveCat() {
  catSaving.value = true
  const payload = { ...catForm, personality_tags: catForm.tagsStr.split(',').map(s => s.trim()).filter(Boolean) }
  try {
    if (catForm.id) { await api.patch(`/cats/${catForm.id}`, payload); ElMessage.success('猫咪已更新') }
    else { await api.post('/cats', payload); ElMessage.success('猫咪已创建') }
    catDialog.value = false; await loadCats()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { catSaving.value = false }
}

async function deleteCat(cat: CatType) {
  await ElMessageBox.confirm(`确认删除猫咪 ${cat.name}？`, '删除猫咪', { type: 'warning' })
  try { await api.remove(`/cats/${cat.id}`); ElMessage.success('已删除'); if (selectedCat.value?.id === cat.id) selectedCat.value = null; await loadCats() }
  catch (e) { ElMessage.error((e as Error).message) }
}

async function loadCats() {
  const managerStoreId = auth.user?.store_id || auth.user?.storeId || 1
  try { cats.value = await api.get<CatType[]>('/cats', { storeId: managerStoreId }) }
  catch { cats.value = fallbackCats }
}

onMounted(loadCats)
</script>

<style scoped>
.admin-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.admin-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; gap: var(--space-sm); align-items: center; }
.status-filter-select { width: 120px; }
.btn-icon { margin-right: 4px; }

.vaccine-alerts { margin-bottom: var(--space-base); padding: var(--space-sm) var(--space-base); background: #fef3c7; border: 1px solid #fde68a; border-radius: var(--radius-md); display: flex; flex-direction: column; gap: var(--space-xs); }
.va-item { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-sm); }

.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: var(--space-base); margin-bottom: var(--space-lg); }
.cat-card { background: var(--paper); padding: var(--space-base); border-radius: var(--radius-lg); border: 2px solid var(--line); text-align: center; cursor: pointer; transition: all 0.15s; position: relative; }
.cat-card:hover { border-color: var(--teal-light); box-shadow: var(--shadow-sm); }
.cat-card.selected { border-color: var(--teal); }
.cc-photo-wrap { position: relative; margin-bottom: var(--space-xs); }
.cat-photo { width: 100%; height: 80px; object-fit: cover; border-radius: var(--radius-md); }
.cat-avatar { color: var(--teal); margin-bottom: var(--space-xs); }
.cc-status { position: absolute; top: var(--space-xs); right: var(--space-xs); padding: 1px 6px; border-radius: var(--radius-sm); font-size: 10px; font-weight: 700; color: #fff; }
.ccs-healthy { background: #16a34a; }
.ccs-observe { background: #3b82f6; }
.ccs-sick { background: #ef4444; }
.ccs-resting { background: #f59e0b; }
.cat-card h4 { margin-bottom: var(--space-xs); }
.cat-card p { font-size: 12px; color: var(--muted); margin-bottom: var(--space-xs); }
.tags { display: flex; gap: var(--space-xs); justify-content: center; flex-wrap: wrap; }
.cc-actions { display: flex; gap: 2px; justify-content: center; margin-top: var(--space-xs); }

.detail-panel { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.dp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-base); }
.dp-header h3 { margin: 0; }
.chart-section { margin-bottom: var(--space-lg); }
.chart-section h4 { font-size: 14px; color: var(--muted); margin-bottom: var(--space-sm); }
.records { display: flex; flex-direction: column; gap: var(--space-sm); }
.record-item { display: flex; gap: var(--space-base); padding: var(--space-sm); background: var(--wash); border-radius: var(--radius-md); font-size: 13px; align-items: center; flex-wrap: wrap; }
.record-time { color: var(--muted); font-size: var(--text-xs); min-width: 120px; }
.rec-tag { font-size: 11px; padding: 1px 6px; border-radius: var(--radius-sm); }
.rec-tag.vaccine { background: #dbeafe; color: #1e40af; }
.rec-tag.interact { background: #dcfce7; color: #166534; }

@media (max-width: 640px) {
  .cat-grid { grid-template-columns: repeat(2, 1fr); gap: var(--space-sm); }
  .detail-panel { padding: var(--space-base); }
  .record-item { flex-direction: column; gap: var(--space-xs); }
}
</style>
