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
              <div>
                <strong>订单 #{{ o.id }}</strong>
                <span class="order-meta">{{ o.status }} · {{ cents(o.total_cents) }}</span>
              </div>
              <el-tag :type="o.status === 'paid' ? 'success' : 'info'" size="small">{{ o.payment_status }}</el-tag>
            </div>
            <el-empty v-if="!orders.length" description="暂无订单" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
const profile = ref<any>({})
const memberInfo = ref<any>(null)

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
})
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
</style>
