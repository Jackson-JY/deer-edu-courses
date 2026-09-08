<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const phone = ref('')
const password = ref('')
const loading = ref(false)

async function login() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    await auth.login(phone.value, password.value)
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
      <div class="brand">
        <img class="brand-logo" src="/logo.jpg" alt="Logo" />
        <span>
          <strong>Deer Edu</strong>
          <em>管理后台</em>
        </span>
      </div>
      <h2>管理员登录</h2>
      <p>请使用管理员手机号 + 密码登录（初始密码 admin123）</p>
      <el-input v-model="phone" placeholder="请输入手机号" maxlength="11" class="input">
        <template #prefix><el-icon><Iphone /></el-icon></template>
      </el-input>
      <el-input v-model="password" type="password" show-password placeholder="密码" maxlength="32" class="input" @keyup.enter="login">
        <template #prefix><el-icon><Lock /></el-icon></template>
      </el-input>
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
  padding: 1.5rem;
  background: linear-gradient(180deg, var(--background-50), var(--background-200));
}
.login-card {
  width: 380px;
  max-width: 100%;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 2.25rem;
  box-shadow: var(--shadow-xl);
}
.brand {
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-logo {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.brand strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}
.brand em {
  display: block;
  font-size: 12px;
  font-style: normal;
  color: var(--muted-foreground);
  font-weight: 500;
}
.login-card h2 { margin: 0 0 6px; font-size: 22px; font-weight: 700; letter-spacing: -0.01em; }
.login-card p { color: var(--muted-foreground); font-size: 13px; margin: 0 0 1.5rem; }
.input { margin-bottom: 14px; }
.submit { width: 100%; }
@media (max-width: 480px) {
  .login-card { padding: 1.5rem; }
}
</style>
