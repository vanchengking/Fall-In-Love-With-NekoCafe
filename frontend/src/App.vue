<template>
  <router-view />
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
