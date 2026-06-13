import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/utils/http'
import type { AuthResult, AuthUser } from '@/types'

const DEMO_LOGIN = { mobileNumber: '13800000006', code: '123456' }

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
        token.value = parsed.token || null
        user.value = parsed.user || null
      }
    } catch {
      token.value = null
      user.value = null
    }
  }

  async function authenticate(): Promise<AuthUser | null> {
    try {
      await api.post('/auth/sms/send', { mobileNumber: DEMO_LOGIN.mobileNumber })
      const result = await api.post<AuthResult>('/auth/login', DEMO_LOGIN)
      if (!result?.user) return null
      const accessToken = result.access_token ?? result.token ?? null
      token.value = accessToken
      user.value = result.user
      localStorage.setItem('neko-auth', JSON.stringify({ token: accessToken, user: result.user }))
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
