import { get } from './client'
import type { CourseCard, CourseDetail, Page, PlayCredential } from '@ielts/shared-types'
import { post } from './client'

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
