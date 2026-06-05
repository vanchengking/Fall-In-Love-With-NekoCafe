<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
      <h2>人员管理</h2>
      <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width: 160px" @change="loadStaff">
        <el-option label="全部" value="" />
        <el-option label="店员" value="staff" />
        <el-option label="店长" value="manager" />
        <el-option label="运营" value="operator" />
        <el-option label="猫咪管家" value="cat_keeper" />
        <el-option label="管理员" value="admin" />
      </el-select>
    </div>

    <div class="staff-grid">
      <div v-for="member in staffList" :key="member.id" class="staff-card">
        <div class="staff-avatar" :style="{ background: roleColor(member.role) }">{{ roleIcon(member.role) }}</div>
        <div class="staff-info">
          <h4>{{ member.name }}</h4>
          <p>{{ roleLabel(member.role) }}</p>
          <p class="meta">{{ member.mobile_number }}</p>
        </div>
        <el-tag :type="member.role === 'admin' ? 'danger' : 'success'" size="small">
          {{ roleLabel(member.role) }}
        </el-tag>
      </div>
    </div>

    <div v-if="staffList.length === 0 && !loading" style="text-align: center; padding: 40px; color: #999">
      暂无员工数据
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/http'

interface StaffMember {
  id: number
  name: string
  mobile_number: string
  role: string
  member_level: string
  points: number
}

const staffList = ref<StaffMember[]>([])
const roleFilter = ref('')
const loading = ref(false)

const roleMap: Record<string, string> = {
  staff: '店员', manager: '店长', operator: '总部运营',
  cat_keeper: '猫咪管家', admin: '管理员', customer: '顾客',
}

const roleIconMap: Record<string, string> = {
  staff: '🏪', manager: '👩‍💼', operator: '📊',
  cat_keeper: '🐱', admin: '⚙️', customer: '👤',
}

const roleColorMap: Record<string, string> = {
  staff: '#e0f2fe', manager: '#fef3c7', operator: '#ede9fe',
  cat_keeper: '#fce7f3', admin: '#fee2e2', customer: '#f0fdf4',
}

function roleLabel(r: string) { return roleMap[r] || r }
function roleIcon(r: string) { return roleIconMap[r] || '👤' }
function roleColor(r: string) { return roleColorMap[r] || '#f3f4f6' }

async function loadStaff() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (roleFilter.value) params.role = roleFilter.value
    else params.role = 'staff,manager,operator,cat_keeper,admin'
    staffList.value = await api.get<StaffMember[]>('/users', params)
  } catch {
    staffList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadStaff)
</script>

<style scoped>
.staff-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; margin-bottom: 24px; }
.staff-card { display: flex; align-items: center; gap: 14px; padding: 18px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.staff-avatar { width: 48px; height: 48px; border-radius: 12px; display: grid; place-items: center; font-size: 22px; flex-shrink: 0; }
.staff-info h4 { font-size: 15px; margin-bottom: 2px; }
.staff-info p { font-size: 13px; color: #667085; }
.meta { font-size: 12px !important; font-family: monospace; }
</style>
