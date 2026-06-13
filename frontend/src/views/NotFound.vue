<template>
  <div style="text-align: center; padding: var(--space-2xl) var(--space-lg)">
    <h1 style="font-size: 64px; color: #667085">404</h1>
    <p style="margin: var(--space-sm) 0 var(--space-lg); color: #667085">页面不存在</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { roleDefaultRoute } from '@/router'

const router = useRouter()

function goHome() {
  try {
    const raw = localStorage.getItem('neko-auth')
    if (raw) {
      const { user } = JSON.parse(raw)
      if (user?.role && roleDefaultRoute[user.role]) {
        router.push(roleDefaultRoute[user.role])
        return
      }
    }
  } catch { /* ignore */ }
  router.push('/login')
}
</script>
