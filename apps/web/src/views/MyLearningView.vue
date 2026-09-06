<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tab = ref('learning')

interface MyCourseRow {
  courseId: number
  title: string
  progress: number
  lastLesson: string
  lastLessonId: number
  source: string
  expire: string
  status: 'learning' | 'finished' | 'expired'
}

const rows: MyCourseRow[] = [
  { courseId: 1, title: '雅思口语冲分班 · 7分突破', progress: 38, lastLesson: '2.1 Work & Study', lastLessonId: 21, source: '已购买', expire: '剩 348 天', status: 'learning' },
  { courseId: 3, title: '雅思听力基础精讲', progress: 72, lastLesson: '4.2 地图题技巧', lastLessonId: 42, source: '已购买', expire: '剩 210 天', status: 'learning' },
  { courseId: 5, title: '雅思口语 Part 2 话题库', progress: 100, lastLesson: '已学完', lastLessonId: 51, source: '已购买', expire: '永久', status: 'finished' },
  { courseId: 9, title: '雅思基础语法', progress: 45, lastLesson: '3.1 时态', lastLessonId: 31, source: '已购买', expire: '已过期', status: 'expired' },
]

const counts = computed(() => ({
  learning: rows.filter((r) => r.status === 'learning').length,
  finished: rows.filter((r) => r.status === 'finished').length,
  expired: rows.filter((r) => r.status === 'expired').length,
}))
const filtered = computed(() => rows.filter((r) => r.status === tab.value))

function cont(r: MyCourseRow) {
  if (r.status === 'expired') router.push(`/course/${r.courseId}`)
  else router.push(`/learn/${r.courseId}/${r.lastLessonId}`)
}
</script>

<template>
  <div class="container my-page">
    <h1 class="page-title">我的学习</h1>

    <el-tabs v-model="tab" class="my-tabs">
      <el-tab-pane :label="`在学 (${counts.learning})`" name="learning" />
      <el-tab-pane :label="`已完成 (${counts.finished})`" name="finished" />
      <el-tab-pane :label="`已过期 (${counts.expired})`" name="expired" />
    </el-tabs>

    <div v-if="filtered.length" class="rows">
      <div v-for="r in filtered" :key="r.courseId" class="row card">
        <div class="row-cover"><el-icon :size="22" color="#fff"><Reading /></el-icon></div>
        <div class="row-info">
          <div class="row-top">
            <span class="row-title">{{ r.title }}</span>
            <span :class="['src', { sub: r.source === '订阅中' }]">{{ r.source }}</span>
          </div>
          <div class="row-progress">
            <el-progress :percentage="r.progress" :stroke-width="6" :show-text="false" />
            <span class="pct">{{ r.progress }}%</span>
          </div>
          <div class="row-meta">
            <span>上次学到：{{ r.lastLesson }}</span>
            <span :class="['expire', { warn: r.status === 'expired' }]">{{ r.expire }}</span>
          </div>
        </div>
        <el-button
          :type="r.status === 'expired' ? 'default' : 'primary'"
          round
          class="row-btn"
          @click="cont(r)"
        >
          {{ r.status === 'finished' ? '再学一遍' : r.status === 'expired' ? '去续费' : '继续学习' }}
        </el-button>
      </div>
    </div>
    <el-empty v-else description="这里还没有课程，去逛逛吧" />
  </div>
</template>

<style scoped>
.my-page { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 8px; }
.rows { display: flex; flex-direction: column; gap: 14px; margin-top: 8px; }
.card { background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-sm); }
.row { display: flex; align-items: center; gap: 16px; padding: 16px 18px; }
.row-cover {
  width: 84px; height: 60px; border-radius: var(--r-sm); flex-shrink: 0;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  display: flex; align-items: center; justify-content: center;
}
.row-info { flex: 1; min-width: 0; }
.row-top { display: flex; align-items: center; gap: 10px; }
.row-title { font-weight: 600; }
.src { font-size: 12px; color: var(--text-3); background: var(--bg-mute); padding: 2px 8px; border-radius: var(--r-sm); }
.src.sub { color: var(--accent); background: rgba(22, 184, 166, 0.1); }
.row-progress { display: flex; align-items: center; gap: 10px; margin: 10px 0 6px; }
.row-progress .el-progress { flex: 1; }
.pct { font-size: 13px; color: var(--text-3); width: 38px; }
.row-meta { display: flex; justify-content: space-between; font-size: 13px; color: var(--text-3); }
.expire.warn { color: var(--danger); }
.row-btn { flex-shrink: 0; }
@media (max-width: 720px) {
  .row { flex-wrap: wrap; }
  .row-btn { width: 100%; }
}
</style>
