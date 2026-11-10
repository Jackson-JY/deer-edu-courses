<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const visible = computed({
  get: () => auth.loginVisible,
  set: (v) => (v ? null : auth.closeLogin()),
})

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')

const phone = ref('')
const password = ref('')
const password2 = ref('')
const nickname = ref('')
const loading = ref(false)

function reset() {
  phone.value = ''
  password.value = ''
  password2.value = ''
  nickname.value = ''
  mode.value = 'login'
}

function switchMode(m: Mode) {
  mode.value = m
  password.value = ''
  password2.value = ''
}

async function confirmLogin() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }
  if (mode.value === 'register') {
    if (!nickname.value.trim()) {
      ElMessage.warning('请输入昵称')
      return
    }
    if (password.value.length < 6) {
      ElMessage.warning('密码至少 6 位')
      return
    }
    if (password.value !== password2.value) {
      ElMessage.warning('两次密码不一致')
      return
    }
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(phone.value, password.value)
      ElMessage.success('登录成功')
    } else {
      await auth.register(phone.value, password.value, nickname.value.trim())
      ElMessage.success('注册成功，已自动登录')
    }
    auth.closeLogin()
    reset()
  } catch (e: any) {
    ElMessage.error(e?.message || (mode.value === 'login' ? '登录失败' : '注册失败'))
  } finally {
    loading.value = false
  }
}

watch(visible, (v) => {
  if (!v) reset()
})

function goForgotPassword() {
  auth.closeLogin()
  router.push('/forgot-password')
}
</script>

<template>
  <el-dialog v-model="visible" width="380px" align-center :show-close="true" class="login-dialog" title="">
    <div class="ld">
      <div class="ld-brand">
        <img class="ld-brand-logo" src="/logo.jpg" alt="Logo" />
        <span>Deer Edu</span>
      </div>

      <div class="ld-tabs">
        <button :class="['ld-tab', { active: mode === 'login' }]" @click="switchMode('login')">登录</button>
        <button :class="['ld-tab', { active: mode === 'register' }]" @click="switchMode('register')">注册</button>
      </div>

      <template v-if="mode === 'login'">
        <h3 class="ld-title">手机号登录</h3>
        <p class="ld-tip">使用手机号 + 密码登录</p>
      </template>
      <template v-else>
        <h3 class="ld-title">新用户注册</h3>
        <p class="ld-tip">注册后即可观看课程</p>
      </template>

      <input v-if="mode === 'register'" v-model="nickname" class="ld-native-input" placeholder="请输入昵称" maxlength="20" @keyup.enter="confirmLogin">

      <input v-model="phone" class="ld-native-input" placeholder="请输入手机号" maxlength="11" @keyup.enter="confirmLogin">

      <input v-model="password" type="password" class="ld-native-input" placeholder="密码（6-32 位）" maxlength="32" @keyup.enter="confirmLogin">

      <input v-if="mode === 'register'" v-model="password2" type="password" class="ld-native-input" placeholder="确认密码" maxlength="32" @keyup.enter="confirmLogin">

      <button class="ld-submit-btn" :disabled="loading" @click="confirmLogin">
        {{ loading ? '请稍候…' : (mode === 'login' ? '登录' : '注册并登录') }}
      </button>

      <p v-if="mode === 'login'" class="ld-forgot">
        <a @click="goForgotPassword">忘记密码？</a>
      </p>

      <p class="ld-agree">{{ mode === 'login' ? '登录' : '注册' }}即代表同意《用户协议》和《隐私政策》</p>
    </div>
  </el-dialog>
</template>

<style scoped>
.ld { text-align: center; padding: 4px 8px 8px; }
.ld-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 14px;
  color: var(--foreground);
}
.ld-brand-logo {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  object-fit: contain;
}
.ld-tabs {
  display: flex; border-bottom: 1px solid var(--border);
  margin-bottom: 18px;
}
.ld-tab {
  flex: 1; padding: 8px 0; background: transparent; border: none;
  cursor: pointer; font-size: 15px; color: var(--muted-foreground);
  border-bottom: 2px solid transparent; transition: all 0.2s;
}
.ld-tab.active {
  color: var(--primary); border-bottom-color: var(--primary); font-weight: 600;
}
.ld-title { font-size: 18px; margin-bottom: 10px; color: var(--foreground); }
.ld-tip { color: var(--muted-foreground); font-size: 13px; margin: 0 0 16px; }
.ld-input { margin-bottom: 12px; }
.ld-native-input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  margin-bottom: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  background: var(--background);
  color: var(--foreground);
  transition: border-color 0.18s, box-shadow 0.18s;
  box-sizing: border-box;
}
.ld-native-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 15%, transparent);
}
.ld-native-input::placeholder { color: var(--muted-foreground); }
.ld-submit-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 999px;
  background: var(--primary);
  color: var(--primary-foreground);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.18s, transform 0.1s;
  margin-top: 4px;
}
.ld-submit-btn:hover { opacity: 0.9; }
.ld-submit-btn:active { transform: scale(0.98); }
.ld-submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ld-agree { font-size: 12px; color: var(--muted-foreground); margin: 16px 0 0; }
.ld-forgot { margin: 12px 0 0; text-align: right; font-size: 13px; }
.ld-forgot a { color: var(--primary); cursor: pointer; text-decoration: none; }
.ld-forgot a:hover { text-decoration: underline; }
@media (max-width: 480px) {
  .ld { padding: 4px 4px 8px; }
}
</style>
