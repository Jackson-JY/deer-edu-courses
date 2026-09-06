// 前后端共享类型契约，对应 docs/openapi.yaml
// 金额单位：分（number, integer）；时间：ISO8601 字符串

export interface ApiResult<T = unknown> {
  code: number // 0 = 成功
  message: string
  data: T
}

export interface ApiError {
  code: number
  message: string
  bizCode?: BizCode
}

export type BizCode =
  | 'SESSION_KICKED'
  | 'NO_ENTITLEMENT'
  | 'COURSE_EXPIRED'
  | 'COUPON_USED'
  | 'NEED_BIND_PHONE'

export interface PageMeta {
  page: number
  size: number
  total: number
}

export interface Page<T> {
  list: T[]
  meta: PageMeta
}

// ---------- catalog ----------
export interface CourseCard {
  id: number
  title: string
  cover: string
  teacherName: string
  targetScore: string
  lessonCount: number
  price: number
  originalPrice: number
  inSubscription: boolean
  sales: number
}

export type ValidityType = 'PERMANENT' | 'DAYS' | 'FIXED_DATE'
export type CtaState = 'GUEST_BUY' | 'BUY' | 'SUBSCRIBE' | 'LEARN' | 'EXPIRED'

export interface LessonBrief {
  id: number
  title: string
  duration: number
  freeTrial: boolean
  locked: boolean
  progress: number
}

export interface Chapter {
  id: number
  title: string
  lessons: LessonBrief[]
}

export type MaterialType = 'PDF' | 'IMAGE' | 'AUDIO'

export interface MaterialMeta {
  id: number
  name: string
  type: MaterialType
  previewOnly: boolean
}

export interface TeacherBrief {
  id: number
  name: string
  avatar: string
  intro: string
}

export interface CourseDetail {
  id: number
  title: string
  cover: string
  intro: string
  teacher: TeacherBrief
  targetScore: string
  price: number
  originalPrice: number
  validityType: ValidityType
  validityDays?: number | null
  fixedExpireAt?: string | null
  inSubscription: boolean
  sales: number
  ctaState: CtaState
  chapters: Chapter[]
  materials: MaterialMeta[]
}

export interface SubPlan {
  id: number
  title: string
  period: 'MONTH' | 'QUARTER' | 'YEAR'
  price: number
  dailyPrice: number
}

// ---------- order ----------
export type ItemType = 'COURSE' | 'BUNDLE' | 'SUBSCRIPTION'
export type OrderStatus = 'UNPAID' | 'PAID' | 'CLOSED' | 'REFUNDED'
export type PayChannel = 'NATIVE' | 'JSAPI' | 'H5'

export interface OrderCreateReq {
  itemType: ItemType
  itemId: number
  userCouponId?: number | null
}

export interface OrderAmount {
  originAmount: number
  discountAmount: number
  payAmount: number
  couponApplicable: boolean
  couponReason?: string | null
}

export interface Order {
  orderNo: string
  itemType: ItemType
  itemId: number
  itemTitle: string
  originAmount: number
  discountAmount: number
  payAmount: number
  status: OrderStatus
  payChannel?: PayChannel | null
  createdAt: string
  paidAt?: string | null
  expireAt?: string | null
}

// ---------- play ----------
export interface Watermark {
  text: string
  opacity: number
  mode: 'MARQUEE' | 'FIXED'
}

export interface PlayCredential {
  vodFileId: string
  psign: string
  appId: number
  expireIn: number
  lastPosition: number
  isTrial: boolean
  watermark: Watermark
}

// ---------- me ----------
export type Role = 'USER' | 'TEACHER' | 'ADMIN'

export interface UserProfile {
  id: number
  nickname: string
  avatar: string
  phone?: string | null
  roles: Role[]
  subscribed: boolean
}

export interface MyCourse {
  courseId: number
  title: string
  cover: string
  overallProgress: number
  lastLessonId?: number | null
  lastPosition?: number | null
  source: 'PURCHASE' | 'BUNDLE' | 'SUBSCRIPTION'
  expireAt?: string | null
  status: 'LEARNING' | 'FINISHED' | 'EXPIRED'
}
