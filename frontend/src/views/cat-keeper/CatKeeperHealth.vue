<template>
  <div class="page-enter-active">
    <div class="ck-toolbar">
      <div class="toolbar-left">
        <el-button text @click="$router.push('/cat-keeper/cats')"><ArrowLeft :size="16" class="btn-icon" />返回列表</el-button>
        <h2>{{ cat?.name || '猫咪详情' }}</h2>
      </div>
    </div>

    <div v-if="loading" v-loading="true" class="loading-placeholder"></div>

    <template v-else-if="cat">
      <!-- 基本信息 -->
      <div class="info-card">
        <div class="info-photo">
          <img v-if="cat.photo_url" :src="cat.photo_url" :alt="cat.name" class="photo" />
          <div v-else class="photo-placeholder"><Cat :size="40" /></div>
          <PhotoUpload :model-value="cat.photo_url || ''" @update:model-value="updatePhoto" placeholder="更换照片" />
        </div>
        <div class="info-detail">
          <div class="info-row"><span class="label">名字</span><el-input v-model="editForm.name" size="small" class="info-input" /></div>
          <div class="info-row"><span class="label">品种</span><el-input v-model="editForm.breed" size="small" class="info-input" /></div>
          <div class="info-row">
            <span class="label">健康状态</span>
            <el-select v-model="editForm.health_status" size="small" class="info-input">
              <el-option label="健康" value="healthy" /><el-option label="观察中" value="observe" />
              <el-option label="生病" value="sick" /><el-option label="休息" value="resting" />
            </el-select>
          </div>
          <div class="info-row">
            <span class="label">性格标签</span>
            <el-input v-model="editForm.tagsStr" size="small" class="info-input" placeholder="逗号分隔" />
          </div>
          <el-button type="primary" size="small" @click="saveInfo" :loading="saving">保存信息</el-button>
        </div>
      </div>

      <!-- 体重趋势图 -->
      <div class="section-card">
        <h3>体重趋势</h3>
        <v-chart v-if="records.length >= 2" :option="weightChartOption" autoresize class="chart-canvas" />
        <el-empty v-else description="至少需要 2 条体重记录才能显示趋势图" :image-size="40" />
      </div>

      <!-- 健康记录 -->
      <div class="section-card">
        <div class="section-head">
          <h3>健康记录</h3>
          <el-button type="primary" size="small" @click="recordDialog = true"><Plus :size="14" class="btn-icon" />新增记录</el-button>
        </div>
        <el-table :data="records" stripe size="small" class="health-table" :row-class-name="vaccineRowClass">
          <el-table-column prop="recorded_at" label="时间" width="170">
            <template #default="{ row }">{{ row.recorded_at?.slice(0, 16) || '-' }}</template>
          </el-table-column>
          <el-table-column prop="weight_kg" label="体重(kg)" width="100" align="center" />
          <el-table-column prop="vaccine_note" label="疫苗记录">
            <template #default="{ row }">
              <span v-if="row.vaccine_note">
                {{ row.vaccine_note }}
                <el-tag v-if="isVaccineExpiringSoon(row)" type="warning" size="small" effect="dark" class="expiring-tag">即将到期</el-tag>
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="interaction_note" label="互动日志">
            <template #default="{ row }">
              <span v-if="row.interaction_note">{{ row.interaction_note }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!records.length" description="暂无健康记录" :image-size="40" />
      </div>

      <!-- 互动日志时间线 -->
      <div class="section-card">
        <h3>互动日志</h3>
        <el-timeline v-if="interactionLogs.length">
          <el-timeline-item
            v-for="log in interactionLogs" :key="log.id"
            :timestamp="log.recorded_at?.slice(0, 16)"
            placement="top"
          >
            {{ log.interaction_note }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无互动日志" :image-size="40" />
      </div>
    </template>

    <el-empty v-else description="未找到猫咪信息" />

    <!-- 新增记录弹窗 -->
    <el-dialog v-model="recordDialog" title="新增健康记录" width="420px" class="ck-dialog">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="80px" label-position="right">
        <el-form-item label="体重(kg)"><el-input-number v-model="recordForm.weight_kg" :min="0" :max="20" :step="0.1" :precision="2" /></el-form-item>
        <el-form-item label="疫苗记录"><el-input v-model="recordForm.vaccine_note" placeholder="如：狂犬疫苗已接种" /></el-form-item>
        <el-form-item label="互动日志"><el-input v-model="recordForm.interaction_note" type="textarea" :rows="3" placeholder="如：今日互动温和，适合安静桌位" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRecord" :loading="savingRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Cat, Plus } from '@lucide/vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { api } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'
import { fallbackCats } from '@/utils/fallback'
import PhotoUpload from '@/components/PhotoUpload.vue'
import type { Cat as CatType, CatHealthRecord } from '@/types'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const route = useRoute()
const auth = useAuthStore()
const catId = Number(route.params.id)

const cat = ref<CatType | null>(null)
const records = ref<CatHealthRecord[]>([])
const loading = ref(true)
const saving = ref(false)
const savingRecord = ref(false)
const recordDialog = ref(false)

const editForm = reactive({ name: '', breed: '', health_status: 'healthy', tagsStr: '' })
const recordForm = reactive({ weight_kg: 0, vaccine_note: '', interaction_note: '' })
const recordFormRef = ref()
const recordRules = {
  weight_kg: [{ required: true, message: '请输入体重', trigger: 'blur' }],
}

const interactionLogs = computed(() =>
  records.value.filter(r => r.interaction_note).sort((a, b) => (b.recorded_at || '').localeCompare(a.recorded_at || ''))
)

function isVaccineExpiringSoon(row: CatHealthRecord): boolean {
  if (!row.vaccine_note || !row.recorded_at) return false
  const recordDate = new Date(row.recorded_at)
  const daysSince = (Date.now() - recordDate.getTime()) / 86400000
  return daysSince > 23 && daysSince < 30
}

function vaccineRowClass({ row }: { row: CatHealthRecord }): string {
  if (isVaccineExpiringSoon(row)) return 'expiring-row'
  return ''
}

const weightChartOption = computed(() => {
  const sorted = [...records.value].filter(r => r.weight_kg).sort((a, b) => (a.recorded_at || '').localeCompare(b.recorded_at || ''))
  return {
    tooltip: { trigger: 'axis', formatter: '{b}<br/>体重: {c} kg' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: sorted.map(r => r.recorded_at?.slice(5, 10) || ''), axisLabel: { color: '#667085' } },
    yAxis: { type: 'value', name: 'kg', min: (v: { min: number }) => Math.floor((v.min - 0.5) * 10) / 10 },
    series: [{
      type: 'line', data: sorted.map(r => r.weight_kg), smooth: true,
      symbol: 'circle', symbolSize: 8,
      itemStyle: { color: '#0f766e', borderColor: '#fff', borderWidth: 2 },
      lineStyle: { width: 3, color: '#0f766e' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(15,118,110,0.2)' }, { offset: 1, color: 'rgba(15,118,110,0)' }] } },
      label: { show: true, position: 'top', fontSize: 11, formatter: '{c} kg', color: '#0f766e' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(15,118,110,0.3)' } },
    }],
  }
})

async function loadData() {
  loading.value = true
  try {
    const storeId = auth.user?.store_id || auth.user?.storeId || 1
    const cats = await api.get<CatType[]>('/cats', { storeId })
    cat.value = cats.find(c => c.id === catId) || null
  } catch {
    cat.value = fallbackCats.find(c => c.id === catId) || null
  }
  if (cat.value) {
    Object.assign(editForm, {
      name: cat.value.name, breed: cat.value.breed,
      health_status: cat.value.health_status || 'healthy',
      tagsStr: (cat.value.personality_tags || []).join(', '),
    })
  }
  try { records.value = await api.get<CatHealthRecord[]>('/cat-health-records', { catId }) }
  catch { records.value = [] }
  loading.value = false
}

async function saveInfo() {
  if (!cat.value) return
  saving.value = true
  try {
    await api.patch(`/cats/${cat.value.id}`, {
      name: editForm.name, breed: editForm.breed,
      health_status: editForm.health_status,
      personality_tags: editForm.tagsStr.split(',').map(s => s.trim()).filter(Boolean),
    })
    ElMessage.success('信息已更新')
    await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { saving.value = false }
}

async function updatePhoto(photoUrl: string) {
  if (!cat.value) return
  try {
    await api.patch(`/cats/${cat.value.id}/photo`, { photoUrl })
    cat.value.photo_url = photoUrl
    ElMessage.success('照片已更新')
  } catch (e) { ElMessage.error((e as Error).message) }
}

async function saveRecord() {
  if (recordFormRef.value) {
    const valid = await recordFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  savingRecord.value = true
  try {
    await api.post('/cat-health-records', { cat_id: catId, ...recordForm })
    ElMessage.success('记录已添加')
    recordDialog.value = false
    Object.assign(recordForm, { weight_kg: 0, vaccine_note: '', interaction_note: '' })
    await loadData()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { savingRecord.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.ck-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.toolbar-left { display: flex; align-items: center; gap: var(--space-sm); }
.toolbar-left h2 { margin: 0; }
.btn-icon { margin-right: 4px; }
.loading-placeholder { height: 200px; }
.text-muted { color: var(--muted); font-size: var(--text-sm); }

.info-card {
  display: flex; gap: var(--space-lg);
  background: var(--paper); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: var(--space-lg);
  margin-bottom: var(--space-base);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast);
}
.info-card:hover { box-shadow: var(--shadow-md); }
.info-photo { display: flex; flex-direction: column; align-items: center; gap: var(--space-sm); }
.photo { width: 140px; height: 140px; border-radius: var(--radius-lg); object-fit: cover; box-shadow: var(--shadow-sm); }
.photo-placeholder {
  width: 140px; height: 140px; border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--teal-light), #d4f0ed);
  color: var(--teal);
  display: grid; place-items: center;
}
.info-detail { flex: 1; display: flex; flex-direction: column; gap: var(--space-sm); }
.info-row { display: flex; align-items: center; gap: var(--space-sm); }
.info-row .label { width: 80px; font-size: var(--text-sm); color: var(--muted); flex-shrink: 0; }
.info-input { width: 200px; }

.section-card {
  background: var(--paper); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: var(--space-lg);
  margin-bottom: var(--space-base);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast);
}
.section-card:hover { box-shadow: var(--shadow-md); }
.section-card h3 { margin: 0 0 var(--space-base); font-size: var(--text-base); font-weight: 700; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-base); }
.section-head h3 { margin: 0; }
.chart-canvas { height: 220px; }

/* 疫苗即将到期高亮行 */
:deep(.expiring-row) { background: #fef3c7 !important; }
.expiring-tag { margin-left: var(--space-xs); font-size: 10px; }
</style>
