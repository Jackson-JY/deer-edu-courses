<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { features } from '@/config'
import { useAuthStore } from '@/stores/auth'
import LoginDialog from '@/components/LoginDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')
const mobileMenuOpen = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

function onKicked() {
  auth.kick()
  ElMessage.warning('登录已失效，请重新登录')
}

onMounted(() => {
  auth.restore()
  window.addEventListener('auth:kicked', onKicked)
})
onUnmounted(() => {
  window.removeEventListener('auth:kicked', onKicked)
})

watch(() => route.path, () => { mobileMenuOpen.value = false })

const navs = computed(() =>
  [
    { to: '/courses', label: '课程', show: true },
    { to: '/courses?type=bundle', label: '套餐', show: features.bundle },
    { to: '/subscribe', label: '订阅会员', vip: true, show: features.subscription },
    { to: '/my/writing', label: '写作批改', show: true },
    { to: '/my/learning', label: '我的学习', show: true },
  ].filter((n) => n.show),
)

function isActive(to: string) {
  return route.path === to.split('?')[0]
}
function onSearch() {
  if (keyword.value.trim()) router.push({ path: '/search', query: { q: keyword.value.trim() } })
}
function onUserCommand(cmd: string) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/')
  } else {
    router.push(cmd)
  }
}
</script>

<template>
  <header v-if="!isAdminRoute" class="globalnav" aria-label="主导航">
    <div class="globalnav__inner">
      <RouterLink to="/" class="brand">
        <img class="brand-logo" src="/logo.jpg" alt="Logo" />
        <span>Deer Edu</span>
      </RouterLink>

      <nav class="rail pc-only" aria-label="主导航链接">
        <RouterLink
          v-for="n in navs"
          :key="n.to"
          :to="n.to"
          :class="['link', { active: isActive(n.to), vip: n.vip }]"
        >
          {{ n.label }}
        </RouterLink>
      </nav>

      <div class="tools">
        <div class="field search-field">
          <input
            v-model="keyword"
            class="control"
            placeholder="搜索课程"
            @keyup.enter="onSearch"
          >
        </div>

        <template v-if="auth.isLoggedIn">
          <el-dropdown @command="onUserCommand" trigger="click">
            <span class="user-trigger">
              <span class="user-avatar">{{ (auth.user?.nickname || '我')[0] }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/my/learning">我的学习</el-dropdown-item>
              <el-dropdown-item command="/my/writing">我的写作</el-dropdown-item>
              <el-dropdown-item command="/my/settings">账号设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <button v-else class="btn btn-primary" @click="auth.openLogin()">登录 / 注册</button>

        <button
          class="icon-btn mobile-only"
          @click="mobileMenuOpen = !mobileMenuOpen"
          aria-label="菜单"
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path v-if="!mobileMenuOpen" d="M3 6h18M3 12h18M3 18h18" />
            <path v-else d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <div v-if="mobileMenuOpen" class="mobile-nav">
      <RouterLink
        v-for="n in navs"
        :key="n.to"
        :to="n.to"
        :class="['mobile-nav__link', { active: isActive(n.to) }]"
      >
        {{ n.label }}
      </RouterLink>
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/my/settings" class="mobile-nav__link">账号设置</RouterLink>
        <a class="mobile-nav__link" @click="onUserCommand('logout')">退出登录</a>
      </template>
    </div>
  </header>

  <main class="page">
    <!-- 按完整路径 key：切换课时（/learn/:courseId/:lessonId）时整页重挂载，确保播放器干净重建 -->
    <RouterView :key="route.fullPath" />
  </main>

  <footer v-if="!isAdminRoute" class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <div class="brand-name">
          <img class="brand-logo-sm" src="/logo.jpg" alt="Logo" />
          Deer Edu
        </div>
        <p>专注雅思冲分的在线课程平台</p>
      </div>
      <nav class="footer-links" aria-label="页脚链接">
        <a>关于我们</a>
        <a>常见问题</a>
        <a>联系客服</a>
        <a>用户协议</a>
        <a>隐私政策</a>
      </nav>
    </div>
    <div class="copyright">© 2026 IELTS学堂</div>
  </footer>

  <LoginDialog />
</template>

<style scoped>
.globalnav {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0 max(1rem, calc((100vw - 1200px) / 2));
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.72);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  backdrop-filter: saturate(180%) blur(20px);
}
.globalnav__inner {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: min(1200px, 100%);
  margin: 0 auto;
}
.brand {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-decoration: none;
  white-space: nowrap;
  color: var(--foreground);
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
}
.brand-logo-sm {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  margin-right: 6px;
  vertical-align: middle;
}
.rail {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.link {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  color: var(--foreground);
  transition: background-color 0.18s ease;
}
.link:hover { background: var(--secondary); }
.link.active { color: var(--primary); }
.link.vip { color: var(--chart-3); }

.tools {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: auto;
}
.search-field {
  width: 220px;
  height: 36px;
}
.user-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--primary-foreground);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.18s ease;
}
.icon-btn:hover { background: var(--secondary); }
.mobile-only { display: none; }

.mobile-nav {
  display: none;
  flex-direction: column;
  background: var(--background);
  border-bottom: 1px solid var(--border);
}
.mobile-nav__link {
  display: block;
  padding: 14px 20px;
  font-size: 15px;
  color: var(--foreground);
  border-bottom: 1px solid var(--border);
}
.mobile-nav__link.active { color: var(--primary); font-weight: 600; }

.page {
  min-height: calc(100vh - 44px - 120px);
}

/* 页脚 */
.site-footer {
  border-top: 1px solid var(--border);
  background: var(--background);
  margin-top: 4rem;
}
.footer-inner {
  width: min(1200px, 100%);
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.footer-brand .brand-name {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
}
.footer-brand p {
  margin: 0;
  font-size: 13px;
  color: var(--muted-foreground);
}
.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}
.footer-links a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--muted-foreground);
  text-decoration: none;
  transition: background-color 0.18s ease, color 0.18s ease;
}
.footer-links a:hover { background: var(--secondary); color: var(--foreground); }
.copyright {
  font-size: 12px;
  color: var(--muted-foreground);
  text-align: center;
  padding: 16px;
  border-top: 1px solid var(--border);
}

@media (max-width: 767px) {
  .pc-only { display: none !important; }
  .mobile-only { display: inline-flex; }
  .search-field { width: 140px; }
  .mobile-nav { display: flex; }
  .footer-inner { flex-direction: column; align-items: flex-start; }
}
@media (max-width: 480px) {
  .brand { font-size: 13px; }
  .search-field { width: 100px; }
}
</style>
