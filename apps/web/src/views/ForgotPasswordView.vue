<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmPasswordReset, requestPasswordReset } from '@/api/auth'

const router = useRouter()

const step = ref<1 | 2>(1)
const phone = ref('')
const code = ref('')
const newPassword = ref('')
const newPassword2 = ref('')
const loading = ref(false)

async function submitRequest() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  loading.value = true
  try {
    await requestPasswordReset(phone.value)
    step.value = 2
    ElMessage.success('申请已提交，请联系老师获取重置码')
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    loading.value = false
  }
}

async function submitConfirm() {
  if (!/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入 6 位数字重置码')
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    ElMessage.warning('密码长度需为 6-32 位')
    return
  }
  if (newPassword.value !== newPassword2.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await confirmPasswordReset(phone.value, code.value, newPassword.value)
    ElMessage.success('密码已重置，请使用新密码登录')
    router.replace('/')
  } catch (e: any) {
    ElMessage.error(e?.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="forgot container">
    <div class="card">
      <h1 class="title">找回密码</h1>

      <template v-if="step === 1">
        <p class="tip">输入注册手机号提交申请，老师核实身份后会为你生成一次性重置码。</p>
        <el-input
          v-model="phone"
          placeholder="请输入注册手机号"
          maxlength="11"
          class="field"
          @keyup.enter="submitRequest"
        />
        <el-button type="primary" round class="submit" :loading="loading" @click="submitRequest">
          提交申请
        </el-button>
      </template>

      <template v-else>
        <p class="tip">
          申请已提交。请联系老师获取 6 位重置码（30 分钟内有效），再在下方设置新密码。
        </p>
        <el-input v-model="phone" class="field" disabled />
        <el-input
          v-model="code"
          placeholder="6 位重置码"
          maxlength="6"
          class="field"
          @keyup.enter="submitConfirm"
        />
        <el-input
          v-model="newPassword"
          type="password"
          show-password
          placeholder="新密码（6-32 位）"
          maxlength="32"
          class="field"
          @keyup.enter="submitConfirm"
        />
        <el-input
          v-model="newPassword2"
          type="password"
          show-password
          placeholder="确认新密码"
          maxlength="32"
          class="field"
          @keyup.enter="submitConfirm"
        />
        <el-button type="primary" round class="submit" :loading="loading" @click="submitConfirm">
          确认重置
        </el-button>
      </template>

      <p class="back">
        <router-link to="/">返回首页</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.forgot {
  max-width: 440px;
  padding: 48px 20px;
}
.card {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  padding: 28px 26px;
}
.title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 10px;
  letter-spacing: -0.01em;
}
.tip {
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted-foreground);
  margin: 0 0 18px;
}
.field { margin-bottom: 12px; }
.submit { width: 100%; margin-top: 4px; }
.back {
  margin: 16px 0 0;
  text-align: center;
  font-size: 13px;
}
.back a { color: var(--primary); text-decoration: none; }
</style>
