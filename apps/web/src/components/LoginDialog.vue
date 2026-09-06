<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { sendSmsCode } from '@/api/auth'

const auth = useAuthStore()
const visible = computed({
  get: () => auth.loginVisible,
  set: (v) => (v ? null : auth.closeLogin()),
})

const phone = ref('')
const code = ref('')
const counting = ref(0)
const loading = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

function reset() {
  phone.value = ''
  code.value = ''
  counting.value = 0
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function sendCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    const res = await sendSmsCode(phone.value)
    ElMessage.success(res.mockCode ? `验证码已发送：${res.mockCode}` : res.message)
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

async function confirmLogin() {
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
    ElMessage.success('登录成功')
    auth.closeLogin()
    reset()
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

watch(visible, (v) => {
  if (!v) reset()
})
</script>

<template>
  <el-dialog v-model="visible" width="380px" align-center :show-close="true" class="login-dialog" title="">
    <div class="ld">
      <div class="ld-brand"><span class="mark">IELTS</span> 雅思学堂</div>

      <h3 class="ld-title">手机号登录</h3>
      <p class="ld-tip">首次登录将自动创建账号</p>
      <el-input v-model="phone" placeholder="请输入手机号" maxlength="11" class="ld-input">
        <template #prefix><el-icon><Iphone /></el-icon></template>
      </el-input>
      <div class="ld-code">
        <el-input v-model="code" placeholder="验证码" maxlength="6" @keyup.enter="confirmLogin" />
        <el-button :disabled="counting > 0" @click="sendCode">
          {{ counting > 0 ? `${counting}s` : '获取验证码' }}
        </el-button>
      </div>
      <el-button type="primary" round class="ld-submit" :loading="loading" @click="confirmLogin">登录 / 注册</el-button>

      <p class="ld-agree">登录即代表同意《用户协议》和《隐私政策》</p>
    </div>
  </el-dialog>
</template>

<style scoped>
.ld { text-align: center; padding: 4px 8px 8px; }
.ld-brand { font-weight: 700; font-size: 16px; margin-bottom: 18px; }
.ld-brand .mark {
  font-size: 12px; color: #fff; background: var(--brand-500);
  padding: 3px 7px; border-radius: var(--r-sm); margin-right: 4px;
}
.ld-title { font-size: 18px; margin-bottom: 10px; }
.ld-tip { color: var(--text-3); font-size: 13px; margin: 0 0 16px; }
.ld-input { margin-bottom: 12px; }
.ld-code { display: flex; gap: 10px; margin-bottom: 16px; }
.ld-code .el-input { flex: 1; }
.ld-submit { width: 100%; }
.ld-agree { font-size: 12px; color: var(--text-3); margin: 16px 0 0; }
</style>
