<template>
  <div>
    <h2 style="margin-bottom: 20px">活动配置</h2>
    <div class="campaign-list">
      <div v-for="c in campaigns" :key="c.id" class="campaign-card">
        <div class="campaign-header">
          <el-tag :type="c.active ? 'success' : 'info'" size="small">{{ c.active ? '进行中' : '已结束' }}</el-tag>
          <h4>{{ c.name }}</h4>
        </div>
        <p class="campaign-desc">{{ c.desc }}</p>
        <div class="campaign-meta">
          <span>{{ c.type }}</span>
          <span>{{ c.period }}</span>
        </div>
      </div>
    </div>

    <div class="panel" style="margin-top: 24px">
      <h3 style="margin-bottom: 14px">跨门店对比</h3>
      <el-table :data="storeCompare" stripe style="width: 100%">
        <el-table-column prop="store" label="门店" width="160" />
        <el-table-column prop="reservations" label="本周预约" />
        <el-table-column prop="revenue" label="本周收入" />
        <el-table-column prop="turnover" label="翻台率" />
        <el-table-column prop="rating" label="平均评分" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const campaigns = ref([
  { id: 1, name: '新客首单 8 折', type: '折扣', desc: '首次预约到店消费享全场 8 折优惠', period: '2026.06.01 - 2026.06.30', active: true },
  { id: 2, name: '周末满 100 减 20', type: '满减', desc: '周六日消费满 100 元立减 20 元', period: '2026.06.01 - 2026.08.31', active: true },
  { id: 3, name: '猫咪生日月', type: '活动', desc: '当月生日猫咪相关菜品 7 折', period: '2026.05.01 - 2026.05.31', active: false },
])

const storeCompare = ref([
  { store: 'NekoCafe 五道口店', reservations: 42, revenue: '¥8,600', turnover: 1.2, rating: 4.6 },
  { store: 'NekoCafe 西直门店', reservations: 28, revenue: '¥5,200', turnover: 0.9, rating: 4.3 },
])
</script>

<style scoped>
.campaign-list { display: flex; flex-direction: column; gap: 12px; }
.campaign-card { background: #fff; padding: 18px; border-radius: 12px; border: 1px solid #e8e5df; }
.campaign-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.campaign-header h4 { margin: 0; font-size: 16px; }
.campaign-desc { font-size: 14px; color: #667085; margin-bottom: 8px; }
.campaign-meta { display: flex; gap: 16px; font-size: 13px; color: #667085; }
.panel { background: #fff; padding: 20px; border-radius: 12px; border: 1px solid #e8e5df; }
</style>
