<template>
  <div class="page-enter-active">
    <div class="admin-toolbar">
      <h2>活动配置</h2>
      <el-button type="primary" @click="openDialog(null)"><Plus :size="14" class="btn-icon" />新建优惠券</el-button>
    </div>

    <!-- 统计概览 -->
    <div class="stats-strip">
      <div class="stat-card"><span>总优惠券</span><strong>{{ coupons.length }}</strong></div>
      <div class="stat-card"><span>进行中</span><strong>{{ coupons.filter(c => c.status === 'active').length }}</strong></div>
      <div class="stat-card"><span>已结束</span><strong>{{ coupons.filter(c => c.status !== 'active').length }}</strong></div>
      <div class="stat-card"><span>总核销</span><strong>{{ coupons.reduce((s, c) => s + (c.used_count || 0), 0) }}</strong></div>
    </div>

    <div class="campaign-list">
      <div v-for="c in coupons" :key="c.id" class="campaign-card">
        <div class="cc-header">
          <div class="cc-title-row">
            <el-tag :type="c.status === 'active' ? 'success' : 'info'" size="small" effect="plain">
              {{ c.status === 'active' ? '进行中' : '已结束' }}
            </el-tag>
            <h4>{{ c.title }}</h4>
          </div>
          <div class="cc-actions">
            <el-button size="small" text @click="openDialog(c)"><Edit :size="14" /></el-button>
            <el-button size="small" text type="danger" @click="deleteCoupon(c)"><Trash2 :size="14" /></el-button>
          </div>
        </div>
        <p class="cc-code">优惠码：<strong>{{ c.code }}</strong></p>
        <div class="cc-meta">
          <span>满 {{ (c.min_spend_cents / 100).toFixed(0) }} 元减 {{ (c.discount_cents / 100).toFixed(0) }} 元</span>
          <span>{{ c.valid_from || '-' }} ~ {{ c.valid_to || '-' }}</span>
        </div>
        <div class="cc-stats">
          <div class="ccs-item"><span>已领取</span><strong>{{ c.claimed_count || 0 }}</strong></div>
          <div class="ccs-item"><span>已核销</span><strong>{{ c.used_count || 0 }}</strong></div>
          <div class="ccs-item"><span>核销率</span><strong>{{ c.claimed_count ? Math.round((c.used_count || 0) / c.claimed_count * 100) : 0 }}%</strong></div>
        </div>
      </div>
      <el-empty v-if="!coupons.length && !loading" description="暂无优惠券" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="480px" class="admin-dialog">
      <el-form ref="formRef" :model="form" :rules="couponRules" label-width="100px" label-position="right">
        <el-form-item label="优惠码"><el-input v-model="form.code" placeholder="如 NEKO20" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="如 新客立减 20 元" /></el-form-item>
        <el-form-item label="优惠金额(分)"><el-input-number v-model="form.discount_cents" :min="100" :step="500" /></el-form-item>
        <el-form-item label="最低消费(分)"><el-input-number v-model="form.min_spend_cents" :min="0" :step="500" /></el-form-item>
        <el-form-item label="有效期"><el-date-picker v-model="form.dateRange" type="daterange" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCoupon">{{ form.id ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { api } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Trash2 } from '@lucide/vue'

interface Coupon {
  id: number; code: string; title: string; discount_cents: number; min_spend_cents: number
  valid_from: string; valid_to: string; status: string; claimed_count?: number; used_count?: number
}

const coupons = ref<Coupon[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const couponRules = {
  code: [{ required: true, message: '请输入优惠码', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
}
const form = reactive({
  id: 0, code: '', title: '', discount_cents: 1000, min_spend_cents: 5000, dateRange: null as [string, string] | null,
})

function openDialog(coupon: Coupon | null) {
  if (coupon) {
    Object.assign(form, { id: coupon.id, code: coupon.code, title: coupon.title, discount_cents: coupon.discount_cents, min_spend_cents: coupon.min_spend_cents, dateRange: coupon.valid_from && coupon.valid_to ? [coupon.valid_from, coupon.valid_to] : null })
  } else {
    Object.assign(form, { id: 0, code: '', title: '', discount_cents: 1000, min_spend_cents: 5000, dateRange: null })
  }
  dialogVisible.value = true
}

async function saveCoupon() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  const payload = { code: form.code, title: form.title, discount_cents: form.discount_cents, min_spend_cents: form.min_spend_cents, valid_from: form.dateRange?.[0] || null, valid_to: form.dateRange?.[1] || null }
  try {
    if (form.id) { await api.patch(`/coupons/${form.id}`, payload); ElMessage.success('已更新') }
    else { await api.post('/coupons', payload); ElMessage.success('创建成功') }
    dialogVisible.value = false; await loadCoupons()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  finally { saving.value = false }
}

async function deleteCoupon(coupon: Coupon) {
  await ElMessageBox.confirm(`确认删除优惠券 ${coupon.title}？`, '删除', { type: 'warning' })
  try { await api.remove(`/coupons/${coupon.id}`); ElMessage.success('已删除'); await loadCoupons() }
  catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

async function loadCoupons() {
  loading.value = true
  try { coupons.value = await api.get<Coupon[]>('/coupons') }
  catch {
    const { fallbackCoupons } = await import('@/utils/fallback')
    coupons.value = fallbackCoupons as Coupon[]
  }
  finally { loading.value = false }
}

onMounted(loadCoupons)
</script>

<style scoped>
.admin-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.admin-toolbar h2 { margin: 0; }
.btn-icon { margin-right: 4px; }

.stats-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-sm); margin-bottom: var(--space-lg); padding: var(--space-base); background: linear-gradient(135deg, var(--teal-light), var(--warm-light)); border-radius: var(--radius-lg); }
.stat-card { text-align: center; padding: var(--space-sm); }
.stat-card span { display: block; font-size: var(--text-xs); color: var(--muted); }
.stat-card strong { display: block; font-size: var(--text-xl); margin-top: 2px; }

.campaign-list { display: flex; flex-direction: column; gap: var(--space-md); }
.campaign-card { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); transition: all var(--transition-fast); }
.campaign-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
.cc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-sm); }
.cc-title-row { display: flex; align-items: center; gap: var(--space-sm); }
.cc-title-row h4 { margin: 0; font-size: var(--text-base); }
.cc-actions { display: flex; gap: 2px; }
.cc-code { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-sm); }
.cc-code strong { color: var(--ink); font-family: var(--font-mono); }
.cc-meta { display: flex; gap: var(--space-base); font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-sm); }
.cc-stats { display: flex; gap: var(--space-lg); padding-top: var(--space-sm); border-top: 1px solid var(--line); }
.ccs-item span { display: block; font-size: var(--text-xs); color: var(--muted); }
.ccs-item strong { display: block; font-size: var(--text-base); color: var(--ink); }

@media (max-width: 640px) { .stats-strip { grid-template-columns: repeat(2, 1fr); } .cc-meta { flex-direction: column; gap: var(--space-xs); } }
</style>
