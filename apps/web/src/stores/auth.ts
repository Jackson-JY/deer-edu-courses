import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserProfile } from '@ielts/shared-types'
import { getToken, setToken as persistToken, clearToken as clearPersistedToken } from '@/api/client'
import { fetchProfile, loginByPassword, registerByPhone } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // token 同时存放在 localStorage（持久化）和响应式 ref（驱动 UI）。
  // isLoggedIn 不能直接读 localStorage——它是非响应式的，且 && 短路会让
  // 未登录首屏时 user 依赖不被收集，导致登录后顶栏不更新（需刷新才正常）。
  const token = ref<string | null>(getToken())
  const user = ref<UserProfile | null>(null)
  const loginVisible = ref(false)
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)
  const isTeacher = computed(() => user.value?.roles.includes('TEACHER') ?? false)

  function openLogin() {
    loginVisible.value = true
  }
  function closeLogin() {
    loginVisible.value = false
  }

  /** 同步更新响应式 token 与 localStorage 持久化 */
  function setSession(next: string | null) {
    token.value = next
    if (next) persistToken(next)
    else clearPersistedToken()
  }

  async function restore() {
    if (!token.value) {
      initialized.value = true
      return
    }
    try {
      user.value = await fetchProfile()
    } catch {
      setSession(null)
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(phone: string, password: string) {
    const result = await loginByPassword(phone, password)
    setSession(result.token)
    user.value = result.profile
    return result.profile
  }

  async function register(phone: string, password: string, nickname: string) {
    const result = await registerByPhone(phone, password, nickname)
    setSession(result.token)
    user.value = result.profile
    return result.profile
  }

  function logout() {
    setSession(null)
    user.value = null
  }

  function kick() {
    setSession(null)
    user.value = null
    openLogin()
  }

  return {
    user,
    loginVisible,
    initialized,
    isLoggedIn,
    isAdmin,
    isTeacher,
    openLogin,
    closeLogin,
    restore,
    login,
    register,
    logout,
    kick,
  }
})
