<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="brand-mark" style="width: 56px; height: 56px; font-size: 24px; margin: 0 auto 16px">N</div>
      <h2 style="text-align: center; margin-bottom: 4px">NekoCafe 智慧餐饮预约平台</h2>
      <p style="text-align: center; color: #667085; font-size: 14px; margin-bottom: 24px">短信验证码登录（沙箱环境）</p>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleSendCode">
        <el-form-item label="手机号" prop="mobileNumber">
          <el-input v-model="form.mobileNumber" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>

        <el-form-item v-if="codeSent" label="验证码" prop="code">
          <el-input v-model="form.code" placeholder="输入验证码" maxlength="6" />
        </el-form-item>

        <el-button v-if="!codeSent" type="primary" style="width: 100%" :loading="sending" @click="handleSendCode">
          发送验证码
        </el-button>
        <el-button v-else type="primary" style="width: 100%" :loading="logging" @click="handleLogin">
          登录
        </el-button>

        <el-button v-if="codeSent" style="width: 100%; margin-top: 4px" @click="codeSent = false">
          返回重输手机号
        </el-button>
      </el-form>

      <div class="demo-section">
        <div class="demo-header">
          <span class="demo-label">演示账号</span>
          <span class="demo-hint">沙箱验证码统一为 <strong>123456</strong></span>
        </div>
        <div class="demo-grid">
          <div v-for="account in demoAccounts" :key="account.phone" class="demo-item" @click="quickLogin(account)">
            <div class="demo-icon" :style="{ background: account.color }">{{ account.icon }}</div>
            <div class="demo-info">
              <div class="demo-role">{{ account.roleName }}</div>
              <div class="demo-phone">{{ account.phone }}</div>
              <div class="demo-desc">{{ account.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { api } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'
import { roleDefaultRoute } from '@/router'
import type { AuthResult } from '@/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const codeSent = ref(false)
const sending = ref(false)
const logging = ref(false)

const form = reactive({ mobileNumber: '13800000001', code: '' })

const demoAccounts = [
  { phone: '13800000001', roleName: '顾客', icon: '👤', color: '#e8f6f1', desc: '浏览门店、预约桌位、在线点单、个人中心' },
  { phone: '13800000002', roleName: '店员', icon: '🏪', color: '#e0f2fe', desc: '今日预约看板、入座/完桌操作、桌位状态' },
  { phone: '13800000003', roleName: '店长', icon: '📊', color: '#fef3c7', desc: '门店经营数据、数据看板、猫咪档案管理' },
  { phone: '13800000004', roleName: '总部运营', icon: '🏢', color: '#ede9fe', desc: '跨门店分析、全局数据看板' },
  { phone: '13800000005', roleName: '猫咪管家', icon: '🐱', color: '#fce7f3', desc: '猫咪健康档案、体重记录、疫苗管理' },
  { phone: '13800000006', roleName: '管理员', icon: '⚙️', color: '#f1f5f9', desc: '全部权限：数据看板 + 猫咪管理 + 门店管理' },
]

const rules: FormRules = {
  mobileNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
}

function quickLogin(account: { phone: string }) {
  form.mobileNumber = account.phone
  form.code = ''
  codeSent.value = true
}

async function handleSendCode() {
  if (!formRef.value) return
  await formRef.value.validateField('mobileNumber', async (valid) => {
    if (!valid) return
    sending.value = true
    try {
      await api.post('/auth/sms/send', { mobileNumber: form.mobileNumber })
      codeSent.value = true
      ElMessage.success('验证码已发送（沙箱固定：123456）')
    } catch (e) {
      ElMessage.error((e as Error).message)
    } finally {
      sending.value = false
    }
  })
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validateField('code', async (valid) => {
    if (!valid) return
    logging.value = true
    try {
      const result = await api.post<AuthResult>('/auth/login', {
        mobileNumber: form.mobileNumber,
        code: form.code,
      })
      // 优先读取 access_token，兼容旧的 token 字段
      const accessToken = result.access_token ?? result.token ?? null
      auth.token = accessToken
      auth.user = result.user
      localStorage.setItem('neko-auth', JSON.stringify({ token: accessToken, user: result.user }))
      ElMessage.success(`欢迎回来，${result.user.name || '用户'}`)
      const redirect = (route.query.redirect as string) || roleDefaultRoute[result.user.role] || '/'
      router.push(redirect)
    } catch (e) {
      ElMessage.error((e as Error).message)
    } finally {
      logging.value = false
    }
  })
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7f3, #e8f6f1);
}
.login-card {
  width: 100%;
  max-width: 480px;
  padding: 32px;
  border: 1px solid #e8e5df;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
.demo-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0eeea;
}
.demo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.demo-label { font-size: 14px; font-weight: 600; color: #172033; }
.demo-hint { font-size: 12px; color: #667085; }
.demo-grid { display: flex; flex-direction: column; gap: 8px; }
.demo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid #f0eeea;
}
.demo-item:hover { background: #f8f9fa; }
.demo-icon { width: 40px; height: 40px; border-radius: 10px; display: grid; place-items: center; font-size: 18px; flex-shrink: 0; }
.demo-info { flex: 1; min-width: 0; }
.demo-role { font-size: 14px; font-weight: 600; color: #172033; }
.demo-phone { font-size: 12px; color: #0f766e; font-family: monospace; }
.demo-desc { font-size: 12px; color: #667085; margin-top: 1px; }
</style>
