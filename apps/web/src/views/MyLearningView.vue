<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCourseDetail, fetchMyProgress, fetchMyCourses } from '@/api/courses'
import type { WatchProgress, EnrolledCourse } from '@/api/courses'

const router = useRouter()
const tab = ref<'learning' | 'finished'>('learning')

interface MyCourseRow {
  courseId: number
  title: string
  progress: number
  lastLesson: string
  lastLessonId: number
  updatedAt: string
  finished: boolean
  lessonCount: number
}

const rows = ref<MyCourseRow[]>([])
const loading = ref(true)

const courseTitleCache = new Map<number, string>()

async function ensureCourseTitle(courseId: number, fallback?: string): Promise<string> {
  if (courseTitleCache.has(courseId)) return courseTitleCache.get(courseId)!
  if (fallback) {
    courseTitleCache.set(courseId, fallback)
    return fallback
  }
  try {
    const detail = await fetchCourseDetail(courseId)
    const title = detail?.title || `课程 #${courseId}`
    courseTitleCache.set(courseId, title)
    return title
  } catch {
    const fb = `课程 #${courseId}`
    courseTitleCache.set(courseId, fb)
    return fb
  }
}

function progressPct(r: WatchProgress): number {
  if (r.finished === 1) return 100
  const watched = r.durationWatched || r.progress || 0
  if (watched <= 0) return 0
  const pct = Math.min(100, Math.round((watched / 1800) * 100))
  return pct
}

/** 友好时间：近 7 天显示相对时间，更早显示日期 */
function formatTime(iso: string): string {
  if (!iso) return ''
  const t = new Date(iso.replace(' ', 'T'))
  if (Number.isNaN(t.getTime())) return iso.slice(0, 16).replace('T', ' ')
  const diff = Date.now() - t.getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 7) return `${day} 天前`
  const p = (n: number) => String(n).padStart(2, '0')
  return `${t.getFullYear()}-${p(t.getMonth() + 1)}-${p(t.getDate())}`
}

async function load() {
  loading.value = true
  try {
    const [enrolled, records] = await Promise.all([
      fetchMyCourses().catch(() => [] as EnrolledCourse[]),
      fetchMyProgress().catch(() => [] as WatchProgress[]),
    ])

    const progressMap = new Map<number, WatchProgress[]>()
    for (const r of records) {
      if (!progressMap.has(r.courseId)) progressMap.set(r.courseId, [])
      progressMap.get(r.courseId)!.push(r)
    }

    const out: MyCourseRow[] = []
    const seenCourseIds = new Set<number>()

    for (const r of records) {
      if (seenCourseIds.has(r.courseId)) continue
      seenCourseIds.add(r.courseId)
      const list = progressMap.get(r.courseId) || []
      const latest = list[0]
      const finished = list.some((x) => x.finished === 1)
      const title = await ensureCourseTitle(r.courseId)
      out.push({
        courseId: r.courseId,
        title,
        progress: finished ? 100 : progressPct(latest),
        lastLesson: finished
          ? '已学完'
          : latest.lessonTitle
            ? latest.lessonTitle
            : `课时 #${latest.lessonId}`,
        lastLessonId: latest.lessonId,
        updatedAt: latest.updatedAt || '',
        finished,
        lessonCount: 0,
      })
    }

    for (const e of enrolled) {
      if (seenCourseIds.has(e.id)) continue
      seenCourseIds.add(e.id)
      out.push({
        courseId: e.id,
        title: e.title || `课程 #${e.id}`,
        progress: 0,
        lastLesson: '尚未开始',
        lastLessonId: 0,
        updatedAt: '',
        finished: false,
        lessonCount: e.lessonCount || 0,
      })
    }

    rows.value = out
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
    rows.value = []
  } finally {
    loading.value = false
  }
}

const counts = computed(() => ({
  learning: rows.value.filter((r) => !r.finished).length,
  finished: rows.value.filter((r) => r.finished).length,
}))
const filtered = computed(() => rows.value.filter((r) => (tab.value === 'finished' ? r.finished : !r.finished)))

const avgProgress = computed(() => {
  const list = rows.value.filter((r) => !r.finished)
  if (!list.length) return 0
  return Math.round(list.reduce((s, r) => s + r.progress, 0) / list.length)
})

function cont(r: MyCourseRow) {
  if (r.lastLessonId) {
    router.push(`/learn/${r.courseId}/${r.lastLessonId}`)
  } else {
    router.push(`/course/${r.courseId}`)
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container my-page">
    <div class="page-head">
      <h1 class="page-title">我的学习</h1>
    </div>

    <!-- 统计卡 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-num">{{ counts.learning }}</div>
        <div class="stat-label">在学课程</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ counts.finished }}</div>
        <div class="stat-label">已完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ avgProgress }}%</div>
        <div class="stat-label">平均进度</div>
      </div>
    </div>

    <!-- 切换 -->
    <div class="seg">
      <button :class="['seg-btn', { active: tab === 'learning' }]" @click="tab = 'learning'">在学 ({{ counts.learning }})</button>
      <button :class="['seg-btn', { active: tab === 'finished' }]" @click="tab = 'finished'">已完成 ({{ counts.finished }})</button>
    </div>

    <div v-if="loading" class="muted-center">加载中…</div>
    <div v-else-if="filtered.length" class="rows">
      <div v-for="r in filtered" :key="r.courseId" class="row card">
        <div class="row-cover">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
        </div>
        <div class="row-info">
          <div class="row-title">{{ r.title }}</div>
          <div class="row-progress">
            <div class="track"><div class="bar" :style="{ width: r.progress + '%' }"></div></div>
            <span class="pct">{{ r.progress }}%</span>
          </div>
          <div class="row-meta">
            <span>上次学到：{{ r.lastLesson }}</span>
            <span v-if="r.updatedAt">更新于 {{ formatTime(r.updatedAt) }}</span>
          </div>
        </div>
        <button class="btn btn-primary" @click="cont(r)">
          {{ r.finished ? '再学一遍' : '继续学习' }}
        </button>
      </div>
    </div>
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
      <p>这里还没有学习记录，去课程列表逛逛吧</p>
      <button class="btn btn-primary" @click="router.push('/courses')">浏览课程</button>
    </div>
  </div>
</template>

<style scoped>
.my-page { padding: 2.5rem 1.5rem 4rem; }

.page-head { margin-bottom: 1.5rem; }
.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}
@media (max-width: 640px) {
  .stat-grid { grid-template-columns: 1fr; }
}
.stat-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
  text-align: center;
  box-shadow: var(--shadow-sm);
}
.stat-num {
  font-size: 2rem;
  font-weight: 700;
  color: var(--primary);
  line-height: 1;
  margin-bottom: 0.375rem;
}
.stat-label {
  font-size: 13px;
  color: var(--muted-foreground);
}

.seg {
  display: inline-flex;
  background: var(--secondary);
  border-radius: var(--radius);
  padding: 4px;
  gap: 4px;
  margin-bottom: 1.25rem;
}
.seg-btn {
  border: none;
  background: transparent;
  padding: 8px 18px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--muted-foreground);
  cursor: pointer;
  transition: all 0.18s ease;
}
.seg-btn.active {
  background: var(--card);
  color: var(--foreground);
  box-shadow: var(--shadow-sm);
  font-weight: 600;
}

.muted-center {
  padding: 2rem;
  text-align: center;
  color: var(--muted-foreground);
}

.rows { display: flex; flex-direction: column; gap: 0.875rem; }

.row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  transition: box-shadow 0.18s ease;
}
.row:hover { box-shadow: var(--shadow-md); }
.row-cover {
  width: 84px;
  height: 60px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.row-info { flex: 1; min-width: 0; }
.row-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 0.375rem;
}
.track {
  flex: 1;
  height: 6px;
  background: var(--secondary);
  border-radius: 999px;
  overflow: hidden;
}
.bar {
  height: 100%;
  background: var(--primary);
  border-radius: 999px;
  transition: width 0.4s ease;
}
.pct { font-size: 13px; color: var(--muted-foreground); width: 38px; flex-shrink: 0; }
.row-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted-foreground);
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--muted-foreground);
}
.empty-state svg { opacity: 0.3; margin-bottom: 1rem; }
.empty-state p { margin: 0 0 1.25rem; }

@media (max-width: 720px) {
  .row { flex-wrap: wrap; }
  .row .btn { width: 100%; }
}
</style>
