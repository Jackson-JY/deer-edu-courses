import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/courses', name: 'courses', component: () => import('@/views/CourseListView.vue') },
  { path: '/course/:id', name: 'course-detail', component: () => import('@/views/CourseDetailView.vue') },
  { path: '/subscribe', name: 'subscribe', component: () => import('@/views/SubscribeView.vue') },
  { path: '/search', name: 'search', component: () => import('@/views/SearchView.vue') },
  {
    path: '/learn/:courseId/:lessonId',
    name: 'player',
    component: () => import('@/views/PlayerView.vue'),
    meta: { requiresAuth: true },
  },
  { path: '/my/learning', name: 'my-learning', component: () => import('@/views/MyLearningView.vue'), meta: { requiresAuth: true } },
  { path: '/my/settings', name: 'my-settings', component: () => import('@/views/AccountSettingsView.vue'), meta: { requiresAuth: true } },

  // 管理后台
  { path: '/admin/login', name: 'admin-login', component: () => import('@/views/admin/AdminLoginView.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, adminOnly: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'courses', name: 'admin-courses', component: () => import('@/views/admin/CourseManageView.vue') },
      { path: 'courses/:id', name: 'admin-course-edit', component: () => import('@/views/admin/CourseEditView.vue') },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UserManageView.vue') },
      { path: 'users/:id/entitlements', name: 'admin-user-entitlements', component: () => import('@/views/admin/UserEntitlementView.vue') },
      { path: 'security', name: 'admin-security', component: () => import('@/views/admin/SecurityView.vue') },
    ],
  },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.path.startsWith('/admin')) {
    if (to.path === '/admin/login') return true
    if (!getToken()) {
      return '/admin/login'
    }
    if (to.meta.adminOnly && !auth.isAdmin) {
      return '/admin/login'
    }
    return true
  }

  if (to.meta.requiresAuth && !getToken()) {
    auth.openLogin()
    return false
  }
  return true
})
