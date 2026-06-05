<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="$emit('submit')">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="姓名" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入姓名" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="手机号" prop="mobileNumber">
          <el-input v-model="form.mobileNumber" placeholder="请输入手机号" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="日期" prop="reservationDate">
          <el-date-picker v-model="form.reservationDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" @change="$emit('refreshTables')" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="时间" prop="reservationTime">
          <el-time-picker v-model="form.reservationTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" @change="$emit('refreshTables')" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="人数" prop="partySize">
          <el-input-number v-model="form.partySize" :min="1" :max="12" style="width: 100%" @change="$emit('refreshTables')" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="选择桌位（点击卡片选桌，不选则自动分配）">
      <div class="table-map">
        <div v-for="table in tables" :key="table.id"
          class="table-seat" :class="{ selected: form.tableId === table.id, occupied: table.available_for_slot === false }"
          @click="table.available_for_slot !== false && selectTable(table.id)">
          <div class="seat-code">{{ table.code }}</div>
          <div class="seat-info">{{ table.seats }}人 · {{ table.area }}</div>
          <el-tag v-if="table.cat_zone" size="small" type="success" style="margin-top: 4px">猫区</el-tag>
          <div v-if="table.available_for_slot === false" class="seat-occupied">已占用</div>
        </div>
        <div v-if="!tables.length" class="table-empty">暂无可用桌位</div>
      </div>
      <div v-if="form.tableId" class="selected-hint">
        已选桌位：<strong>{{ tables.find(t => t.id === form.tableId)?.code }}</strong>
        <el-button text type="primary" size="small" @click="form.tableId = ''">取消选择（自动分配）</el-button>
      </div>
    </el-form-item>

    <el-form-item label="备注">
      <el-input v-model="form.note" type="textarea" :rows="3" placeholder="特殊需求" />
    </el-form-item>
    <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
      <CheckCircle2 :size="18" style="margin-right: 6px" />
      提交预约
    </el-button>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CheckCircle2 } from '@lucide/vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { DiningTable, ReservationForm } from '@/types'

const formRef = ref<FormInstance>()

const props = defineProps<{
  form: ReservationForm
  tables: DiningTable[]
  loading: boolean
}>()

defineEmits<{
  submit: []
  refreshTables: []
}>()

function selectTable(id: number) {
  props.form.tableId = props.form.tableId === id ? '' : id
}

const rules: FormRules = {
  customerName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobileNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  reservationDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  reservationTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  partySize: [{ required: true, message: '请选择人数', trigger: 'change' }],
}

defineExpose({ formRef })
</script>

<style scoped>
.table-map {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 10px;
  width: 100%;
}
.table-seat {
  padding: 14px;
  border: 2px solid #e8e5df;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
  position: relative;
}
.table-seat:hover:not(.occupied) {
  border-color: #0f766e;
  background: #f0fdfa;
}
.table-seat.selected {
  border-color: #0f766e;
  background: #e8f6f1;
  box-shadow: 0 0 0 2px rgba(15, 118, 110, 0.2);
}
.table-seat.occupied {
  background: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.6;
}
.seat-code { font-size: 18px; font-weight: 700; }
.seat-info { font-size: 12px; color: #667085; margin-top: 2px; }
.seat-occupied { font-size: 11px; color: #ef4444; margin-top: 4px; }
.table-empty { grid-column: 1 / -1; text-align: center; color: #667085; padding: 20px; }
.selected-hint { margin-top: 8px; font-size: 13px; color: #0f766e; display: flex; align-items: center; gap: 8px; }
</style>
