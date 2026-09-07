<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'
import { fetchCourses } from '@/api/courses'
import CourseCard from '@/components/CourseCard.vue'

const courses = ref<CourseCardType[]>([])
const loading = ref(true)
const activeCat = ref('all')

const cats = [
  { key: 'all', label: '全部' },
  { key: 'listening', label: '听力' },
  { key: 'speaking', label: '口语' },
  { key: 'reading', label: '阅读' },
  { key: 'writing', label: '写作' },
  { key: 'sprint', label: '冲分' },
]

const MOCK: CourseCardType[] = [
  { id: 1, title: '雅思口语冲分班 · 7分突破', cover: '', teacherName: '李老师', targetScore: '7+', lessonCount: 16, price: 39900, originalPrice: 59900, inSubscription: true, sales: 1280 },
  { id: 2, title: '雅思写作 7 分进阶', cover: '', teacherName: '王老师', targetScore: '7+', lessonCount: 20, price: 49900, originalPrice: 69900, inSubscription: false, sales: 860 },
  { id: 3, title: '雅思听力基础精讲', cover: '', teacherName: '张老师', targetScore: '6.0', lessonCount: 30, price: 19900, originalPrice: 29900, inSubscription: true, sales: 2100 },
  { id: 4, title: '雅思阅读技巧全解', cover: '', teacherName: '陈老师', targetScore: '6.5', lessonCount: 18, price: 29900, originalPrice: 0, inSubscription: false, sales: 540 },
  { id: 5, title: '雅思口语 Part 2 话题库', cover: '', teacherName: '李老师', targetScore: '6.5', lessonCount: 24, price: 25900, originalPrice: 35900, inSubscription: true, sales: 980 },
  { id: 6, title: '雅思大作文论证模板', cover: '', teacherName: '王老师', targetScore: '6.5', lessonCount: 12, price: 0, originalPrice: 0, inSubscription: true, sales: 3200 },
  { id: 7, title: '雅思听力 section 3-4 攻坚', cover: '', teacherName: '张老师', targetScore: '7+', lessonCount: 14, price: 32900, originalPrice: 0, inSubscription: false, sales: 410 },
  { id: 8, title: '雅思全程冲分班 8 周', cover: '', teacherName: '教研组', targetScore: '7+', lessonCount: 64, price: 129900, originalPrice: 169900, inSubscription: false, sales: 320 },
]

const features = [
  { icon: 'Notebook', title: '考官级教研', desc: '前考官参与的评分标准拆解' },
  { icon: 'VideoCamera', title: '高清加密课程', desc: '私有加密播放，随到随学' },
  { icon: 'TrendCharts', title: '学习进度追踪', desc: '断点续播 + 完播记录' },
  { icon: 'ChatDotRound', title: '答疑与批改', desc: '社群答疑，作文口语批改' },
]

const visibleCourses = computed(() => courses.value)

onMounted(async () => {
  try {
    const page = await fetchCourses({ size: 8 })
    courses.value = page.list.length ? page.list : MOCK
  } catch {
    courses.value = MOCK
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- Hero -->
  <section class="hero">
    <div class="container hero__inner">
      <div class="hero-copy">
        <span class="hero-eyebrow">2026 雅思备考</span>
        <h1>系统化冲分课程<br />让每一分都看得见</h1>
        <p>覆盖听说读写全科，考官级教研团队，从基础到 7+ 的完整提分路径。</p>
        <div class="hero-cta">
          <RouterLink to="/courses"><el-button type="primary" size="large" round>浏览课程</el-button></RouterLink>
        </div>
        <div class="hero-stats">
          <div><b>12,000+</b><span>累计学员</span></div>
          <div><b>4.9</b><span>课程评分</span></div>
          <div><b>120+</b><span>精品课时</span></div>
        </div>
      </div>
      <div class="hero-art">
        <div class="art-card art-1"><el-icon :size="22"><Headset /></el-icon><span>听力</span></div>
        <div class="art-card art-2"><el-icon :size="22"><Microphone /></el-icon><span>口语</span></div>
        <div class="art-card art-3"><el-icon :size="22"><Reading /></el-icon><span>阅读</span></div>
        <div class="art-card art-4"><el-icon :size="22"><EditPen /></el-icon><span>写作</span></div>
      </div>
    </div>
  </section>

  <!-- Features -->
  <section class="container features">
    <div v-for="f in features" :key="f.title" class="feature">
      <div class="feature-icon"><el-icon :size="22"><component :is="f.icon" /></el-icon></div>
      <div>
        <div class="feature-title">{{ f.title }}</div>
        <div class="feature-desc">{{ f.desc }}</div>
      </div>
    </div>
  </section>

  <!-- Courses -->
  <section class="container">
    <div class="section-head">
      <h2>精选课程</h2>
      <RouterLink to="/courses" class="more">查看全部 →</RouterLink>
    </div>

    <div class="cats">
      <button
        v-for="c in cats"
        :key="c.key"
        :class="['cat', { active: activeCat === c.key }]"
        @click="activeCat = c.key"
      >
        {{ c.label }}
      </button>
    </div>

    <div v-if="loading" class="grid">
      <el-skeleton v-for="i in 8" :key="i" :rows="3" animated style="height: 280px" />
    </div>
    <div v-else class="grid">
      <CourseCard v-for="c in visibleCourses" :key="c.id" :course="c" />
    </div>
  </section>
</template>

<style scoped>
.hero {
  background: linear-gradient(160deg, var(--brand-50) 0%, #ffffff 60%);
  border-bottom: 1px solid var(--line);
}
.hero__inner {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 40px;
  padding: 56px 20px 48px;
  align-items: center;
}
.hero-eyebrow {
  display: inline-block;
  font-size: 13px;
  color: var(--brand-600);
  background: var(--brand-100);
  padding: 4px 12px;
  border-radius: 999px;
  margin-bottom: 16px;
}
.hero-copy h1 {
  font-size: 40px;
  line-height: 1.25;
  letter-spacing: -0.5px;
}
.hero-copy > p {
  margin: 16px 0 24px;
  font-size: 16px;
  color: var(--text-2);
  max-width: 520px;
}
.hero-cta {
  display: flex;
  gap: 12px;
}
.hero-stats {
  display: flex;
  gap: 40px;
  margin-top: 36px;
}
.hero-stats b {
  display: block;
  font-size: 26px;
  color: var(--brand-700);
}
.hero-stats span {
  font-size: 13px;
  color: var(--text-3);
}
.hero-art {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  align-content: center;
}
.art-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 18px;
  border-radius: var(--r-lg);
  background: var(--bg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
  font-weight: 600;
  color: var(--brand-700);
}
.art-card .el-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--r-md);
  background: var(--brand-50);
  color: var(--brand-500);
  display: flex;
  align-items: center;
  justify-content: center;
}
.art-2 { transform: translateY(16px); }
.art-3 { transform: translateY(-16px); }
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 32px;
}
.feature {
  display: flex;
  gap: 12px;
  align-items: center;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 16px;
}
.feature-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: var(--r-md);
  background: var(--brand-50);
  color: var(--brand-500);
  display: flex;
  align-items: center;
  justify-content: center;
}
.feature-title {
  font-weight: 600;
  font-size: 15px;
}
.feature-desc {
  font-size: 13px;
  color: var(--text-3);
}
.cats {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.cat {
  border: 1px solid var(--line-strong);
  background: var(--bg);
  color: var(--text-2);
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 14px;
  transition: all 0.15s;
}
.cat:hover {
  border-color: var(--brand-300);
  color: var(--brand-600);
}
.cat.active {
  background: var(--brand-500);
  border-color: var(--brand-500);
  color: #fff;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
@media (max-width: 980px) {
  .hero__inner {
    grid-template-columns: 1fr;
  }
  .features,
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 480px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .features {
    grid-template-columns: 1fr;
  }
}
</style>
