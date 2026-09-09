import { get, post, postUpload, put, del } from './client'

export interface AdminCourse {
  id?: number
  title: string
  cover: string
  intro?: string
  teacherId?: number
  price: number
  originalPrice?: number
  validityType: 'PERMANENT' | 'DAYS' | 'FIXED'
  validityDays?: number
  fixedExpireAt?: string
  inSubscription: boolean
  targetScore?: string
  status: string
  sort?: number
}

export interface AdminChapter {
  id?: number
  title: string
  sort?: number
  lessons?: AdminLesson[]
}

export interface AdminLesson {
  id?: number
  title: string
  vodFileId?: string
  duration?: number
  freeTrial: boolean
  sort?: number
}

export interface AdminMaterial {
  id: number
  courseId: number
  lessonId?: number
  name: string
  type: string
  cosKey: string
  previewOnly: boolean
}

export function listAdminCourses() {
  return get<AdminCourse[]>('/api/admin/courses')
}

export function createAdminCourse(data: AdminCourse) {
  return post<AdminCourse>('/api/admin/courses', data)
}

export function updateAdminCourse(id: number, data: AdminCourse) {
  return put<AdminCourse>(`/api/admin/courses/${id}`, data)
}

export function getAdminCourse(id: number) {
  return get<AdminCourse & { chapters: AdminChapter[]; materials: AdminMaterial[] }>(`/api/admin/courses/${id}`)
}

export function deleteAdminCourse(id: number) {
  return del<void>(`/api/admin/courses/${id}`)
}

export function publishCourse(id: number) {
  return post<AdminCourse>(`/api/admin/courses/${id}/publish`)
}

export function offlineCourse(id: number) {
  return post<AdminCourse>(`/api/admin/courses/${id}/offline`)
}

export function createChapter(courseId: number, data: { title: string; sort?: number }) {
  return post<AdminChapter>(`/api/admin/courses/${courseId}/chapters`, data)
}

export function updateChapter(courseId: number, chapterId: number, data: { title: string; sort?: number }) {
  return put<AdminChapter>(`/api/admin/courses/${courseId}/chapters/${chapterId}`, data)
}

export function deleteChapter(courseId: number, chapterId: number) {
  return del<void>(`/api/admin/courses/${courseId}/chapters/${chapterId}`)
}

export function createLesson(courseId: number, chapterId: number, data: AdminLesson) {
  return post<AdminLesson>(`/api/admin/courses/${courseId}/chapters/${chapterId}/lessons`, data)
}

export function updateLesson(courseId: number, lessonId: number, data: AdminLesson) {
  return put<AdminLesson>(`/api/admin/courses/${courseId}/lessons/${lessonId}`, data)
}

export function deleteLesson(courseId: number, lessonId: number) {
  return del<void>(`/api/admin/courses/${courseId}/lessons/${lessonId}`)
}

export interface AdminUser {
  id: number
  phone: string
  nickname: string
  status: number
  roles: string[]
}

export function listAdminUsers(phone?: string) {
  return get<AdminUser[]>('/api/admin/users', phone ? { phone } : undefined)
}

export function assignUserRole(userId: number, role: 'STUDENT' | 'TEACHER' | 'ADMIN') {
  return post<{ userId: number; role: string }>(`/api/admin/users/${userId}/role`, { role })
}

export function grantEntitlement(userId: number, data: { courseId: number; source?: string; expireAt?: string }) {
  return post<{ id: number; userId: number; courseId: number; status: string }>(`/api/admin/users/${userId}/entitlements`, data)
}

export function getSecurityConfig() {
  return get<{ watermarkTemplate: string; credentialTtl: number; previewTtl: number }>('/api/admin/security')
}

export function saveSecurityConfig(data: { watermarkTemplate: string; credentialTtl: number; previewTtl: number }) {
  return put<{ watermarkTemplate: string; credentialTtl: number; previewTtl: number }>('/api/admin/security', data)
}

export function getDashboardStats() {
  return get<{
    courseCount: number
    onlineCourseCount: number
    lessonCount: number
    userCount: number
    activeEntitlementCount: number
    activeUserCount: number
    totalWatchMinutes: number
    finishedLessonCount: number
    activeLearnerCount: number
    courseRanking: { courseId: number; title: string; learners: number; watchMinutes: number }[]
  }>('/api/admin/dashboard')
}

export function getVodUploadSign() {
  return get<{ appId: number; subAppId: number; secretId: string; signature: string; expireTime: number; configured: boolean }>('/api/admin/vod/upload-sign')
}

export function listMaterials(courseId: number) {
  return get<AdminMaterial[]>(`/api/admin/courses/${courseId}/materials`)
}

export function createMaterial(courseId: number, data: { name: string; type: string; cosKey: string; lessonId?: number; previewOnly?: boolean }) {
  return post<AdminMaterial>(`/api/admin/courses/${courseId}/materials`, data)
}

export function deleteMaterial(courseId: number, id: number) {
  return del<void>(`/api/admin/courses/${courseId}/materials/${id}`)
}

/** 上传课程封面图到 COS，返回可访问 URL */
export function uploadCover(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return postUpload<{ url: string; key: string }>('/api/admin/upload/cover', fd)
}

/** 上传课件文件到 COS 并自动登记为课程课件 */
export function uploadMaterialFile(courseId: number, file: File, lessonId?: number) {
  const fd = new FormData()
  fd.append('file', file)
  if (lessonId) fd.append('lessonId', String(lessonId))
  return postUpload<AdminMaterial>(`/api/admin/courses/${courseId}/materials/upload`, fd)
}
