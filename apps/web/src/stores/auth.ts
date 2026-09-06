import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserProfile } from '@ielts/shared-types'
import { getToken, setToken, clearToken } from '@/api/client'
import { fetchProfile, loginByPhone } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const loginVisible = ref(false)
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!getToken() && !!user.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)

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

  async function login(phone: string, code: string) {
    const result = await loginByPhone(phone, code)
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
    openLogin,
    closeLogin,
    restore,
    login,
    logout,
    kick,
  }
})
