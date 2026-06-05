<template>
  <div>
    <h2 style="margin-bottom: 20px">人员管理</h2>
    <div class="staff-grid">
      <div v-for="member in staffList" :key="member.phone" class="staff-card">
        <div class="staff-avatar" :style="{ background: member.color }">{{ member.icon }}</div>
        <div class="staff-info">
          <h4>{{ member.name }}</h4>
          <p>{{ member.roleName }}</p>
          <p class="meta">{{ member.phone }}</p>
        </div>
        <el-tag :type="member.tagType" size="small">{{ member.status }}</el-tag>
      </div>
    </div>

    <div class="panel" style="margin-top: 24px">
      <h3 style="margin-bottom: 14px">今日排班</h3>
      <el-table :data="scheduleData" stripe style="width: 100%">
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="role" label="岗位" width="120" />
        <el-table-column prop="shift" label="班次" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '在岗' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const staffList = ref([
  { name: '高店员', phone: '13800000002', roleName: '店员', icon: '🏪', color: '#e0f2fe', status: '在岗', tagType: 'success' as const },
  { name: '卢猫咪管家', phone: '13800000005', roleName: '猫咪管家', icon: '🐱', color: '#fce7f3', status: '在岗', tagType: 'success' as const },
])

const scheduleData = ref([
  { name: '高店员', role: '店员', shift: '09:00 - 18:00', status: '在岗' },
  { name: '卢猫咪管家', role: '猫咪管家', shift: '08:00 - 17:00', status: '在岗' },
  { name: '李店员', role: '店员', shift: '14:00 - 22:00', status: '休息' },
])
</script>

<style scoped>
.staff-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; margin-bottom: 24px; }
.staff-card { display: flex; align-items: center; gap: 14px; padding: 18px; background: #fff; border-radius: 12px; border: 1px solid #e8e5df; }
.staff-avatar { width: 48px; height: 48px; border-radius: 12px; display: grid; place-items: center; font-size: 22px; flex-shrink: 0; }
.staff-info h4 { font-size: 15px; margin-bottom: 2px; }
.staff-info p { font-size: 13px; color: #667085; }
.meta { font-size: 12px !important; font-family: monospace; }
.panel { background: #fff; padding: 20px; border-radius: 12px; border: 1px solid #e8e5df; }
</style>
