<template>
  <div>
    <h2 style="margin-bottom: 20px">猫咪健康档案</h2>
    <div class="cat-grid">
      <div v-for="cat in cats" :key="cat.id" class="cat-card" :class="{ selected: selectedCat?.id === cat.id }" @click="selectCat(cat)">
        <img v-if="cat.photo_url" :src="cat.photo_url" class="cat-photo" />
        <div v-else class="cat-avatar"><Cat :size="24" /></div>
        <h4>{{ cat.name }}</h4>
        <p>{{ cat.breed }} · {{ cat.health_status }}</p>
        <div class="tags"><el-tag v-for="t in cat.personality_tags" :key="t" size="small">{{ t }}</el-tag></div>
      </div>
    </div>
    <div v-if="selectedCat" class="detail-panel">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
        <h3 style="margin: 0">{{ selectedCat.name }} - 健康记录</h3>
        <div v-if="canWrite" style="display: flex; align-items: center; gap: 8px">
          <span style="font-size: 13px; color: #667085">上传照片：</span>
          <PhotoUpload :model-value="selectedCat.photo_url || ''" @update:model-value="updateCatPhoto(selectedCat!.id, $event)" placeholder="猫咪照片" />
        </div>
      </div>

      <div v-if="records.length >= 2" class="chart-section">
        <h4>体重趋势</h4>
        <v-chart :option="weightChartOption" autoresize style="height: 200px" />
      </div>

      <div class="records">
        <div v-for="rec in records" :key="rec.id" class="record-item">
          <span class="record-time">{{ rec.recorded_at?.slice(0, 16) || '-' }}</span>
          <span>体重: {{ rec.weight_kg }}kg</span>
          <span>{{ rec.vaccine_note || '-' }}</span>
          <span>{{ rec.interaction_note || '-' }}</span>
        </div>
        <el-empty v-if="!records.length" description="暂无记录" />
      </div>
      <el-form v-if="canWrite" inline style="margin-top: 16px" @submit.prevent="addRecord">
        <el-form-item><el-input-number v-model="healthForm.weightKg" :min="1" :step="0.1" placeholder="体重" /></el-form-item>
        <el-form-item><el-input v-model="healthForm.vaccineNote" placeholder="疫苗备注" /></el-form-item>
        <el-form-item><el-input v-model="healthForm.interactionNote" placeholder="互动备注" /></el-form-item>
        <el-form-item><el-button type="primary" @click="addRecord">添加记录</el-button></el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Cat } from '@lucide/vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import PhotoUpload from '@/components/PhotoUpload.vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { fallbackCats } from '@/utils/fallback'
import type { Cat as CatType, CatHealthRecord } from '@/types'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const auth = useAuthStore()
const canWrite = ['cat_keeper', 'manager', 'admin'].includes(auth.user?.role || '')

const cats = ref<CatType[]>([])
const selectedCat = ref<CatType | null>(null)
const records = ref<CatHealthRecord[]>([])
const healthForm = reactive({ weightKg: 4.5, vaccineNote: '', interactionNote: '' })

const weightChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 10, bottom: 30 },
  xAxis: { type: 'category', data: records.value.map(r => r.recorded_at?.slice(5, 10) || '-').reverse() },
  yAxis: { type: 'value', name: 'kg', min: (v: { min: number }) => Math.floor(v.min - 0.5), max: (v: { max: number }) => Math.ceil(v.max + 0.5) },
  series: [{
    type: 'line', smooth: true, areaStyle: { opacity: 0.15 },
    data: records.value.map(r => Number(r.weight_kg)).reverse(),
    itemStyle: { color: '#0f766e' },
  }],
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

onMounted(async () => {
  try { cats.value = await api.get<CatType[]>('/cats', { storeId: 1 }) }
  catch { cats.value = fallbackCats }
})
</script>

<style scoped>
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; margin-bottom: 24px; }
.cat-card { background: #fff; padding: 16px; border-radius: 12px; border: 2px solid #e8e5df; text-align: center; cursor: pointer; transition: border-color 0.15s; }
.cat-card.selected { border-color: #0f766e; }
.cat-photo { width: 100%; height: 80px; object-fit: cover; border-radius: 8px; margin-bottom: 6px; }
.cat-avatar { color: #0f766e; margin-bottom: 6px; }
.cat-card h4 { margin-bottom: 4px; }
.cat-card p { font-size: 12px; color: #667085; margin-bottom: 6px; }
.tags { display: flex; gap: 4px; justify-content: center; flex-wrap: wrap; }
.detail-panel { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; }
.detail-panel h3 { margin-bottom: 16px; }
.chart-section { margin-bottom: 20px; }
.chart-section h4 { font-size: 14px; color: #667085; margin-bottom: 8px; }
.records { display: flex; flex-direction: column; gap: 8px; }
.record-item { display: flex; gap: 16px; padding: 10px; background: #f8f9fa; border-radius: 8px; font-size: 13px; }
.record-time { color: #667085; font-size: 12px; min-width: 120px; }
</style>
