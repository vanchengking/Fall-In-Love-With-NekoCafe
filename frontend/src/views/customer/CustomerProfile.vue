<template>
  <div>
    <h2 style="margin-bottom: 20px">个人中心</h2>
    <div class="profile-grid">
      <div class="profile-card">
        <h3>{{ profile.name || auth.user?.name || '未登录' }}</h3>
        <div class="field"><span class="label">手机号</span><span>{{ profile.mobile_number || auth.user?.mobileNumber || auth.user?.mobile_number || '-' }}</span></div>
        <div class="field"><span class="label">角色</span><span>{{ profile.role || auth.user?.role || '-' }}</span></div>
        <div class="field"><span class="label">会员等级</span><span class="member-level">{{ getLevelLabel(profile.member_level) }}</span></div>
        <div class="field"><span class="label">积分</span><span class="points">{{ profile.points ?? '-' }}</span></div>
        
        <!-- 会员权益信息 -->
        <div v-if="memberInfo" class="member-benefits">
          <div class="benefit-item">
            <span class="label">会员折扣</span>
            <span class="discount">{{ (memberInfo.discount * 10).toFixed(1) }}折</span>
          </div>
          <div v-if="memberInfo.next_level" class="benefit-item">
            <span class="label">下一等级</span>
            <span>{{ getLevelLabel(memberInfo.next_level) }}</span>
          </div>
          <div v-if="memberInfo.points_to_next_level > 0" class="benefit-item">
            <span class="label">距离升级</span>
            <span>还需 {{ memberInfo.points_to_next_level }} 积分</span>
          </div>
          <div v-else class="benefit-item">
            <span class="label">会员状态</span>
            <span class="max-level">已达到最高等级</span>
          </div>
        </div>
        
        <el-button type="danger" plain style="width: 100%; margin-top: 16px" @click="handleLogout">退出登录</el-button>
      </div>

      <div class="profile-section">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="个人资料" name="account">
            <div class="prefs-section">
              <p class="prefs-tip">姓名、手机号、用餐偏好可自行维护；会员等级与积分由系统规则计算，仅供查看。</p>
              <el-form label-position="top" style="max-width: 360px">
                <el-form-item label="姓名">
                  <el-input v-model="profileForm.name" placeholder="请输入姓名" maxlength="32" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.mobileNumber" placeholder="请输入手机号" maxlength="11" />
                </el-form-item>
                <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的预约" name="reservations">
            <div v-for="r in reservations" :key="r.id" class="res-item">
              <div class="res-left">
                <el-tag :type="statusType(r.status)" size="small">{{ r.status_label || statusLabel(r.status) }}</el-tag>
                <span>{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</span>
              </div>
              <el-button v-if="r.status === 'created' || r.status === 'booked'" type="danger" plain size="small" @click="cancelReservation(r)">取消预约</el-button>
            </div>
            <el-empty v-if="!reservations.length" description="暂无预约" />
          </el-tab-pane>

          <el-tab-pane label="我的订单" name="orders">
            <div v-for="o in orders" :key="o.id" class="order-item">
              <div class="order-header" @click="toggleOrderDetail(o.id)">
                <div>
                  <strong>订单 #{{ o.id }}</strong>
                  <span class="order-meta">{{ o.created_at }} · {{ cents(o.total_cents) }}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <el-tag :type="getOrderStatusType(o.status)" size="small">{{ getOrderStatusLabel(o.status) }}</el-tag>
                  <el-icon :class="{ 'is-rotate': expandedOrderId === o.id }"><ArrowDown /></el-icon>
                </div>
              </div>
              
              <!-- 订单详情（展开显示） -->
              <div v-if="expandedOrderId === o.id" class="order-detail">
                <div v-if="orderDetails[o.id]">
                  <!-- 原价和折扣价对比 -->
                  <div v-if="orderDetails[o.id].original_total_cents && orderDetails[o.id].original_total_cents !== orderDetails[o.id].total_cents" class="price-comparison">
                    <div class="price-row">
                      <span>原价</span>
                      <span class="original-price">{{ cents(orderDetails[o.id].original_total_cents) }}</span>
                    </div>
                    <div class="price-row discount">
                      <span>折扣优惠（{{ (orderDetails[o.id].discount_rate * 10).toFixed(1) }}折）</span>
                      <span class="discount-amount">- {{ cents(orderDetails[o.id].original_total_cents - orderDetails[o.id].total_cents) }}</span>
                    </div>
                  </div>
                  
                  <div v-for="item in orderDetails[o.id].items" :key="item.id" class="order-dish">
                    <span>{{ item.menu_item_name }} × {{ item.quantity }}</span>
                    <span>{{ cents(item.unit_price_cents * item.quantity) }}</span>
                  </div>
                  <div class="order-total">
                    <span>实付金额</span>
                    <span>{{ cents(o.total_cents) }}</span>
                  </div>
                </div>
                <div v-else class="loading">加载中...</div>
                
                <!-- 撤销订单按钮 -->
                <el-button 
                  v-if="o.status === 'paid'" 
                  type="danger" 
                  plain 
                  size="small" 
                  style="margin-top: 12px; width: 100%"
                  @click="cancelOrder(o)">
                  撤销订单
                </el-button>
              </div>
            </div>
            <el-empty v-if="!orders.length" description="暂无订单" />
          </el-tab-pane>

          <el-tab-pane label="用餐偏好" name="prefs">
            <div class="prefs-section">
              <p class="prefs-tip">设置您的用餐偏好，帮助我们为您提供更好的体验</p>

              <!-- 当前偏好标签 -->
              <div class="current-prefs">
                <span v-if="!preferences.length" class="no-prefs">暂无偏好设置，添加后我们将为您提供更个性化的服务</span>
                <el-tag
                  v-for="(p, idx) in preferences"
                  :key="idx"
                  closable
                  @close="removePreference(idx)"
                  type="primary"
                  style="margin: 0 8px 8px 0"
                >{{ p }}</el-tag>
              </div>

              <!-- 预设偏好快捷添加 -->
              <div class="preset-prefs">
                <div class="preset-label">快捷添加：</div>
                <el-button
                  v-for="pp in presetPrefs"
                  :key="pp"
                  size="small"
                  :type="preferences.includes(pp) ? 'primary' : 'default'"
                  @click="togglePreference(pp)"
                  style="margin: 0 6px 6px 0"
                >{{ pp }}</el-button>
              </div>

              <!-- 自定义输入 -->
              <div class="custom-pref">
                <el-input
                  v-model="newPreference"
                  placeholder="输入自定义偏好，如：过生日"
                  size="small"
                  style="width: 240px; margin-right: 8px"
                  @keyup.enter="addCustomPreference"
                />
                <el-button size="small" @click="addCustomPreference" :disabled="!newPreference.trim()">添加</el-button>
              </div>

              <el-button type="primary" size="small" style="margin-top: 16px" @click="savePreferences" :loading="savingPrefs">保存偏好</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="积分明细" name="points">
            <div class="points-toolbar">
              <span>当前积分：<strong class="points">{{ profile.points ?? '-' }}</strong></span>
              <el-button size="small" :loading="loadingPoints" @click="refreshPoints">刷新</el-button>
            </div>
            <el-table v-if="pointsHistory.length" :data="pointsHistory" size="small">
              <el-table-column prop="created_at" label="时间" width="160" />
              <el-table-column label="变动" width="80">
                <template #default="{ row }">
                  <span :class="row.delta >= 0 ? 'delta-plus' : 'delta-minus'">{{ row.delta >= 0 ? `+${row.delta}` : row.delta }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="balance_after" label="变动后余额" width="100" />
              <el-table-column label="来源" width="150">
                <template #default="{ row }">
                  {{ sourceLabel(row.source_type) }}<span v-if="row.source_id" class="source-id"> #{{ row.source_id }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="说明" min-width="140" />
            </el-table>
            <el-empty v-else description="暂无积分变动记录" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { cents, statusLabel, statusType } from '@/utils/format'
import type { Reservation, Order, PointTransaction } from '@/types'

const auth = useAuthStore()
const router = useRouter()
const activeTab = ref('reservations')
const reservations = ref<Reservation[]>([])
const orders = ref<Order[]>([])
const profile = ref<any>({})
const memberInfo = ref<any>(null)
const expandedOrderId = ref<number | null>(null)
const orderDetails = ref<Record<number, any>>({})

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

function getLevelLabel(level: string) {
  const map: Record<string, string> = {
    'bronze': '青铜',
    'silver': '白银',
    'gold': '黄金',
    'platinum': '铂金'
  }
  return map[level] || level || '-'
}

async function cancelReservation(r: Reservation) {
  try {
    await ElMessageBox.confirm('确定要取消这个预约吗？', '取消预约', { type: 'warning' })
    const updated = await api.patch<Reservation>(`/reservations/${r.id}/cancel`)
    reservations.value = reservations.value.map(x => x.id === updated.id ? updated : x)
    ElMessage.success('预约已取消')
  } catch (e) {
    if ((e as string) !== 'cancel') ElMessage.error((e as Error).message)
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
  if (phone) {
    try { reservations.value = await api.get<Reservation[]>('/reservations', { mobileNumber: phone }) }
    catch { /* empty */ }
  }
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { /* empty */ }
  
  // 加载用户资料和会员信息
  await loadProfile()
  await loadMemberInfo()
  loadPreferences()
  syncProfileForm()
  await loadPointsHistory()
})

// 展开/收起订单详情
async function toggleOrderDetail(orderId: number) {
  if (expandedOrderId.value === orderId) {
    expandedOrderId.value = null
    return
  }
  expandedOrderId.value = orderId
  
  // 加载订单详情
  if (!orderDetails.value[orderId]) {
    try {
      orderDetails.value[orderId] = await api.get(`/orders/${orderId}`)
    } catch (e) {
      ElMessage.error('加载订单详情失败')
    }
  }
}

// 撤销订单
async function cancelOrder(order: any) {
  try {
    await ElMessageBox.confirm('确定要撤销这个订单吗？撤销后积分将返还', '撤销订单', { type: 'warning' })
    await api.patch(`/orders/${order.id}/cancel`)
    ElMessage.success('订单已撤销')
    // 刷新订单列表
    orders.value = await api.get<Order[]>('/orders')
    expandedOrderId.value = null
  } catch (e) {
    if ((e as string) !== 'cancel') ElMessage.error((e as Error).message)
  }
}

// 获取订单状态类型（用于Tag颜色）
function getOrderStatusType(status: string) {
  const map: Record<string, string> = {
    'paid': 'success',
    'cancelled': 'danger',
    'preparing': 'warning',
    'served': 'info'
  }
  return map[status] || 'info'
}

// 获取订单状态标签
function getOrderStatusLabel(status: string) {
  const map: Record<string, string> = {
    'paid': '已支付',
    'cancelled': '已撤销',
    'preparing': '制作中',
    'served': '已上菜'
  }
  return map[status] || status
}
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 320px 1fr; gap: 24px; }
@media (max-width: 768px) { .profile-grid { grid-template-columns: 1fr; } }
.profile-card { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; }
.profile-card h3 { font-size: 20px; margin-bottom: 16px; }
.field { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0eeea; font-size: 14px; }
.label { color: #667085; }

.member-benefits { 
  margin-top: 16px; 
  padding: 16px; 
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
  border-radius: 8px; 
  color: white; 
}
.benefit-item { 
  display: flex; 
  justify-content: space-between; 
  padding: 8px 0; 
  font-size: 14px; 
  border-bottom: 1px solid rgba(255,255,255,0.2); 
}
.benefit-item:last-child { border-bottom: none; }
.benefit-item .label { color: rgba(255,255,255,0.8); }
.discount { font-weight: bold; font-size: 16px; }
.points { color: #e6a23c; font-weight: bold; }
.member-level { color: #409eff; font-weight: bold; }
.max-level { color: #67c23a; font-weight: bold; }

.profile-section { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; }
.res-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0eeea; }
.res-left { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0eeea; }
.order-meta { display: block; font-size: 13px; color: #667085; margin-top: 2px; }

/* 订单详情中的原价和折扣价对比 */
.price-comparison { margin: 12px 0; padding: 12px; background: #f8f9fa; border-radius: 8px; }
.price-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.original-price { color: #999; }
.original-price span:last-child { text-decoration: line-through; color: #999; }
.discount { color: #e6a23c; }
.discount-amount { color: #e6a23c; font-weight: 700; }

.order-detail { padding: 12px; background: #f8f9fa; border-radius: 8px; margin-top: 8px; }
.order-dish { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; color: #333; }
.order-total { display: flex; justify-content: space-between; padding-top: 8px; margin-top: 8px; border-top: 1px solid #e8e5df; font-weight: 700; }
.prefs-section { padding: 8px 0; }
.prefs-tip { font-size: 13px; color: #667085; margin-bottom: 16px; }
.current-prefs { margin-bottom: 16px; min-height: 32px; display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.no-prefs { color: #999; font-size: 13px; padding: 8px 0; }
.preset-prefs { margin-bottom: 16px; }
.preset-label { font-size: 13px; color: #667085; margin-bottom: 8px; }
.custom-pref { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.loading { text-align: center; color: #999; padding: 12px; font-size: 13px; }
.points-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 14px; }
.delta-plus { color: #67c23a; font-weight: 700; }
.delta-minus { color: #f56c6c; font-weight: 700; }
.source-id { color: #909399; font-size: 12px; }
</style>
