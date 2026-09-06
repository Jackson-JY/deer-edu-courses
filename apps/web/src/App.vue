<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { features } from '@/config'
import { useAuthStore } from '@/stores/auth'
import LoginDialog from '@/components/LoginDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

function onKicked() {
  auth.kick()
  ElMessage.warning('账号已在其他设备登录，请重新登录')
}

onMounted(() => {
  auth.restore()
  window.addEventListener('auth:kicked', onKicked)
})
onUnmounted(() => {
  window.removeEventListener('auth:kicked', onKicked)
})

const navs = computed(() =>
  [
    { to: '/courses', label: '课程', show: true },
    { to: '/courses?type=bundle', label: '套餐', show: features.bundle },
    { to: '/subscribe', label: '订阅会员', vip: true, show: features.subscription },
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
  <header v-if="!isAdminRoute" class="topnav">
    <div class="container topnav__inner">
      <RouterLink to="/" class="brand">
        <span class="brand-mark">IELTS</span>
        <span class="brand-name">雅思学堂</span>
      </RouterLink>

      <nav class="links">
        <RouterLink
          v-for="n in navs"
          :key="n.to"
          :to="n.to"
          :class="['link', { active: isActive(n.to), vip: n.vip }]"
        >
          {{ n.label }}
        </RouterLink>
      </nav>

      <div class="actions">
        <el-input
          v-model="keyword"
          placeholder="搜索课程"
          class="search"
          clearable
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <template v-if="auth.isLoggedIn">
          <el-dropdown @command="onUserCommand">
            <span class="user-trigger">
              <span class="user-avatar">{{ (auth.user?.nickname || '我')[0] }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/my/learning"><el-icon><VideoCamera /></el-icon> 我的学习</el-dropdown-item>
                <el-dropdown-item command="/my/orders"><el-icon><List /></el-icon> 我的订单</el-dropdown-item>
                <el-dropdown-item command="/my/coupons"><el-icon><Ticket /></el-icon> 我的优惠券</el-dropdown-item>
                <el-dropdown-item command="/my/settings"><el-icon><Setting /></el-icon> 账号设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" round @click="auth.openLogin()">登录 / 注册</el-button>
      </div>
    </div>
  </header>

  <main class="page">
    <RouterView />
  </main>

  <footer v-if="!isAdminRoute" class="footer">
    <div class="container footer__inner">
      <div class="footer-col">
        <div class="brand-name">雅思学堂</div>
        <p>专注雅思冲分的在线课程平台</p>
      </div>
      <div class="footer-links">
        <a>关于我们</a><a>常见问题</a><a>联系客服</a><a>用户协议</a><a>隐私政策</a>
      </div>
    </div>
    <div class="footer-copy">© 2026 雅思学堂 · 京ICP备xxxxxxxx号</div>
  </footer>

  <LoginDialog />
</template>

<style scoped>
.topnav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(180%) blur(8px);
  border-bottom: 1px solid var(--line);
}
.topnav__inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 64px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.brand-mark {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #fff;
  background: var(--brand-500);
  padding: 4px 8px;
  border-radius: var(--r-sm);
}
.brand-name {
  font-size: 18px;
  font-weight: 700;
}
.links {
  display: flex;
  gap: 4px;
}
.link {
  position: relative;
  padding: 8px 12px;
  font-size: 15px;
  color: var(--text-2);
  border-radius: var(--r-sm);
  transition: color 0.15s, background 0.15s;
  white-space: nowrap;
}
.link:hover {
  color: var(--brand-600);
  background: var(--brand-50);
}
.link.active {
  color: var(--brand-600);
  font-weight: 600;
}
.link.vip {
  color: var(--accent);
}
.actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
  align-items: center;
}
.search {
  width: 220px;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--text-2);
}
.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--brand-500);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.page {
  min-height: calc(100vh - 64px - 200px);
}
.footer {
  margin-top: 64px;
  background: #0f1f33;
  color: #c4cdda;
}
.footer__inner {
  display: flex;
  justify-content: space-between;
  gap: 40px;
  padding-top: 40px;
  padding-bottom: 28px;
}
.footer .brand-name {
  color: #fff;
  font-size: 18px;
  margin-bottom: 8px;
}
.footer-col p {
  margin: 0;
  font-size: 14px;
  color: #8b97a8;
}
.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-content: flex-start;
}
.footer-links a {
  font-size: 14px;
  color: #c4cdda;
  cursor: pointer;
}
.footer-links a:hover {
  color: #fff;
}
.footer-copy {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
  font-size: 13px;
  color: #6b7888;
  padding: 16px;
}
</style>
