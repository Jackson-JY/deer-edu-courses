import { http } from './client'
import { router } from '@/router'

/**
 * 轻量访问埋点：仅上报页面浏览（PAGE_VIEW）。
 * 课程浏览（COURSE_VIEW）、课时播放（LESSON_VIEW）、课件预览、登录等
 * 敏感/关键事件由后端在对应接口自动记录，前端不重复上报。
 */

const PAGE_TITLES: Record<string, string> = {
  home: '首页',
  courses: '全部课程',
  search: '搜索课程',
  subscribe: '会员订阅',
  'my-learning': '我的学习',
  'my-settings': '账号设置',
}

// 不做 PV 上报的路由：后台、登录、404，以及由后端记录的课程详情/播放页
const SKIP_NAMES = new Set([
  'admin-login', 'admin-dashboard', 'admin-courses', 'admin-course-edit',
  'admin-users', 'admin-user-entitlements', 'admin-security', 'admin-logs',
  'course-detail', 'player', 'not-found',
])

function reportPageView(path: string, title: string) {
  // sendBeacon 不保证带 Authorization，且这里需要登录态识别；统一走 axios，失败静默
  http
    .post('/api/track', { path, title })
    .catch(() => {
      /* 埋点失败不影响用户 */
    })
}

let initialized = false

export function initTracking() {
  if (initialized) return
  initialized = true
  router.afterEach((to) => {
    const name = typeof to.name === 'string' ? to.name : ''
    if (!name || SKIP_NAMES.has(name)) return
    const title = PAGE_TITLES[name] ?? (String(to.meta.title ?? '') || null)
    if (!title) return
    reportPageView(to.fullPath, title)
  })
}
