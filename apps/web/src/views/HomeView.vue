<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'
import { fetchCourses } from '@/api/courses'
import CourseCard from '@/components/CourseCard.vue'

const courses = ref<CourseCardType[]>([])
const loading = ref(true)

const cats = [
  { key: 'listening', label: '听力' },
  { key: 'speaking', label: '口语' },
  { key: 'reading', label: '阅读' },
  { key: 'writing', label: '写作' },
  { key: 'sprint', label: '冲分班' },
]

const stats = [
  { num: '5年+', label: '全科教学经验' },
  { num: '剑桥', label: 'CELTA 认证' },
  { num: '6.5+', label: '冲刺提分目标' },
  { num: '7+', label: '口语写作突破' },
]

const teacherCreds = [
  { icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z', label: '雅思 8 分' },
  { icon: 'M5 13l4 4L19 7', label: '剑桥 CELTA 证书' },
  { icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253', label: '211 & 双一流 理科背景' },
  { icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4', label: '中传 · 中财 · 北二外 授课' },
]

const teacherAchievements = [
  '多名学员从 6 分冲刺 7-8 分',
  '口语写作从 5.5 → 7+ 案例众多',
  '学员考入剑桥、UCL、UAL 等世界名校',
  '每天 6am 早起直播备课 110 天不间断',
]

const features = [
  { icon: 'M9 12l2 2 4-4', title: '考官级教研', desc: '前考官参与评分标准拆解' },
  { icon: 'M15 10l4.553-2.276A1 1 0 0 1 21 8.618v6.764a1 1 0 0 1-1.447.894L15 14M5 18h8a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2z', title: '高清加密课程', desc: '私有加密播放，随到随学' },
  { icon: 'M3 3v18h18M7 14l3-3 3 3 5-5', title: '学习进度追踪', desc: '断点续播 + 完播记录' },
  { icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 0 1-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z', title: '答疑与批改', desc: '社群答疑，作文口语批改' },
]

const visibleCourses = computed(() => courses.value.slice(0, 8))

onMounted(async () => {
  try {
    const page = await fetchCourses({ size: 8 })
    courses.value = page.list.length ? page.list : []
  } catch {
    courses.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- Hero -->
  <section class="hero">
    <div class="hero-inner">
      <div class="hero-copy">
        <div class="hero-eyebrow">Cris · 雅思全科提分导师</div>
        <h1>从 6 分到 7+<br />一套有方法的系统</h1>
        <p class="subhead">211 理科自学雅思 8 分，剑桥 CELTA 认证，5 年全科教学，曾在中传、中财、北二外等多所高校授课。多名学员考入剑桥、UCL、UAL。</p>
        <div class="hero-cta">
          <RouterLink to="/courses" class="btn btn-primary btn-lg">浏览课程</RouterLink>
          <a href="#about-teacher" class="btn btn-secondary btn-lg">了解 Cris</a>
        </div>
      </div>
      <div class="hero-visual">
        <div class="visual-card">
          <div class="visual-blob blob-1"></div>
          <div class="visual-blob blob-2"></div>
          <div class="visual-card-content">
            <div class="badge-row">
              <span class="badge">雅思 8 分</span>
              <span class="badge badge-outline">剑桥 CELTA</span>
            </div>
            <div class="title">听说读写 全科提分</div>
            <div class="score-row">
              <span>6.5</span>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
              <span>7.0+</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- 统计带 -->
  <section class="stats-band">
    <div class="stats">
      <div v-for="s in stats" :key="s.label" class="stat">
        <div class="stat-num">{{ s.num }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>
  </section>

  <!-- 分类快捷入口 -->
  <section class="section">
    <div class="chips">
      <RouterLink v-for="c in cats" :key="c.key" :to="`/courses?cat=${c.key}`" class="chip">
        {{ c.label }}
      </RouterLink>
    </div>
  </section>

  <!-- 精选课程 -->
  <section class="section">
    <div class="section-header">
      <h2>Cris 老师主讲课程</h2>
      <RouterLink to="/courses" class="section-link">查看全部 →</RouterLink>
    </div>
    <div class="course-grid">
      <div v-if="loading" class="grid-skeleton">
        <div v-for="i in 8" :key="i" class="skeleton-card"></div>
      </div>
      <div v-else class="grid grid-4">
        <CourseCard v-for="c in visibleCourses" :key="c.id" :course="c" />
      </div>
    </div>
  </section>

  <!-- 关于主讲老师 -->
  <section id="about-teacher" class="section-alt teacher-section">
    <div class="section teacher-wrapper">
      <div class="teacher-photo">
        <img src="/teacher-cris.jpg" alt="Cris 老师" />
        <div class="teacher-photo-badge">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          剑桥 CELTA 认证
        </div>
      </div>

      <div class="teacher-info">
        <div class="teacher-meta">主讲老师</div>
        <h2>Cris · 雅思全科提分导师</h2>
        <p class="teacher-intro">
          211 & 双一流理科生，自学雅思 8 分后转战教学。持有剑桥 CELTA 证书，知名机构雅思听说读写教学经验 5 年，
          曾在中国传媒大学、中央财经大学、北京第二外国语学院等北京多所高校教授雅思全科。
          擅长冲刺总分 6.5 分，口语写作 7 分入名校。
        </p>

        <div class="teacher-creds">
          <div v-for="c in teacherCreds" :key="c.label" class="cred-item">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path :d="c.icon"/></svg>
            <span>{{ c.label }}</span>
          </div>
        </div>

        <h3 class="teacher-subtitle">教学成果</h3>
        <ul class="teacher-achievements">
          <li v-for="a in teacherAchievements" :key="a">{{ a }}</li>
        </ul>

        <div class="teacher-contact">
          <div class="contact-row">
            <span class="contact-label">小红书</span>
            <a href="https://www.xiaohongshu.com/search_result?keyword=Cris教雅思" target="_blank" rel="noopener" class="contact-chip contact-xhs">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 2a10 10 0 100 20 10 10 0 000-20zm-1.67 4.5c.92 0 1.67.75 1.67 1.67s-.75 1.67-1.67 1.67S8.67 9.08 8.67 8.17 9.42 6.5 10.33 6.5zm3.34 0c.92 0 1.67.75 1.67 1.67s-.75 1.67-1.67 1.67-1.67-.75-1.67-1.67.75-1.67 1.67-1.67zM12 18c-1.5 0-3.11-.56-4-1.5-.11-.11-.11-.29 0-.41l2-2c.12-.11.3-.11.41 0 .5.5 1.09.75 1.59.75s1.09-.25 1.59-.75c.12-.11.3-.11.41 0l2 2c.12.12.12.3 0 .41-.89.94-2.5 1.5-4 1.5z"/></svg>
              Cris教雅思
            </a>
            <a href="https://www.xiaohongshu.com/search_result?keyword=Cris" target="_blank" rel="noopener" class="contact-chip contact-xhs">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 2a10 10 0 100 20 10 10 0 000-20zm-1.67 4.5c.92 0 1.67.75 1.67 1.67s-.75 1.67-1.67 1.67S8.67 9.08 8.67 8.17 9.42 6.5 10.33 6.5zm3.34 0c.92 0 1.67.75 1.67 1.67s-.75 1.67-1.67 1.67-1.67-.75-1.67-1.67.75-1.67 1.67-1.67zM12 18c-1.5 0-3.11-.56-4-1.5-.11-.11-.11-.29 0-.41l2-2c.12-.11.3-.11.41 0 .5.5 1.09.75 1.59.75s1.09-.25 1.59-.75c.12-.11.3-.11.41 0l2 2c.12.12.12.3 0 .41-.89.94-2.5 1.5-4 1.5z"/></svg>
              Cris.
            </a>
          </div>
          <div class="contact-row">
            <span class="contact-label">邮箱</span>
            <a href="mailto:ccrisss@icloud.com" class="contact-chip">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></svg>
              ccrisss@icloud.com
            </a>
          </div>
          <div class="contact-tip">微信可通过小红书主页扫码添加</div>
        </div>
      </div>
    </div>
  </section>

  <!-- 特色 -->
  <section class="section">
    <div class="section-header">
      <h2>为什么选择我们</h2>
    </div>
    <div class="features grid grid-4">
      <article v-for="f in features" :key="f.title" class="card feature-card">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="f.icon" />
          </svg>
        </div>
        <div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  background: linear-gradient(180deg, var(--background-50) 0%, var(--background-200) 100%);
  overflow: hidden;
}
.hero-inner {
  width: min(1200px, 100%);
  margin: 0 auto;
  padding: 6rem 1.5rem 5rem;
  display: grid;
  grid-template-columns: 1fr 0.9fr;
  gap: 3rem;
  align-items: center;
}
@media (max-width: 991px) {
  .hero-inner {
    grid-template-columns: 1fr;
    padding: 4rem 1.5rem;
    text-align: center;
  }
}
.hero-eyebrow {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  background: var(--accent);
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 1.25rem;
}
.hero-copy h1 {
  font-size: clamp(2.5rem, 6vw, 4.5rem);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.03em;
  margin: 0 0 1.25rem;
}
.hero-copy .subhead {
  font-size: clamp(1rem, 2vw, 1.15rem);
  color: var(--muted-foreground);
  margin: 0 0 2rem;
  line-height: 1.7;
  max-width: 540px;
}
@media (max-width: 991px) {
  .hero-copy .subhead { margin-left: auto; margin-right: auto; }
}
.hero-cta {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
}
@media (max-width: 991px) { .hero-cta { justify-content: center; } }

.hero-visual { display: flex; justify-content: center; }
.visual-card {
  position: relative;
  width: min(420px, 100%);
  aspect-ratio: 4 / 3;
  border-radius: var(--radius);
  background: var(--card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg), 0 32px 64px -24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 1.5rem;
}
.visual-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.55;
}
.blob-1 { width: 220px; height: 220px; top: -40px; right: -40px; background: var(--brand-300); }
.blob-2 { width: 160px; height: 160px; bottom: 20px; left: -30px; background: var(--brand-200); }
.visual-card-content { position: relative; z-index: 2; }
.badge-row { display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--primary);
  color: var(--primary-foreground);
}
.badge-outline {
  background: transparent;
  color: var(--foreground);
  border: 1px solid var(--border);
}
.visual-card-content .title { font-size: 1.5rem; font-weight: 700; margin: 0.25rem 0 0.75rem; }
.score-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--muted-foreground);
}
.score-row svg { color: var(--primary); }
.score-row span:last-child { color: var(--primary); }

.stats-band { padding: 3rem 1.5rem; }
.stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2rem;
  text-align: center;
}
@media (min-width: 768px) { .stats { grid-template-columns: repeat(4, 1fr); } }
.stat-num {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}
.stat-label {
  margin-top: 0.5rem;
  font-size: 13px;
  color: var(--muted-foreground);
}

.chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 0.75rem; }
.chip {
  display: inline-flex; align-items: center; height: 44px; padding: 0 20px;
  border-radius: 999px; background: var(--secondary); color: var(--secondary-foreground);
  font-size: 14px; font-weight: 600; text-decoration: none;
  transition: background-color 0.18s ease, color 0.18s ease;
}
.chip:hover { background: var(--primary); color: var(--primary-foreground); }

.section-link { font-size: 14px; color: var(--muted-foreground); text-decoration: none; }
.section-link:hover { color: var(--primary); }

.grid { display: grid; gap: 1.5rem; }
.grid-4 { grid-template-columns: repeat(1, minmax(0, 1fr)); }
@media (min-width: 768px) { .grid-4 { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (min-width: 1024px) { .grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); } }

.grid-skeleton { display: grid; gap: 1.5rem; grid-template-columns: repeat(1, minmax(0, 1fr)); }
@media (min-width: 768px) { .grid-skeleton { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (min-width: 1024px) { .grid-skeleton { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
.skeleton-card {
  aspect-ratio: 4 / 5;
  border-radius: var(--radius);
  background: linear-gradient(90deg, var(--background-200) 25%, var(--background-100) 50%, var(--background-200) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.section-alt { background: var(--background-100); }
.section-alt .section { background: transparent; }

/* === 老师介绍板块 === */
.teacher-section { padding: 4rem 0; }
.teacher-wrapper {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 3rem;
  align-items: start;
}
@media (max-width: 991px) {
  .teacher-wrapper { grid-template-columns: 1fr; gap: 2rem; text-align: center; }
}
.teacher-photo { position: relative; }
.teacher-photo img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
}
.teacher-photo-badge {
  position: absolute;
  left: 50%;
  bottom: -16px;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--primary);
  color: var(--primary-foreground);
  font-size: 12px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 999px;
  box-shadow: var(--shadow-md);
  white-space: nowrap;
}
@media (max-width: 991px) { .teacher-photo { max-width: 280px; margin: 0 auto; } }

.teacher-meta {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 8px;
}
.teacher-info h2 {
  font-size: clamp(1.75rem, 3.5vw, 2.5rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 1rem;
}
.teacher-intro {
  font-size: 15px;
  line-height: 1.8;
  color: var(--foreground);
  margin: 0 0 1.5rem;
}
@media (max-width: 991px) {
  .teacher-intro { text-align: left; }
}

.teacher-creds {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 14px;
  margin-bottom: 2rem;
}
@media (max-width: 640px) { .teacher-creds { grid-template-columns: 1fr; } }
.cred-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--foreground);
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
}
.cred-item svg { color: var(--primary); flex-shrink: 0; }

.teacher-subtitle {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 10px;
}
.teacher-achievements {
  list-style: none;
  padding: 0;
  margin: 0 0 1.75rem;
  display: grid;
  gap: 6px;
}
@media (max-width: 991px) { .teacher-achievements { text-align: left; } }
.teacher-achievements li {
  font-size: 14px;
  color: var(--muted-foreground);
  padding-left: 18px;
  position: relative;
  line-height: 1.6;
}
.teacher-achievements li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: var(--primary);
  font-weight: 700;
}

.teacher-contact {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
}
.contact-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.contact-row:last-of-type { margin-bottom: 0; }
.contact-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted-foreground);
  min-width: 40px;
}
.contact-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: var(--secondary);
  color: var(--foreground);
  text-decoration: none;
  transition: background 0.18s ease;
}
.contact-chip:hover { background: var(--primary); color: var(--primary-foreground); }
.contact-xhs { background: #ff2442; color: #fff; }
.contact-xhs:hover { background: #e01b36; color: #fff; }
.contact-tip {
  font-size: 12px;
  color: var(--muted-foreground);
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}

/* === 特色 === */
.features { gap: 1rem; }
.feature-card {
  flex-direction: row;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.5rem;
}
.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--accent);
  color: var(--primary);
}
.feature-icon svg { width: 24px; height: 24px; }
.feature-title { font-size: 16px; font-weight: 600; margin: 0 0 4px; }
.feature-desc { margin: 0; font-size: 13px; color: var(--muted-foreground); }
</style>
