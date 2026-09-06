import { get, post } from './client'
import type { UserProfile } from '@ielts/shared-types'

export interface LoginResult {
  token: string
  profile: UserProfile
}

export function sendSmsCode(phone: string) {
  return post<{ message: string; mockCode: string }>('/api/auth/phone/send-code', { phone })
}

export function loginByPhone(phone: string, code: string) {
  return post<LoginResult>('/api/auth/phone/login', { phone, code })
}

export function logout() {
  return post<void>('/api/auth/logout')
}

export function fetchProfile() {
  return get<UserProfile>('/api/auth/me/profile')
}
