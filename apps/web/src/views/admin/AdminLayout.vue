<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/admin', label: '工作台', icon: 'DataLine' },
  { path: '/admin/courses', label: '课程管理', icon: 'Reading' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/security', label: '安全策略', icon: 'Lock' },
]

const active = computed(() => route.path)

function logout() {
  auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <span class="mark">IELTS</span>
        <span>管理后台</span>
      </div>
      <nav class="admin-menu">
        <RouterLink
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          :class="['admin-menu-item', { active: active === m.path || active.startsWith(m.path + '/') }]"
        >
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <span class="admin-header-title">{{ route.meta.title || '管理后台' }}</span>
        <div class="admin-header-user">
          <span>{{ auth.user?.phone }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </header>
      <div class="admin-body">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-soft, #f6f7f9);
}
.admin-sidebar {
  width: 220px;
  background: #0f1f33;
  color: #c4cdda;
  flex-shrink: 0;
}
.admin-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 64px;
  padding: 0 20px;
  font-weight: 700;
  font-size: 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.admin-brand .mark {
  font-size: 12px;
  color: #fff;
  background: var(--brand-500);
  padding: 3px 7px;
  border-radius: var(--r-sm);
}
.admin-menu {
  padding: 12px 0;
}
.admin-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 20px;
  color: #c4cdda;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.15s;
}
.admin-menu-item:hover,
.admin-menu-item.active {
  background: rgba(255,255,255,0.06);
  color: #fff;
}
.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.admin-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.admin-header-title {
  font-weight: 600;
  font-size: 16px;
}
.admin-header-user {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: var(--text-2);
}
.admin-body {
  flex: 1;
  padding: 24px;
  overflow: auto;
}
</style>
