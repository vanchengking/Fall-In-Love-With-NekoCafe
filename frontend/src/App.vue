<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const AUTH_VERSION = 'v3'
const stored = localStorage.getItem('neko-auth-version')
if (stored !== AUTH_VERSION) {
  localStorage.removeItem('neko-auth')
  localStorage.setItem('neko-auth-version', AUTH_VERSION)
} else {
  auth.restore()
}
</script>

<style>
/* ── 路由过渡动画 ── */
.page-fade-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.page-fade-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-4px); }

/* ── 全局滚动条美化 ── */
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c4c9c4; border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: #a0a5a0; }
* { scrollbar-width: thin; scrollbar-color: #c4c9c4 transparent; }
</style>
