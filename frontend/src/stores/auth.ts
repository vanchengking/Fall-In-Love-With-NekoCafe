import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/utils/http'
import type { AuthUser } from '@/types'

const DEMO_LOGIN = { mobileNumber: '13800000006', code: '8888' }

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function restore() {
    try {
      const raw = localStorage.getItem('neko-auth')
      if (raw) {
        const parsed = JSON.parse(raw)
        token.value = parsed.token
        user.value = parsed.user
      }
    } catch {
      // ignore
    }
  }

  async function authenticate(): Promise<AuthUser | null> {
    try {
      await api.post('/auth/sms/send', { mobileNumber: DEMO_LOGIN.mobileNumber })
      const result = await api.post<{ token: string; user: AuthUser }>('/auth/login', DEMO_LOGIN)
      token.value = result.token
      user.value = result.user
      localStorage.setItem('neko-auth', JSON.stringify({ token: result.token, user: result.user }))
      return result.user
    } catch {
      token.value = null
      user.value = null
      return null
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('neko-auth')
  }

  return { token, user, isAuthenticated, isAdmin, restore, authenticate, logout }
})
