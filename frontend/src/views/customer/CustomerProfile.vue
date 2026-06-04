<template>
  <div>
    <h2 style="margin-bottom: 20px">个人中心</h2>
    <div class="profile-grid">
      <div class="profile-card">
        <h3>{{ auth.user?.name || '未登录' }}</h3>
        <div class="field"><span class="label">手机号</span><span>{{ auth.user?.mobileNumber || auth.user?.mobile_number || '-' }}</span></div>
        <div class="field"><span class="label">角色</span><span>{{ auth.user?.role || '-' }}</span></div>
        <div class="field"><span class="label">会员等级</span><span>{{ auth.user?.memberLevel || auth.user?.member_level || '-' }}</span></div>
        <div class="field"><span class="label">积分</span><span>{{ auth.user?.points ?? '-' }}</span></div>
        <el-button type="danger" plain style="width: 100%; margin-top: 16px" @click="handleLogout">退出登录</el-button>
      </div>

      <div class="profile-section">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="我的预约" name="reservations">
            <div v-for="r in reservations" :key="r.id" class="res-item">
              <div class="res-left">
                <el-tag :type="statusType(r.status)" size="small">{{ statusLabel(r.status) }}</el-tag>
                <span>{{ r.reservation_date }} {{ r.reservation_time }} · {{ r.party_size }}人 · {{ r.table_code || '待分配' }}</span>
              </div>
              <el-button v-if="r.status === 'booked'" type="danger" plain size="small" @click="cancelReservation(r)">取消预约</el-button>
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

onMounted(async () => {
  const phone = auth.user?.mobileNumber || auth.user?.mobile_number
  if (phone) {
    try { reservations.value = await api.get<Reservation[]>('/reservations', { mobileNumber: phone }) }
    catch { /* empty */ }
  }
  try { orders.value = await api.get<Order[]>('/orders') }
  catch { /* empty */ }
})
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 320px 1fr; gap: 24px; }
@media (max-width: 768px) { .profile-grid { grid-template-columns: 1fr; } }
.profile-card { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; }
.profile-card h3 { font-size: 20px; margin-bottom: 16px; }
.field { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0eeea; font-size: 14px; }
.label { color: #667085; }
.profile-section { background: #fff; padding: 24px; border-radius: 12px; border: 1px solid #e8e5df; }
.res-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0eeea; }
.res-left { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0eeea; }
.order-meta { display: block; font-size: 13px; color: #667085; margin-top: 2px; }
</style>
