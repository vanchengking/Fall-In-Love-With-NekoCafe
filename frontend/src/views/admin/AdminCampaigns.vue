<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
      <h2>活动配置</h2>
      <el-button type="primary" @click="showCreate = true">新建优惠券</el-button>
    </div>

    <div class="campaign-list">
      <div v-for="c in coupons" :key="c.id" class="campaign-card">
        <div class="campaign-header">
          <el-tag :type="c.status === 'active' ? 'success' : 'info'" size="small">
            {{ c.status === 'active' ? '进行中' : '已结束' }}
          </el-tag>
          <h4>{{ c.title }}</h4>
        </div>
        <p class="campaign-desc">优惠码：{{ c.code }}</p>
        <div class="campaign-meta">
          <span>满 {{ (c.min_spend_cents / 100).toFixed(0) }} 元减 {{ (c.discount_cents / 100).toFixed(0) }} 元</span>
          <span>{{ c.valid_from }} ~ {{ c.valid_to }}</span>
        </div>
      </div>
      <div v-if="coupons.length === 0 && !loading" style="text-align: center; padding: 40px; color: #999">
        暂无优惠券
      </div>
    </div>

    <el-dialog v-model="showCreate" title="新建优惠券" width="480px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="优惠码">
          <el-input v-model="createForm.code" placeholder="如 NEKO20" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="如 新客立减 20 元" />
        </el-form-item>
        <el-form-item label="优惠金额(分)">
          <el-input-number v-model="createForm.discount_cents" :min="100" :step="500" />
        </el-form-item>
        <el-form-item label="最低消费(分)">
          <el-input-number v-model="createForm.min_spend_cents" :min="0" :step="500" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="createForm.dateRange" type="daterange" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="createCoupon">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/http'
import { ElMessage } from 'element-plus'

interface Coupon {
  id: number
  code: string
  title: string
  discount_cents: number
  min_spend_cents: number
  valid_from: string
  valid_to: string
  status: string
}

const coupons = ref<Coupon[]>([])
const loading = ref(false)
const showCreate = ref(false)
const createForm = ref({
  code: '',
  title: '',
  discount_cents: 1000,
  min_spend_cents: 5000,
  dateRange: null as [string, string] | null,
})

async function loadCoupons() {
  loading.value = true
  try {
    coupons.value = await api.get<Coupon[]>('/coupons')
  } catch {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

async function createCoupon() {
  const f = createForm.value
  if (!f.code || !f.title) {
    ElMessage.warning('请填写优惠码和标题')
    return
  }
  try {
    await api.post('/coupons', {
      code: f.code,
      title: f.title,
      discount_cents: f.discount_cents,
      min_spend_cents: f.min_spend_cents,
      valid_from: f.dateRange?.[0] || null,
      valid_to: f.dateRange?.[1] || null,
    })
    ElMessage.success('创建成功')
    showCreate.value = false
    createForm.value = { code: '', title: '', discount_cents: 1000, min_spend_cents: 5000, dateRange: null }
    loadCoupons()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  }
}

onMounted(loadCoupons)
</script>

<style scoped>
.campaign-list { display: flex; flex-direction: column; gap: 12px; }
.campaign-card { background: #fff; padding: 18px; border-radius: 12px; border: 1px solid #e8e5df; }
.campaign-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.campaign-header h4 { margin: 0; font-size: 16px; }
.campaign-desc { font-size: 14px; color: #667085; margin-bottom: 8px; }
.campaign-meta { display: flex; gap: 16px; font-size: 13px; color: #667085; }
</style>
