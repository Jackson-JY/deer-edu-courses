import { get, post } from './client'
import type { UserProfile } from '@ielts/shared-types'

export interface LoginResult {
  token: string
  profile: UserProfile
}

/** 密码登录 */
export function loginByPassword(phone: string, password: string) {
  return post<LoginResult>('/api/auth/login', { phone, password })
}

/** 用户注册：手机号 + 密码 + 昵称 */
export function registerByPhone(phone: string, password: string, nickname: string) {
  return post<LoginResult>('/api/auth/register', { phone, password, nickname })
}

export function logout() {
  return post<void>('/api/auth/logout')
}

export function fetchProfile() {
  return get<UserProfile>('/api/auth/me/profile')
}
