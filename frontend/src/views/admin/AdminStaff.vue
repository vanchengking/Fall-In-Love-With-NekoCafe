<template>
  <div class="page-enter-active">
    <div class="admin-toolbar">
      <h2>人员管理</h2>
      <div class="toolbar-right">
        <el-select v-model="roleFilter" placeholder="角色筛选" clearable class="role-filter-select" @change="loadStaff">
          <el-option label="全部" value="" />
          <el-option label="店员" value="staff" />
          <el-option label="店长" value="manager" />
          <el-option label="运营" value="operator" />
          <el-option label="猫咪管家" value="cat_keeper" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-button type="primary" @click="openDialog(null)"><Plus :size="14" class="btn-icon" />新增员工</el-button>
      </div>
    </div>

    <!-- 角色说明 -->
    <div class="role-info">
      <div v-for="(desc, role) in roleDescMap" :key="role" class="ri-item">
        <span class="ri-dot" :style="{ background: roleColor(role) }"></span>
        <strong>{{ roleMap[role] || role }}</strong>
        <span>{{ desc }}</span>
      </div>
    </div>

    <div class="staff-grid">
      <div v-for="member in staffList" :key="member.id" class="staff-card">
        <div class="staff-avatar" :style="{ background: roleColor(member.role) }">{{ roleIcon(member.role) }}</div>
        <div class="staff-info">
          <h4>{{ member.name }}</h4>
          <p>{{ roleLabel(member.role) }}</p>
          <p class="meta">{{ member.mobile_number }}</p>
        </div>
        <div class="staff-actions">
          <el-tag :type="member.role === 'admin' ? 'danger' : 'success'" size="small">{{ roleLabel(member.role) }}</el-tag>
          <div class="sa-btns">
            <el-button size="small" text @click="openDialog(member)"><Edit :size="14" /></el-button>
            <el-button size="small" text type="danger" @click="deleteStaff(member)"><Trash2 :size="14" /></el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!staffList.length && !loading" description="暂无员工数据" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑员工' : '新增员工'" width="440px" class="admin-dialog">
      <el-form ref="formRef" :model="form" :rules="staffRules" label-width="80px" label-position="right">
        <el-form-item label="姓名"><el-input v-model="form.name" placeholder="员工姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.mobile_number" placeholder="登录手机号" maxlength="11" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="r in assignableRoles" :key="r" :label="roleLabel(r)" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStaff">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Trash2 } from '@lucide/vue'
import { api } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'

interface StaffMember { id: number; name: string; mobile_number: string; role: string }

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'admin')
const isManager = computed(() => role.value === 'manager')

const staffList = ref<StaffMember[]>([])
const roleFilter = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: 0, name: '', mobile_number: '', role: 'staff' })
const staffRules = {
  name: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  mobile_number: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 店长只能分配 staff/cat_keeper，运营可分配所有角色
const assignableRoles = computed(() => {
  if (isManager.value) return ['staff', 'cat_keeper']
  return ['staff', 'manager', 'operator', 'cat_keeper', 'admin']
})

const roleMap: Record<string, string> = {
  staff: '店员', manager: '店长', operator: '总部运营',
  cat_keeper: '猫咪管家', admin: '管理员', customer: '顾客',
}
const roleDescMap: Record<string, string> = {
  staff: '订单处理、桌位调度、猫咪打卡',
  manager: '门店经营数据、人员排班',
  operator: '跨门店分析、活动投放',
  cat_keeper: '猫咪健康档案、互动记录',
  admin: '全部权限',
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

function openDialog(member: StaffMember | null) {
  if (member) { Object.assign(form, { id: member.id, name: member.name, mobile_number: member.mobile_number, role: member.role }) }
  else { Object.assign(form, { id: 0, name: '', mobile_number: '', role: 'staff' }) }
  dialogVisible.value = true
}

async function saveStaff() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  try {
    if (form.id) { await api.patch(`/users/${form.id}`, form); ElMessage.success('员工已更新') }
    else { await api.post('/users', form); ElMessage.success('员工已创建') }
    dialogVisible.value = false; await loadStaff()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { saving.value = false }
}

async function deleteStaff(member: StaffMember) {
  await ElMessageBox.confirm(`确认删除员工 ${member.name}？`, '删除员工', { type: 'warning' })
  try { await api.remove(`/users/${member.id}`); ElMessage.success('已删除'); await loadStaff() }
  catch (e) { ElMessage.error((e as Error).message) }
}

async function loadStaff() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (roleFilter.value) params.role = roleFilter.value
    else params.role = 'staff,manager,operator,cat_keeper,admin'
    staffList.value = await api.get<StaffMember[]>('/users', params)
  } catch {
    // API 不存在时使用 fallback 数据
    const { fallbackStaffList } = await import('@/utils/fallback')
    staffList.value = roleFilter.value
      ? fallbackStaffList.filter(u => u.role === roleFilter.value)
      : fallbackStaffList.filter(u => u.role !== 'customer')
  }
  finally { loading.value = false }
}

onMounted(loadStaff)
</script>

<style scoped>
.admin-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.admin-toolbar h2 { margin: 0; }
.toolbar-right { display: flex; gap: var(--space-sm); align-items: center; }
.role-filter-select { width: 140px; }
.btn-icon { margin-right: 4px; }

.role-info { display: flex; flex-wrap: wrap; gap: var(--space-base); margin-bottom: var(--space-lg); padding: var(--space-base); background: var(--wash); border-radius: var(--radius-md); }
.ri-item { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-xs); color: var(--muted); }
.ri-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ri-item strong { color: var(--ink); }

.staff-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: var(--space-base); }
.staff-card { display: flex; align-items: center; gap: var(--space-base); padding: var(--space-lg); background: var(--paper); border-radius: var(--radius-lg); border: 1px solid var(--line); transition: all var(--transition-fast); }
.staff-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
.staff-card:nth-child(even) { background: var(--wash); }
.staff-avatar { width: 48px; height: 48px; border-radius: var(--radius-md); display: grid; place-items: center; font-size: 22px; flex-shrink: 0; }
.staff-info { flex: 1; min-width: 0; }
.staff-info h4 { font-size: var(--text-base); margin-bottom: var(--space-xs); }
.staff-info p { font-size: var(--text-sm); color: var(--muted); }
.meta { font-size: var(--text-xs) !important; font-family: var(--font-mono); }
.staff-actions { display: flex; flex-direction: column; align-items: flex-end; gap: var(--space-xs); }
.sa-btns { display: flex; gap: 2px; }
@media (max-width: 640px) { .staff-grid { grid-template-columns: 1fr; } .staff-card { flex-wrap: wrap; } }
</style>
