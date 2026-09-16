import { get, post, put, del } from './client'

export type WritingStatus =
  | 'PENDING'
  | 'AI_DRAFTING'
  | 'AWAITING_REVIEW'
  | 'CORRECTED'
  | 'FAILED'

export type AnnotationCategory = 'G' | 'V' | 'C' | 'T'

export interface WritingAnnotation {
  id?: number
  seq?: number
  category: AnnotationCategory
  startPos: number
  endPos: number
  quote: string
  note: string
  suggestion: string
}

export interface ModelIdeasColumn {
  heading: string
  points: string[]
}

export interface WritingCorrection {
  id: number
  submissionId: number
  correctorId: number | null
  scores: {
    tr: number | null
    cc: number | null
    lr: number | null
    ga: number | null
    overall: number | null
  }
  comments: {
    tr: string | null
    cc: string | null
    lr: string | null
    ga: string | null
  }
  closingRemark: string | null
  modelIdeas: ModelIdeasColumn[]
  modelEssay: string | null
  status: 'DRAFT' | 'PUBLISHED'
  createdAt: string
  annotations: WritingAnnotation[]
}

export interface WritingSubmissionDetail {
  id: number
  userId: number
  taskPrompt: string
  content: string
  wordCount: number
  status: WritingStatus
  aiError: string | null
  createdAt: string
  correction?: WritingCorrection
}

export interface WritingSubmissionRow {
  id: number
  wordCount: number
  status: WritingStatus
  createdAt: string
  promptPreview: string
  essayPreview: string
  overall: number | null
}

export interface TeacherSubmissionRow extends WritingSubmissionRow {
  userId: number
  studentName: string
}

export interface TeacherSubmissionDetail extends WritingSubmissionDetail {
  studentName: string
}

// ===== 学生端 =====

export interface WritingQuota {
  granted: boolean
  expireAt: string | null
  used: number
  limit: number
  remaining: number
}

export function getWritingQuota() {
  return get<WritingQuota>('/api/writing/quota')
}

export function submitWriting(data: { taskPrompt: string; content: string }) {
  return post<WritingSubmissionDetail>('/api/writing/submissions', data)
}

export function listMyWriting() {
  return get<WritingSubmissionRow[]>('/api/writing/submissions/mine')
}

export function getWritingSubmission(id: number | string) {
  return get<WritingSubmissionDetail>(`/api/writing/submissions/${id}`)
}

// ===== 教师端 =====

export function listTeacherSubmissions(status?: string) {
  return get<TeacherSubmissionRow[]>('/api/writing/teacher/submissions', status ? { status } : undefined)
}

export function getTeacherSubmission(id: number | string) {
  return get<TeacherSubmissionDetail>(`/api/writing/teacher/submissions/${id}`)
}

/** 保存编辑草稿所需的请求体 */
export interface CorrectionSavePayload {
  scores: { tr: number; cc: number; lr: number; ga: number }
  comments: { tr: string; cc: string; lr: string; ga: string }
  closingRemark: string
  modelIdeas: ModelIdeasColumn[]
  modelEssay: string
  annotations: WritingAnnotation[]
}

export function saveCorrection(id: number | string, payload: CorrectionSavePayload) {
  return put<WritingCorrection>(`/api/writing/teacher/corrections/${id}`, payload)
}

export function publishCorrection(id: number | string) {
  return post<WritingCorrection>(`/api/writing/teacher/corrections/${id}/publish`)
}

export function regenerateDraft(id: number | string) {
  return post<void>(`/api/writing/teacher/submissions/${id}/regenerate`)
}

export function createBlankCorrection(id: number | string) {
  return post<WritingCorrection>(`/api/writing/teacher/submissions/${id}/correction`)
}

// ===== 管理员：写作权限 =====

export interface WritingGrantRow {
  userId: number
  nickname?: string
  phone?: string
  expireAt: string | null
  active: boolean
  used?: number
  limit?: number
}

export function listWritingGrants() {
  return get<WritingGrantRow[]>('/api/admin/writing/grants')
}

export function grantWriting(userId: number, expireAt?: string | null) {
  return post<{ userId: number; expireAt: string | null }>(
    `/api/admin/writing/users/${userId}/grant`,
    { expireAt: expireAt ?? null },
  )
}

export function revokeWriting(userId: number) {
  return del<void>(`/api/admin/writing/users/${userId}/grant`)
}
