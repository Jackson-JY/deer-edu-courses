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
