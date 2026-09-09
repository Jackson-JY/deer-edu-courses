<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDashboardStats } from '@/api/admin'

interface CourseRank { courseId: number; title: string; learners: number; watchMinutes: number }
interface UserRank { userId: number; nickname: string; phone: string; watchMinutes: number; finishedLessons: number }

interface DashboardData {
  courseCount: number
  onlineCourseCount: number
  lessonCount: number
  userCount: number
  activeEntitlementCount: number
  activeUserCount: number
  totalWatchMinutes: number
  finishedLessonCount: number
  activeLearnerCount: number
  courseRanking: CourseRank[]
  userRanking: UserRank[]
}

const data = ref<DashboardData | null>(null)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    data.value = await getDashboardStats()
  } finally {
    loading.value = false
  }
}

function formatMinutes(min: number): string {
  if (min < 60) return `${min} 分钟`
  const h = Math.floor(min / 60)
  const m = min % 60
  return m > 0 ? `${h} 小时 ${m} 分` : `${h} 小时`
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <h1 class="page-title">工作台</h1>

    <!-- 基础数据 -->
    <h2 class="section-title">平台概览</h2>
    <div class="stats">
      <div class="stat-card">
        <div class="stat-value">{{ data?.courseCount ?? 0 }}</div>
        <div class="stat-label">课程总数</div>
        <div class="stat-sub">已上架 {{ data?.onlineCourseCount ?? 0 }} 门</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data?.lessonCount ?? 0 }}</div>
        <div class="stat-label">课时总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data?.userCount ?? 0 }}</div>
        <div class="stat-label">注册用户</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ data?.activeUserCount ?? 0 }}</div>
        <div class="stat-label">有效学员</div>
        <div class="stat-sub">权益 {{ data?.activeEntitlementCount ?? 0 }} 条</div>
      </div>
    </div>

    <!-- 学习情况大盘 -->
    <h2 class="section-title">学习情况大盘</h2>
    <div class="stats">
      <div class="stat-card stat-accent">
        <div class="stat-value">{{ data?.activeLearnerCount ?? 0 }}</div>
        <div class="stat-label">活跃学员</div>
        <div class="stat-sub">有过观看行为</div>
      </div>
      <div class="stat-card stat-accent">
        <div class="stat-value">{{ formatMinutes(data?.totalWatchMinutes ?? 0) }}</div>
        <div class="stat-label">累计观看时长</div>
      </div>
      <div class="stat-card stat-accent">
        <div class="stat-value">{{ data?.finishedLessonCount ?? 0 }}</div>
        <div class="stat-label">已学完课时</div>
      </div>
    </div>

    <!-- 排行榜 -->
    <h2 class="section-title">学习排行榜</h2>
    <div class="ranking-grid">
      <div class="ranking-card">
        <h3 class="ranking-title">学员观看时长榜</h3>
        <el-table :data="data?.userRanking ?? []" stripe style="width: 100%" empty-text="暂无学习数据">
          <el-table-column type="index" label="名次" width="60">
            <template #default="{ $index }">
              <span :class="['rank-medal', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="学员" min-width="140">
            <template #default="{ row }">
              <div class="rank-user">{{ row.nickname }}</div>
              <div class="rank-phone">{{ row.phone || 'UID' + row.userId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="观看时长" width="110" align="center">
            <template #default="{ row }">{{ formatMinutes(row.watchMinutes) }}</template>
          </el-table-column>
          <el-table-column prop="finishedLessons" label="学完课时" width="90" align="center" />
        </el-table>
      </div>

      <div class="ranking-card">
        <h3 class="ranking-title">课程学习榜</h3>
        <el-table :data="data?.courseRanking ?? []" stripe style="width: 100%" empty-text="暂无学习数据">
          <el-table-column type="index" label="名次" width="60">
            <template #default="{ $index }">
              <span :class="['rank-medal', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="课程名称" min-width="150" />
          <el-table-column prop="learners" label="学习人数" width="90" align="center" />
          <el-table-column label="观看时长" width="110" align="center">
            <template #default="{ row }">{{ formatMinutes(row.watchMinutes) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; letter-spacing: -0.01em; }
.section-title { font-size: 16px; font-weight: 600; margin: 28px 0 14px; color: var(--foreground); }

.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1024px) { .stats { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .stats { grid-template-columns: 1fr; } }

.stat-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.18s ease;
}
.stat-card:hover { box-shadow: var(--shadow-md); }
.stat-value { font-size: 1.85rem; font-weight: 700; color: var(--primary); line-height: 1.1; }
.stat-label { font-size: 13px; color: var(--muted-foreground); margin-top: 6px; }
.stat-sub { font-size: 12px; color: var(--muted-foreground); margin-top: 4px; opacity: 0.8; }

.stat-accent .stat-value { color: var(--success, #10b981); }

.ranking-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (max-width: 1024px) { .ranking-grid { grid-template-columns: 1fr; } }

.ranking-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}
.ranking-title { font-size: 14px; font-weight: 600; margin: 0 0 12px; color: var(--foreground); }
.rank-user { font-size: 13px; font-weight: 500; color: var(--foreground); line-height: 1.3; }
.rank-phone { font-size: 12px; color: var(--muted-foreground); line-height: 1.3; }
.rank-medal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: var(--secondary);
  color: var(--muted-foreground);
}
.rank-medal.rank-1 { background: #f59e0b; color: #fff; }
.rank-medal.rank-2 { background: #9ca3af; color: #fff; }
.rank-medal.rank-3 { background: #d08a5e; color: #fff; }
</style>
