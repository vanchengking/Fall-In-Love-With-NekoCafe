<template>
  <div class="page-enter-active">
    <h2>个人中心</h2>
    <div class="profile-grid">
      <!-- 骨架屏 -->
      <div v-if="loading" class="profile-card">
        <div class="skeleton" style="height: 22px; width: 40%; margin-bottom: var(--space-base)"></div>
        <div v-for="i in 4" :key="i" style="padding: var(--space-sm) 0; border-bottom: 1px solid var(--line); display: flex; justify-content: space-between">
          <div class="skeleton" style="height: 14px; width: 60px"></div>
          <div class="skeleton" style="height: 14px; width: 100px"></div>
        </div>
        <div class="skeleton" style="height: 36px; margin-top: var(--space-base)"></div>
      </div>
      <div v-else class="profile-card">
        <h3>{{ auth.user?.name || auth.user?.mobileNumber || '未登录' }}</h3>
        <div class="field"><span class="label">手机号</span><span>{{ auth.user?.mobileNumber || auth.user?.mobile_number || '-' }}</span></div>
        <div class="field"><span class="label">角色</span><span>{{ auth.user?.role || '-' }}</span></div>
        <div class="field"><span class="label">会员等级</span><span>{{ auth.user?.memberLevel || auth.user?.member_level || '-' }}</span></div>
        <div class="field"><span class="label">积分</span><span>{{ auth.user?.points ?? '-' }}</span></div>
        <el-button type="danger" plain class="logout-btn" @click="handleLogout">退出登录</el-button>
      </div>

      <div class="profile-section">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="我的预约" name="reservations">
            <!-- 骨架屏 -->
            <template v-if="loading">
              <div v-for="i in 3" :key="i" style="padding: var(--space-sm) 0; border-bottom: 1px solid var(--line)">
                <div class="skeleton" style="height: 14px; width: 70%; margin-bottom: var(--space-xs)"></div>
                <div class="skeleton" style="height: 13px; width: 50%"></div>
              </div>
            </template>
            <template v-else>
              <div v-for="r in reservations" :key="r.id" class="res-item">
                <div class="res-left">
                  <el-tag :type="statusType(r.status)" size="small">{{ r.status_label || statusLabel(r.status) }}</el-tag>
                  <span>{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</span>
                </div>
                <div v-if="r.status === 'created' || r.status === 'booked'" class="res-actions">
                  <el-button size="small" @click="openReschedule(r)">改约</el-button>
                  <el-button type="danger" plain size="small" @click="cancelReservation(r)">取消预约</el-button>
                </div>
              </div>
              <el-empty v-if="!reservations.length" description="暂无预约" />
            </template>
          </el-tab-pane>

          <el-tab-pane label="我的订单" name="orders">
            <template v-if="loading">
              <div v-for="i in 3" :key="i" style="padding: var(--space-sm) 0; border-bottom: 1px solid var(--line)">
                <div class="skeleton" style="height: 14px; width: 60%; margin-bottom: var(--space-xs)"></div>
                <div class="skeleton" style="height: 13px; width: 40%"></div>
              </div>
            </template>
            <template v-else>
              <div v-for="o in orders" :key="o.id" class="order-item">
                <div>
                  <strong>订单 #{{ o.id }}</strong>
                  <span class="order-meta">{{ o.status }} · {{ cents(o.total_cents) }}</span>
                </div>
                <el-tag :type="o.status === 'paid' ? 'success' : 'info'" size="small">{{ o.payment_status }}</el-tag>
              </div>
              <el-empty v-if="!orders.length" description="暂无订单" />
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="rescheduleVisible" title="改约" width="420px">
      <el-form label-position="top">
        <el-form-item label="日期">
          <el-date-picker
            v-model="rescheduleForm.reservationDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker
            v-model="rescheduleForm.reservationTime"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="人数">
          <el-input-number
            v-model="rescheduleForm.partySize"
            :min="1"
            :max="12"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="rescheduleForm.note"
            type="textarea"
            :rows="3"
            placeholder="如需重新安排桌位，可填写补充说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rescheduleVisible = false">取消</el-button>
        <el-button type="primary" :loading="rescheduling" @click="submitReschedule">确认改约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { cents, statusLabel, statusType } from '@/utils/format'
import type { Reservation, Order } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const activeTab = ref('reservations')
const reservations = ref<Reservation[]>([])
const orders = ref<Order[]>([])
const loading = ref(true)
const profile = ref<any>({})
const memberInfo = ref<any>(null)
const expandedOrderId = ref<number | null>(null)
const orderDetails = ref<Record<number, any>>({})
const rescheduleVisible = ref(false)
const rescheduling = ref(false)
const rescheduleForm = reactive({
  id: 0,
  storeId: 0,
  recommendedCatId: null as number | null,
  reservationDate: '',
  reservationTime: '',
  partySize: 2,
  note: '',
})

// 个人资料编辑（姓名/手机号；等级与积分只读）
const profileForm = reactive({ name: '', mobileNumber: '' })
const savingProfile = ref(false)

// 积分明细
const pointsHistory = ref<PointTransaction[]>([])
const loadingPoints = ref(false)

// 偏好管理
const preferences = ref<string[]>([])
const newPreference = ref('')
const savingPrefs = ref(false)
const presetPrefs = [
  '喜欢靠窗座位',
  '喜欢安静角落',
  '对猫毛过敏',
  '不吃辣',
  '喜欢互动多的猫咪',
  '带小孩',
  '过生日',
  '需要儿童座椅',
  '需要充电插座',
  '素食优先',
]

function loadPreferences() {
  if (profile.value?.preferences) {
    preferences.value = [...profile.value.preferences]
  } else if (auth.user?.preferences) {
    preferences.value = [...auth.user.preferences]
  }
}

function togglePreference(pref: string) {
  const idx = preferences.value.indexOf(pref)
  if (idx >= 0) {
    preferences.value.splice(idx, 1)
  } else {
    preferences.value.push(pref)
  }
}

function addCustomPreference() {
  const val = newPreference.value.trim()
  if (!val) return
  if (preferences.value.includes(val)) {
    ElMessage.warning('该偏好已存在')
    return
  }
  preferences.value.push(val)
  newPreference.value = ''
}

function removePreference(idx: number) {
  preferences.value.splice(idx, 1)
}

async function savePreferences() {
  savingPrefs.value = true
  try {
    const res = await api.put('/users/me', { preferences: preferences.value })
    profile.value = res
    if (auth.user) auth.user.preferences = preferences.value
    ElMessage.success('偏好保存成功')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    savingPrefs.value = false
  }
}

function syncProfileForm() {
  profileForm.name = profile.value?.name || ''
  profileForm.mobileNumber = profile.value?.mobile_number || ''
}

async function saveProfile() {
  if (!profileForm.name.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  if (!/^\d{8,}$/.test(profileForm.mobileNumber.replace(/\D/g, ''))) {
    ElMessage.warning('手机号至少包含 8 位数字')
    return
  }
  savingProfile.value = true
  try {
    const res = await api.put<any>('/users/me', {
      name: profileForm.name.trim(),
      mobileNumber: profileForm.mobileNumber.trim(),
    })
    profile.value = res
    syncProfileForm()
    // 同步认证状态与本地缓存，导航栏等处立即生效
    if (auth.user) {
      auth.user.name = res.name
      auth.user.mobile_number = res.mobile_number
      auth.user.mobileNumber = res.mobile_number
      localStorage.setItem('neko-auth', JSON.stringify({ token: auth.token, user: auth.user }))
    }
    ElMessage.success('资料保存成功')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

const SOURCE_LABELS: Record<string, string> = {
  reservation_finished: '预约完成奖励',
  order_paid: '订单支付积分',
  order_cancelled: '订单撤销扣回',
}

function sourceLabel(sourceType: string) {
  return SOURCE_LABELS[sourceType] || sourceType
}

async function loadPointsHistory() {
  loadingPoints.value = true
  try {
    pointsHistory.value = await api.get<PointTransaction[]>('/users/me/points/history')
  } catch (e) {
    console.error('加载积分明细失败', e)
  } finally {
    loadingPoints.value = false
  }
}

/** 刷新资料 + 积分明细：完成预约/支付后可在此看到积分变化 */
async function refreshPoints() {
  await loadProfile()
  await loadMemberInfo()
  await loadPointsHistory()
}

// 切到积分明细页时自动刷新，保证看到最新的积分变化
watch(activeTab, (tab) => {
  if (tab === 'points') refreshPoints()
})

function handleLogout() { auth.logout(); router.push('/login') }

async function cancelReservation(r: Reservation) {
  try {
    await ElMessageBox.confirm('确定要取消这个预约吗？', '取消预约', { type: 'warning' })
    const updated = await api.patch<Reservation>(`/reservations/${r.id}/status`, { status: 'cancelled' })
    reservations.value = reservations.value.map(x => x.id === updated.id ? updated : x)
    ElMessage.success('预约已取消')
  } catch (e) {
    if ((e as string) !== 'cancel') ElMessage.error((e as Error).message)
  }
}

function openReschedule(r: Reservation) {
  rescheduleForm.id = r.id
  rescheduleForm.storeId = r.store_id
  rescheduleForm.recommendedCatId = r.recommended_cat_id ?? null
  rescheduleForm.reservationDate = r.reservation_date
  rescheduleForm.reservationTime = r.reservation_time
  rescheduleForm.partySize = r.party_size
  rescheduleForm.note = r.note || ''
  rescheduleVisible.value = true
}

async function submitReschedule() {
  if (!rescheduleForm.id || !rescheduleForm.reservationDate || !rescheduleForm.reservationTime) {
    ElMessage.warning('请完整填写改约信息')
    return
  }
  rescheduling.value = true
  try {
    const payload: Record<string, unknown> = {
      storeId: rescheduleForm.storeId,
      reservationDate: rescheduleForm.reservationDate,
      reservationTime: rescheduleForm.reservationTime,
      partySize: rescheduleForm.partySize,
      note: rescheduleForm.note,
    }
    if (rescheduleForm.recommendedCatId) {
      payload.recommendedCatId = rescheduleForm.recommendedCatId
    }
    const updated = await api.patch<Reservation>(`/reservations/${rescheduleForm.id}/reschedule`, payload)
    reservations.value = reservations.value.map(item => item.id === updated.id ? updated : item)
    rescheduleVisible.value = false
    ElMessage.success('改约成功，原桌位时段已释放')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    rescheduling.value = false
  }
}

async function loadProfile() {
  try {
    profile.value = await api.get<any>('/users/me')
  } catch (e) {
    console.error('加载用户资料失败', e)
  }
}

async function loadMemberInfo() {
  try {
    memberInfo.value = await api.get<any>('/users/me/member')
  } catch (e) {
    console.error('加载会员信息失败', e)
  }
}

onMounted(async () => {
  const phone = auth.user?.mobileNumber || auth.user?.mobile_number
  const tasks: Promise<void>[] = []
  if (phone) {
    tasks.push(api.get<Reservation[]>('/reservations', { mobileNumber: phone }).then(d => { reservations.value = d }).catch(() => {}))
  }
  tasks.push(api.get<Order[]>('/orders').then(d => { orders.value = d }).catch(() => {}))
  await Promise.all(tasks)
  loading.value = false
})
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 320px 1fr; gap: var(--space-lg); }
@media (max-width: 768px) { .profile-grid { grid-template-columns: 1fr; } }
.profile-card { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.profile-card h3 { font-size: var(--text-xl); margin-bottom: var(--space-base); }
.logout-btn { width: 100%; margin-top: var(--space-base); }
.field { display: flex; justify-content: space-between; padding: var(--space-sm) 0; border-bottom: 1px solid var(--line); font-size: var(--text-sm); }
.label { color: var(--muted); }
.profile-section { background: var(--paper); padding: var(--space-lg); border-radius: var(--radius-lg); border: 1px solid var(--line); }
.res-item { display: flex; justify-content: space-between; align-items: center; padding: var(--space-md) 0; border-bottom: 1px solid var(--line); transition: background var(--transition-fast); }
.res-item:hover { background: var(--wash); }
.res-left { display: flex; align-items: center; gap: var(--space-sm); font-size: var(--text-sm); }
.res-actions { display: flex; gap: var(--space-sm); }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: var(--space-md) 0; border-bottom: 1px solid var(--line); transition: background var(--transition-fast); }
.order-item:hover { background: var(--wash); }
.order-meta { display: block; font-size: var(--text-sm); color: var(--muted); margin-top: var(--space-xs); }

/* 会员信息 */
.member-benefits { margin-top: var(--space-base); padding: var(--space-base); background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: var(--radius-md); color: white; }
.benefit-item { display: flex; justify-content: space-between; padding: var(--space-sm) 0; font-size: var(--text-sm); border-bottom: 1px solid rgba(255,255,255,0.2); }
.benefit-item:last-child { border-bottom: none; }
.benefit-item .label { color: rgba(255,255,255,0.8); }
.profile-actions { margin-top: var(--space-base); display: flex; flex-direction: column; gap: var(--space-sm); align-items: stretch; }
.profile-actions :deep(.el-button) { margin-left: 0 !important; margin-right: 0 !important; }
.discount { font-weight: bold; font-size: var(--text-base); }
.points { color: #e6a23c; font-weight: bold; }
.member-level { color: #409eff; font-weight: bold; }
.max-level { color: #67c23a; font-weight: bold; }

/* 订单详情 */
.order-detail { padding: var(--space-sm); background: var(--wash); border-radius: var(--radius-md); margin-top: var(--space-sm); }
.order-dish { display: flex; justify-content: space-between; padding: var(--space-xs) 0; font-size: var(--text-sm); color: var(--ink); }
.order-total { display: flex; justify-content: space-between; padding-top: var(--space-sm); margin-top: var(--space-sm); border-top: 1px solid var(--line); font-weight: 700; }
.price-comparison { margin: var(--space-sm) 0; padding: var(--space-sm); background: var(--wash); border-radius: var(--radius-md); }
.price-row { display: flex; justify-content: space-between; padding: var(--space-xs) 0; font-size: var(--text-sm); }
.original-price { color: var(--muted); }
.original-price span:last-child { text-decoration: line-through; }
.discount-amount { color: #e6a23c; font-weight: 700; }

/* 偏好管理 */
.prefs-section { padding: var(--space-sm) 0; }
.prefs-tip { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-base); }
.current-prefs { margin-bottom: var(--space-base); min-height: 32px; display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-xs); }
.no-prefs { color: var(--muted); font-size: var(--text-sm); padding: var(--space-sm) 0; }
.preset-prefs { margin-bottom: var(--space-base); }
.preset-label { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-sm); }
.custom-pref { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-base); }
.loading { text-align: center; color: var(--muted); padding: var(--space-sm); font-size: var(--text-sm); }

/* 积分明细 */
.points-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-sm); font-size: var(--text-sm); }
.delta-plus { color: #67c23a; font-weight: 700; }
.delta-minus { color: #f56c6c; font-weight: 700; }
.source-id { color: #909399; font-size: var(--text-xs); }
</style>
