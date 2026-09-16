<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sidebarOpen = ref(false)

const menus = [
  { path: '/admin', label: '工作台', icon: 'DataLine' },
  { path: '/admin/courses', label: '课程管理', icon: 'Reading' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/logs', label: '日志审计', icon: 'Document' },
  { path: '/writing/teacher', label: '写作批改', icon: 'EditPen' },
  { path: '/admin/security', label: '安全策略', icon: 'Lock' },
]

const active = computed(() => route.path)

function logout() {
  auth.logout()
  router.push('/admin/login')
}

function onMenuClick() {
  if (window.innerWidth <= 768) {
    sidebarOpen.value = false
  }
}
</script>

<template>
  <div class="admin-layout">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false" />

    <aside :class="['admin-sidebar', { open: sidebarOpen }]">
      <RouterLink to="/admin" class="admin-brand">
        <img class="admin-brand-logo" src="/logo.jpg" alt="Logo" />
        <span>
          <strong>Deer Edu</strong>
          <em>管理后台</em>
        </span>
      </RouterLink>
      <nav class="admin-menu">
        <RouterLink
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          :class="['admin-menu-item', { active: active === m.path || active.startsWith(m.path + '/') }]"
          @click="onMenuClick"
        >
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-left">
          <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen" aria-label="菜单">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path v-if="!sidebarOpen" d="M3 6h18M3 12h18M3 18h18" />
              <path v-else d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
          <span class="admin-header-title">{{ route.meta.title || '管理后台' }}</span>
        </div>
        <div class="admin-header-user">
          <span class="admin-phone">{{ auth.user?.phone }}</span>
          <button class="btn btn-ghost btn-sm" @click="logout">退出</button>
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
  background: var(--background-100);
}
.admin-sidebar {
  width: 220px;
  background: var(--background);
  color: var(--foreground);
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}
.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 20px;
  color: var(--foreground);
  border-bottom: 1px solid var(--border);
  text-decoration: none;
}
.admin-brand-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.admin-brand strong {
  display: block;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
}
.admin-brand em {
  display: block;
  font-size: 11px;
  font-style: normal;
  color: var(--muted-foreground);
  font-weight: 500;
}
.admin-menu {
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.admin-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  color: var(--muted-foreground);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: background 0.18s ease, color 0.18s ease;
}
.admin-menu-item:hover {
  background: var(--secondary);
  color: var(--foreground);
}
.admin-menu-item.active {
  background: var(--primary);
  color: var(--primary-foreground);
}
.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.admin-header {
  height: 64px;
  background: rgba(255, 255, 255, 0.72);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.admin-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.admin-header-title {
  font-weight: 600;
  font-size: 16px;
}
.sidebar-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--foreground);
  transition: background 0.18s ease;
}
.sidebar-toggle:hover { background: var(--secondary); }
.admin-header-user {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: var(--muted-foreground);
}
.admin-body {
  flex: 1;
  padding: 24px;
  overflow: auto;
}

/* 移动端遮罩 */
.sidebar-overlay {
  display: none;
}

/* ===== 响应式：768px 以下 ===== */
@media (max-width: 768px) {
  .sidebar-toggle {
    display: flex;
  }
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: var(--shadow-lg);
  }
  .admin-sidebar.open {
    transform: translateX(0);
  }
  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 150;
  }
  .admin-header {
    padding: 0 16px;
  }
  .admin-phone {
    display: none;
  }
  .admin-body {
    padding: 16px;
  }
}
</style>
