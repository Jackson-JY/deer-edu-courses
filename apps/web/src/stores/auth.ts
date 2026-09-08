import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserProfile } from '@ielts/shared-types'
import { getToken, setToken, clearToken } from '@/api/client'
import { fetchProfile, loginByPassword, registerByPhone } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const loginVisible = ref(false)
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!getToken() && !!user.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)
  const isTeacher = computed(() => user.value?.roles.includes('TEACHER') ?? false)

  function openLogin() {
    loginVisible.value = true
  }
  function closeLogin() {
    loginVisible.value = false
  }

  async function restore() {
    const token = getToken()
    if (!token) {
      initialized.value = true
      return
    }
    try {
      user.value = await fetchProfile()
    } catch {
      clearToken()
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(phone: string, password: string) {
    const result = await loginByPassword(phone, password)
    setToken(result.token)
    user.value = result.profile
    return result.profile
  }

  async function register(phone: string, password: string, nickname: string) {
    const result = await registerByPhone(phone, password, nickname)
    setToken(result.token)
    user.value = result.profile
    return result.profile
  }

  function logout() {
    clearToken()
    user.value = null
  }

  function kick() {
    clearToken()
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
