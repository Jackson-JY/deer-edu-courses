<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  listAdminLogs,
  listUserLogs,
  getLogOverview,
  type AdminOperationLogItem,
  type UserEventLogItem,
  type LogOverview,
} from '@/api/admin'

const activeTab = ref<'dashboard' | 'admin' | 'user'>('dashboard')
const loading = ref(false)

// ===== 访问看板 =====
const overview = ref<LogOverview | null>(null)

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await getLogOverview()
  } finally {
    loading.value = false
  }
}

const maxTrend = computed(() => Math.max(1, ...(overview.value?.pvTrend ?? []).map((d) => Math.max(d.pv, d.uv))))
const total14dPv = computed(() => (overview.value?.pvTrend ?? []).reduce((s, d) => s + d.pv, 0))
const maxTopViews = computed(() => Math.max(1, ...(overview.value?.topCourses ?? []).map((c) => c.views)))
const eventCountItems = computed(() => {
  const counts = overview.value?.eventTypeCounts ?? {}
  const keys = Object.keys(counts).filter((k) => counts[k] > 0)
  const max = Math.max(1, ...keys.map((k) => counts[k]))
  return keys
    .sort((a, b) => counts[b] - counts[a])
    .map((k) => ({ type: k, label: EVENT_TYPES[k] || k, count: counts[k], pct: (counts[k] / max) * 100 }))
})

function barHeight(v: number): string {
  return `${Math.max(2, Math.round((v / maxTrend.value) * 100))}%`
}
function rankWidth(v: number): string {
  return `${Math.max(4, Math.round((v / maxTopViews.value) * 100))}%`
}
function shortDate(d: string): string {
  return d.substring(5)
}

const EVENT_TYPES: Record<string, string> = {
  LOGIN: '登录',
  LOGIN_FAIL: '登录失败',
  LOGOUT: '退出登录',
  REGISTER: '注册',
  PAGE_VIEW: '页面浏览',
  COURSE_VIEW: '课程浏览',
  LESSON_VIEW: '课时播放',
  MATERIAL_PREVIEW: '课件预览',
}

// ===== 管理员操作 =====
const adminList = ref<AdminOperationLogItem[]>([])
const adminTotal = ref(0)
const adminPage = ref(1)
const pageSize = ref(20)
const moduleFilter = ref('')
const keyword = ref('')

const MODULES = ['课程管理', '用户管理', '视频点播', '安全策略', '文件上传', '日志审计', '写作批改', '工作台', '其他']

async function loadAdmin() {
  loading.value = true
  try {
    const res = await listAdminLogs({
      page: adminPage.value - 1,
      size: pageSize.value,
      module: moduleFilter.value || undefined,
      keyword: keyword.value || undefined,
    })
    adminList.value = res.list
    adminTotal.value = res.total
  } finally {
    loading.value = false
  }
}

function searchAdmin() {
  adminPage.value = 1
  loadAdmin()
}

function prettyParams(raw: string | null): string {
  if (!raw) return '（无参数）'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function methodType(m: string): 'warning' | 'primary' | 'danger' | 'info' {
  if (m === 'POST') return 'warning'
  if (m === 'PUT') return 'primary'
  if (m === 'DELETE') return 'danger'
  return 'info'
}

// ===== 用户动态 =====
const userList = ref<UserEventLogItem[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const eventTypeFilter = ref('')
const targetTypeFilter = ref('')

type TagType = 'success' | 'danger' | 'info' | 'warning' | 'primary' | undefined

const EVENT_TAG: Record<string, TagType> = {
  LOGIN: 'success',
  LOGIN_FAIL: 'danger',
  LOGOUT: 'info',
  REGISTER: 'success',
  PAGE_VIEW: undefined,
  COURSE_VIEW: 'warning',
  LESSON_VIEW: 'primary',
  MATERIAL_PREVIEW: 'warning',
}
const TARGET_TYPES: Record<string, string> = {
  PAGE: '页面',
  COURSE: '课程',
  LESSON: '课时',
  MATERIAL: '课件',
}

async function loadUser() {
  loading.value = true
  try {
    const res = await listUserLogs({
      page: userPage.value - 1,
      size: pageSize.value,
      eventType: eventTypeFilter.value || undefined,
      targetType: targetTypeFilter.value || undefined,
    })
    userList.value = res.list
    userTotal.value = res.total
  } finally {
    loading.value = false
  }
}

function searchUser() {
  userPage.value = 1
  loadUser()
}

function switchTab(tab: 'dashboard' | 'admin' | 'user') {
  activeTab.value = tab
  if (tab === 'dashboard' && overview.value === null) loadOverview()
  if (tab === 'admin' && adminList.value.length === 0) loadAdmin()
  if (tab === 'user' && userList.value.length === 0) loadUser()
}

function fmt(t: string): string {
  return t ? t.replace('T', ' ').substring(0, 19) : ''
}

function userDisplay(row: UserEventLogItem): string {
  if (row.userId == null) return '匿名访客'
  return row.userName || `UID${row.userId}`
}

onMounted(loadOverview)
</script>

<template>
  <div v-loading="loading">
    <h1 class="page-title">日志审计</h1>

    <el-tabs
      :model-value="activeTab"
      @tab-change="(v: string | number) => switchTab(v as 'dashboard' | 'admin' | 'user')"
    >
      <!-- ========== 访问看板 ========== -->
      <el-tab-pane label="访问看板" name="dashboard">
        <template v-if="overview">
          <div class="stats">
            <div class="stat-card">
              <div class="stat-value">{{ overview.todayPv }}</div>
              <div class="stat-label">今日浏览 PV</div>
              <div class="stat-sub">页面/课程/课时访问</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ overview.todayUv }}</div>
              <div class="stat-label">今日访客 UV</div>
              <div class="stat-sub">按用户/IP 去重</div>
            </div>
            <div class="stat-card stat-accent">
              <div class="stat-value">{{ overview.todayAdminOps }}</div>
              <div class="stat-label">今日管理员操作</div>
              <div class="stat-sub">新增/修改/删除类操作</div>
            </div>
            <div class="stat-card stat-accent">
              <div class="stat-value">{{ total14dPv }}</div>
              <div class="stat-label">近 14 天浏览 PV</div>
              <div class="stat-sub">累计访问次数</div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">近 14 天浏览趋势</div>
            <div class="trend-chart">
              <div v-for="d in overview.pvTrend" :key="d.date" class="trend-col">
                <div class="trend-bars">
                  <div class="bar bar-pv" :style="{ height: barHeight(d.pv) }">
                    <span v-if="d.pv" class="bar-num">{{ d.pv }}</span>
                  </div>
                  <div class="bar bar-uv" :style="{ height: barHeight(d.uv) }" />
                </div>
                <div class="trend-date">{{ shortDate(d.date) }}</div>
              </div>
            </div>
            <div class="legend">
              <span><i class="dot dot-pv" />PV 浏览量</span>
              <span><i class="dot dot-uv" />UV 访客数</span>
            </div>
          </div>

          <div class="panel-row">
            <div class="panel half">
              <div class="panel-title">课程浏览榜（近 14 天）</div>
              <div v-if="overview.topCourses.length === 0" class="muted empty-hint">暂无数据</div>
              <div v-for="(c, i) in overview.topCourses" :key="c.courseId" class="rank-row">
                <span class="rank-idx" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-name" :title="c.title">{{ c.title }}</span>
                <span class="rank-bar-wrap"><span class="rank-bar" :style="{ width: rankWidth(c.views) }" /></span>
                <span class="rank-num">{{ c.views }}</span>
              </div>
            </div>
            <div class="panel half">
              <div class="panel-title">今日事件分布</div>
              <div v-if="eventCountItems.length === 0" class="muted empty-hint">暂无数据</div>
              <div v-for="e in eventCountItems" :key="e.type" class="rank-row">
                <span class="rank-name fixed">{{ e.label }}</span>
                <span class="rank-bar-wrap"><span class="rank-bar alt" :style="{ width: e.pct + '%' }" /></span>
                <span class="rank-num">{{ e.count }}</span>
              </div>
            </div>
          </div>

          <div class="panel-row">
            <div class="panel half">
              <div class="panel-title">最近管理员操作</div>
              <div v-if="overview.recentAdminOps.length === 0" class="muted empty-hint">暂无数据</div>
              <div v-for="op in overview.recentAdminOps.slice(0, 8)" :key="op.id" class="recent-row">
                <span class="recent-time">{{ fmt(op.createdAt) }}</span>
                <span class="recent-main">
                  <b>{{ op.adminName || `UID${op.adminUserId ?? '-'}` }}</b>
                  {{ op.action }}
                  <el-tag :type="op.success === 1 ? 'success' : 'danger'" size="small">
                    {{ op.success === 1 ? '成功' : '失败' }}
                  </el-tag>
                </span>
              </div>
            </div>
            <div class="panel half">
              <div class="panel-title">最近用户动态</div>
              <div v-if="overview.recentUserEvents.length === 0" class="muted empty-hint">暂无数据</div>
              <div v-for="ev in overview.recentUserEvents.slice(0, 8)" :key="ev.id" class="recent-row">
                <span class="recent-time">{{ fmt(ev.createdAt) }}</span>
                <span class="recent-main">
                  <b>{{ userDisplay(ev) }}</b>
                  <el-tag :type="EVENT_TAG[ev.eventType]" size="small">
                    {{ EVENT_TYPES[ev.eventType] || ev.eventType }}
                  </el-tag>
                  <span v-if="ev.targetName" class="muted">{{ ev.targetName }}</span>
                </span>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else description="加载中..." />
      </el-tab-pane>

      <!-- ========== 管理员操作 ========== -->
      <el-tab-pane label="管理员操作" name="admin">
        <div class="filter-bar">
          <el-select v-model="moduleFilter" placeholder="全部模块" clearable style="width: 150px" @change="searchAdmin">
            <el-option v-for="m in MODULES" :key="m" :label="m" :value="m" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="动作 / 路径 / 操作者"
            clearable
            style="width: 240px"
            @keyup.enter="searchAdmin"
            @clear="searchAdmin"
          />
          <el-button type="primary" @click="searchAdmin">查询</el-button>
          <el-button @click="moduleFilter = ''; keyword = ''; searchAdmin()">重置</el-button>
        </div>

        <el-table :data="adminList" stripe style="width: 100%" empty-text="暂无操作记录">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="detail-box">
                <div class="detail-line"><b>完整路径：</b>{{ row.method }} {{ row.path }}</div>
                <div class="detail-line"><b>IP：</b>{{ row.ip || '-' }}　<b>耗时：</b>{{ row.durationMs ?? '-' }} ms</div>
                <div class="detail-line"><b>User-Agent：</b>{{ row.userAgent || '-' }}</div>
                <div v-if="row.error" class="detail-line error"><b>错误信息：</b>{{ row.error }}</div>
                <div class="detail-line"><b>请求参数：</b></div>
                <pre class="params-pre">{{ prettyParams(row.params) }}</pre>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作者" width="140">
            <template #default="{ row }">{{ row.adminName || `UID${row.adminUserId ?? '-'}` }}</template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="action" label="动作" min-width="130" />
          <el-table-column label="方法" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="methodType(row.method)" size="small">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
                {{ row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路径" min-width="220">
            <template #default="{ row }"><span class="path-text">{{ row.path }}</span></template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            :current-page="adminPage"
            :page-size="pageSize"
            :total="adminTotal"
            layout="total, prev, pager, next"
            background
            @current-change="(p: number) => { adminPage = p; loadAdmin() }"
          />
        </div>
      </el-tab-pane>

      <!-- ========== 用户动态 ========== -->
      <el-tab-pane label="用户访问动态" name="user">
        <div class="filter-bar">
          <el-select v-model="eventTypeFilter" placeholder="全部事件" clearable style="width: 150px" @change="searchUser">
            <el-option v-for="(label, value) in EVENT_TYPES" :key="value" :label="label" :value="value" />
          </el-select>
          <el-select v-model="targetTypeFilter" placeholder="全部对象" clearable style="width: 140px" @change="searchUser">
            <el-option v-for="(label, value) in TARGET_TYPES" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button type="primary" @click="searchUser">查询</el-button>
          <el-button @click="eventTypeFilter = ''; targetTypeFilter = ''; searchUser()">重置</el-button>
        </div>

        <el-table :data="userList" stripe style="width: 100%" empty-text="暂无访问记录">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="detail-box">
                <div class="detail-line"><b>IP：</b>{{ row.ip || '-' }}</div>
                <div class="detail-line"><b>User-Agent：</b>{{ row.userAgent || '-' }}</div>
                <div class="detail-line"><b>对象 ID：</b>{{ row.targetId || '-' }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="用户" min-width="140">
            <template #default="{ row }">
              {{ userDisplay(row as UserEventLogItem) }}
              <el-tag v-if="row.userId == null" size="small" type="info" effect="plain">匿名</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="事件" width="110">
            <template #default="{ row }">
              <el-tag :type="EVENT_TAG[row.eventType as string]" size="small">
                {{ EVENT_TYPES[row.eventType] || row.eventType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="浏览对象" min-width="200">
            <template #default="{ row }">
              <span v-if="row.targetType">
                [{{ TARGET_TYPES[row.targetType] || row.targetType }}]
                {{ row.targetName || row.targetId || '' }}
              </span>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="详情" min-width="180">
            <template #default="{ row }"><span class="muted">{{ row.detail || '' }}</span></template>
          </el-table-column>
          <el-table-column label="IP" width="140">
            <template #default="{ row }">{{ row.ip || '-' }}</template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            :current-page="userPage"
            :page-size="pageSize"
            :total="userTotal"
            layout="total, prev, pager, next"
            background
            @current-change="(p: number) => { userPage = p; loadUser() }"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.detail-box {
  padding: 10px 20px 14px;
  font-size: 13px;
  color: var(--muted-foreground);
  line-height: 1.9;
}
.detail-line.error {
  color: var(--el-color-danger);
}
.params-pre {
  margin: 6px 0 0;
  padding: 10px;
  background: var(--secondary, #f5f5f5);
  border-radius: 6px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.path-text {
  font-size: 12px;
  color: var(--muted-foreground);
}
.muted {
  color: var(--muted-foreground);
}

/* ===== 访问看板 ===== */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1024px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
.stat-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.1rem 1.4rem;
  box-shadow: var(--shadow-sm);
}
.stat-value {
  font-size: 1.85rem;
  font-weight: 700;
  color: var(--primary);
  line-height: 1.1;
}
.stat-label {
  font-size: 13px;
  color: var(--muted-foreground);
  margin-top: 6px;
}
.stat-sub {
  font-size: 12px;
  color: var(--muted-foreground);
  margin-top: 4px;
  opacity: 0.8;
}
.stat-accent .stat-value {
  color: var(--success, #10b981);
}

.panel {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  color: var(--foreground);
}
.panel-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 1024px) {
  .panel-row {
    grid-template-columns: 1fr;
  }
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 160px;
  padding-top: 8px;
}
.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
}
.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 130px;
  width: 100%;
  justify-content: center;
}
.bar {
  width: 12px;
  max-width: 40%;
  border-radius: 3px 3px 0 0;
  position: relative;
  transition: height 0.3s ease;
}
.bar-pv {
  background: var(--primary, #2563eb);
}
.bar-uv {
  background: var(--success, #10b981);
  opacity: 0.75;
}
.bar-num {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--muted-foreground);
  white-space: nowrap;
}
.trend-date {
  font-size: 10px;
  color: var(--muted-foreground);
  margin-top: 6px;
  white-space: nowrap;
}
.legend {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--muted-foreground);
}
.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}
.dot-pv {
  background: var(--primary, #2563eb);
}
.dot-uv {
  background: var(--success, #10b981);
  opacity: 0.75;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  font-size: 13px;
}
.rank-idx {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: var(--secondary, #f0f0f0);
  color: var(--muted-foreground);
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rank-idx.top {
  background: var(--primary, #2563eb);
  color: #fff;
}
.rank-name {
  width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
}
.rank-name.fixed {
  width: 76px;
}
.rank-bar-wrap {
  flex: 1;
  height: 14px;
  background: var(--secondary, #f5f5f5);
  border-radius: 7px;
  overflow: hidden;
}
.rank-bar {
  display: block;
  height: 100%;
  border-radius: 7px;
  background: linear-gradient(90deg, var(--primary, #2563eb), #60a5fa);
  transition: width 0.3s ease;
}
.rank-bar.alt {
  background: linear-gradient(90deg, var(--success, #10b981), #6ee7b7);
}
.rank-num {
  width: 44px;
  text-align: right;
  color: var(--muted-foreground);
  font-size: 12px;
  flex-shrink: 0;
}
.empty-hint {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
}

.recent-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--border, #eee);
}
.recent-row:last-child {
  border-bottom: none;
}
.recent-time {
  font-size: 12px;
  color: var(--muted-foreground);
  flex-shrink: 0;
  width: 150px;
}
.recent-main {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  min-width: 0;
}
</style>
