<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { sendSmsCode } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

const phone = ref('')
const code = ref('')
const counting = ref(0)
const loading = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

async function sendCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    const res = await sendSmsCode(phone.value)
    ElMessage.success(res.mockCode ? `验证码已发送：${res.mockCode}` : '验证码已发送')
    counting.value = 60
    timer && clearInterval(timer)
    timer = setInterval(() => {
      counting.value--
      if (counting.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e: any) {
    ElMessage.error(e?.message || '发送失败')
  }
}

async function login() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  loading.value = true
  try {
    await auth.login(phone.value, code.value)
    if (!auth.isAdmin) {
      ElMessage.error('该账号没有管理员权限')
      auth.logout()
      return
    }
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login">
    <div class="login-card">
      <div class="brand"><span class="mark">IELTS</span> 管理后台</div>
      <h2>管理员登录</h2>
      <p>请使用管理员手机号 + 验证码登录</p>
      <el-input v-model="phone" placeholder="请输入手机号" maxlength="11" class="input">
        <template #prefix><el-icon><Iphone /></el-icon></template>
      </el-input>
      <div class="code-row">
        <el-input v-model="code" placeholder="验证码" maxlength="6" @keyup.enter="login" />
        <el-button :disabled="counting > 0" @click="sendCode">
          {{ counting > 0 ? `${counting}s` : '获取验证码' }}
        </el-button>
      </div>
      <el-button type="primary" round class="submit" :loading="loading" @click="login">登录</el-button>
    </div>
  </div>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f3c69, #1f6fc4);
}
.login-card {
  width: 380px;
  background: #fff;
  border-radius: var(--r-lg);
  padding: 36px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
}
.brand {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 24px;
}
.brand .mark {
  font-size: 12px;
  color: #fff;
  background: var(--brand-500);
  padding: 3px 7px;
  border-radius: var(--r-sm);
  margin-right: 6px;
}
.login-card h2 { margin: 0 0 8px; font-size: 22px; }
.login-card p { color: var(--text-3); font-size: 13px; margin: 0 0 24px; }
.input { margin-bottom: 14px; }
.code-row { display: flex; gap: 10px; margin-bottom: 24px; }
.code-row .el-input { flex: 1; }
.submit { width: 100%; }
</style>
