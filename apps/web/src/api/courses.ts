import { get } from './client'
import type { CourseCard, CourseDetail, Page, PlayCredential } from '@ielts/shared-types'
import { getToken, post } from './client'

export function fetchCourses(params?: Record<string, unknown>) {
  return get<Page<CourseCard>>('/api/courses', params)
}

export function fetchCourseDetail(id: number) {
  return get<CourseDetail>(`/api/courses/${id}`)
}

export function fetchPlayCredential(lessonId: number) {
  return post<PlayCredential>('/api/play/credential', { lessonId })
}

export function heartbeat(lessonId: number, progressSeconds: number, totalSeconds: number) {
  return post<{ alive: boolean; progressSeconds: number }>('/api/play/heartbeat', { lessonId, progressSeconds, totalSeconds })
}

/**
 * 页面卸载/切后台时用 keepalive fetch 兜底上报（sendBeacon 无法携带鉴权头）。
 * 浏览器保证页面关闭后请求仍能发出；失败静默，不影响播放。
 */
export function heartbeatBeacon(lessonId: number, progressSeconds: number, totalSeconds: number) {
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
    void fetch('/api/play/heartbeat', {
      method: 'POST',
      headers,
      body: JSON.stringify({ lessonId, progressSeconds, totalSeconds }),
      keepalive: true,
    }).catch(() => {})
  } catch {
    // 忽略：兜底上报失败不阻断任何流程
  }
}

export interface MaterialPreview {
  url: string
  name: string
  type: string
  previewOnly: boolean
  expireIn: number
}

export function fetchMaterialPreviewUrl(materialId: number) {
  return get<MaterialPreview>(`/api/materials/${materialId}/preview-url`)
}

export interface WatchProgress {
  lessonId: number
  courseId: number
  lessonTitle?: string
  progress: number
  lastPosition: number
  durationWatched: number
  finished: number
  updatedAt: string
}

export function fetchMyProgress() {
  return get<WatchProgress[]>('/api/play/progress')
}

export interface EnrolledCourse {
  id: number
  title: string
  cover: string
  targetScore: string
  lessonCount: number
  expireAt: string
}

export function fetchMyCourses() {
  return get<EnrolledCourse[]>('/api/my/courses')
}
