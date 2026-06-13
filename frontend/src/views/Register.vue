<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="brand-mark" style="width: 56px; height: 56px; font-size: 24px; margin: 0 auto 16px">N</div>
      <h2 style="text-align: center; margin-bottom: 4px">注册 NekoCafé</h2>
      <p style="text-align: center; color: #667085; font-size: 14px; margin-bottom: 24px">
        注册后自动登录，开始您的猫咪咖啡之旅
      </p>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
        <!-- 步骤1：填信息 + 发送验证码 -->
        <template v-if="!codeSent">
          <el-form-item label="手机号" prop="mobileNumber">
            <el-input v-model="form.mobileNumber" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>

          <el-form-item label="用户昵称" prop="name">
            <el-input v-model="form.name" placeholder="请输入您的昵称" maxlength="20" />
          </el-form-item>

          <el-form-item label="用餐偏好（可选）">
            <el-checkbox-group v-model="form.preferences">
              <el-checkbox v-for="pp in presetPrefs" :key="pp" :label="pp" style="margin-right: 12px" />
            </el-checkbox-group>

            <!-- 已选偏好标签 -->
            <div v-if="form.preferences.length" style="margin: var(--space-sm) 0 var(--space-xs); display: flex; flex-wrap: wrap; gap: 6px">
              <el-tag
                v-for="(p, idx) in form.preferences"
                :key="idx"
                :closable="!presetPrefs.includes(p)"
                @close="removePref(idx)"
                type="primary"
                size="small"
              >{{ p }}</el-tag>
            </div>
            <div v-else style="color: #999; font-size: var(--text-xs); margin: var(--space-xs) 0">还未选择任何偏好</div>

            <el-input
              v-model="customPref"
              placeholder="自定义偏好，如：过生日"
              size="small"
              style="width: 100%; margin-top: 8px"
              @keyup.enter="addCustomPref"
            >
              <template #append>
                <el-button :icon="Plus" @click="addCustomPref">添加</el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="primary" style="width: 100%" :loading="sending" @click="handleSendCode">
            发送验证码
          </el-button>
        </template>

        <!-- 步骤2：输入验证码 + 完成注册 -->
        <template v-else>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="form.code" placeholder="输入验证码（沙箱固定：8888）" maxlength="4" />
          </el-form-item>

          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
            <template #default>
              即将为 <strong>{{ form.mobileNumber }}</strong> 注册账号<br>
              用户昵称：{{ form.name }}
            </template>
          </el-alert>

          <el-button type="primary" style="width: 100%" :loading="registering" @click="handleRegister">
            注册并登录
          </el-button>

          <el-button style="width: 100%; margin-top: 8px" @click="handleBack">
            返回修改
          </el-button>
        </template>
      </el-form>

      <div style="text-align: center; margin-top: 16px; font-size: 13px; color: #667085">
        已有账号？
        <el-link type="primary" @click="goLogin">立即登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '@/utils/http'

const router = useRouter()
const formRef = ref<FormInstance>()

const codeSent = ref(false)
const sending = ref(false)
const registering = ref(false)
const customPref = ref('')

const presetPrefs = [
  '喜欢靠窗座位',
  '喜欢安静角落',
  '对猫毛过敏',
  '不吃辣',
  '喜欢互动多的猫咪',
  '带小孩',
  '过生日',
  '需要儿童座椅',
  '需要充电插座',
  '素食优先',
]

const form = reactive({
  mobileNumber: '',
  code: '',
  name: '',
  preferences: [] as string[],
})

const rules: FormRules = {
  mobileNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 20, message: '昵称长度 1-20 个字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位', trigger: 'blur' },
  ],
}

function addCustomPref() {
  const val = customPref.value.trim()
  if (!val) return
  if (form.preferences.includes(val)) {
    ElMessage.warning('该偏好已添加')
    return
  }
  form.preferences.push(val)
  customPref.value = ''
}

function removePref(idx: number) {
  form.preferences.splice(idx, 1)
}

async function handleSendCode() {
  if (!formRef.value) return
  await formRef.value.validateField('mobileNumber', async (valid) => {
    if (!valid) return
    await formRef.value!.validateField('name', async (valid2) => {
      if (!valid2) return
      sending.value = true
      try {
        await api.post('/auth/sms/send', { mobileNumber: form.mobileNumber })
        codeSent.value = true
        ElMessage.success('验证码已发送（沙箱固定：8888）')
      } catch (e: any) {
        ElMessage.error(e.message || '发送失败')
      } finally {
        sending.value = false
      }
    })
  })
}

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    registering.value = true
    try {
      const result = await api.post<{ token: string; user: any }>('/auth/login', {
        mobileNumber: form.mobileNumber,
        code: form.code,
        name: form.name,
        preferences: form.preferences,
      })
      localStorage.setItem('neko-auth', JSON.stringify({ token: result.token, user: result.user }))
      ElMessage.success(`注册成功，欢迎你，${form.name}！`)
      router.push('/')
    } catch (e: any) {
      ElMessage.error(e.message || '注册失败')
    } finally {
      registering.value = false
    }
  })
}

function handleBack() {
  codeSent.value = false
  form.code = ''
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: var(--space-lg);
  background: linear-gradient(135deg, #f5f7f3, #e8f6f1);
}
.login-card {
  width: 100%;
  max-width: 480px;
  padding: var(--space-xl);
  border: 1px solid #e8e5df;
  border-radius: var(--radius-lg);
  background: #fff;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
</style>
