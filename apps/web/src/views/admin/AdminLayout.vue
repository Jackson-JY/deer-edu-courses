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
  { path: '/admin/security', label: '安全策略', icon: 'Lock' },
]

const active = computed(() => route.path)

function logout() {
  auth.logout()
  router.push('/admin/login')
}

function onMenuClick() {
  // 移动端点击菜单后收起侧边栏
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
          <el-icon class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen">
            <Fold v-if="sidebarOpen" />
            <Expand v-else />
          </el-icon>
          <span class="admin-header-title">{{ route.meta.title || '管理后台' }}</span>
        </div>
        <div class="admin-header-user">
          <span class="admin-phone">{{ auth.user?.phone }}</span>
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
  font-size: 20px;
  cursor: pointer;
  color: var(--text-2);
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

/* 移动端遮罩 */
.sidebar-overlay {
  display: none;
}

/* ===== 响应式：768px 以下 ===== */
@media (max-width: 768px) {
  .sidebar-toggle {
    display: flex;
    align-items: center;
  }
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
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
