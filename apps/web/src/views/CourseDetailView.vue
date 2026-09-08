<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { CourseDetail } from '@ielts/shared-types'
import { fetchCourseDetail } from '@/api/courses'

const route = useRoute()
const router = useRouter()
const detail = ref<CourseDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')

const ctaText = computed(() => {
  switch (detail.value?.ctaState) {
    case 'LEARN': return '开始 / 继续学习'
    case 'EXPIRED': return '已过期 · 联系管理员续费'
    case 'SUBSCRIBE': return '订阅会员可看'
    default: return '联系管理员开通'
  }
})

function fmtDur(s: number) {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    detail.value = await fetchCourseDetail(id)
  } catch (e: any) {
    errorMsg.value = e?.message || '课程加载失败'
  } finally {
    loading.value = false
  }
})

function onCta() {
  const d = detail.value
  if (!d) return
  if (d.ctaState === 'LEARN') {
    const first = d.chapters[0]?.lessons[0]
    if (first) router.push(`/learn/${d.id}/${first.id}`)
  } else if (d.ctaState === 'SUBSCRIBE') {
    router.push('/subscribe')
  }
}
function playLesson(lessonId: number, locked: boolean, trial: boolean) {
  if (!locked || trial) router.push(`/learn/${detail.value!.id}/${lessonId}`)
}
</script>

<template>
  <div v-if="loading" class="detail-loading">加载中…</div>
  <div v-else-if="errorMsg" class="detail-error">
    <p>{{ errorMsg }}</p>
    <button class="btn btn-primary" @click="router.push('/courses')">返回课程列表</button>
  </div>
  <template v-else-if="detail">
    <!-- 头图区 -->
    <section class="course-hero-wrap">
      <div class="page-container">
        <a class="back-link" href="javascript:history.back()">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          全部课程
        </a>
        <section class="course-hero">
          <div class="preview-media">
            <div class="play-button">
              <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            </div>
          </div>
          <div class="hero-info">
            <div class="eyebrow">IELTS {{ detail.targetScore }} · {{ detail.chapters.length }} 章</div>
            <h1>{{ detail.title }}</h1>
            <p class="tagline">{{ detail.intro ? detail.intro.slice(0, 60) + '…' : '' }}</p>
            <div class="hero-actions">
              <button v-if="detail.ctaState === 'LEARN' || detail.ctaState === 'SUBSCRIBE'" class="btn btn-primary btn-lg" @click="onCta">{{ ctaText }}</button>
            </div>
            <div class="rating-row">
              <span>{{ detail.sales }} 位学员</span>
            </div>
          </div>
        </section>
      </div>
    </section>

    <div class="page-container page-layout">
      <div class="main-content">
        <!-- 课程大纲 -->
        <section class="card block">
          <h2 class="block-title">课程大纲</h2>
          <div v-for="ch in detail.chapters" :key="ch.id" class="chapter">
            <div class="chapter-title">{{ ch.title }}</div>
            <div
              v-for="ls in ch.lessons"
              :key="ls.id"
              :class="['lesson', { playable: !ls.locked || ls.freeTrial }]"
              @click="playLesson(ls.id, ls.locked, ls.freeTrial)"
            >
              <svg class="l-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon v-if="!ls.locked || ls.freeTrial" points="5 3 19 12 5 21 5 3" fill="currentColor"/>
                <rect v-else x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path v-if="ls.locked && !ls.freeTrial" d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <span class="l-title">{{ ls.title }}</span>
              <span v-if="ls.freeTrial" class="tag tag-trial">试看</span>
              <span class="l-dur">{{ fmtDur(ls.duration) }}</span>
            </div>
          </div>
        </section>

        <!-- 课件 -->
        <section class="card block">
          <h2 class="block-title">配套课件 <span class="hint">仅在线预览，不支持下载</span></h2>
          <div class="materials">
            <div v-for="m in detail.materials" :key="m.id" class="material">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <span>{{ m.name }}</span>
              <span class="m-preview">预览</span>
            </div>
          </div>
        </section>

        <!-- 介绍 -->
        <section class="card block">
          <h2 class="block-title">课程介绍</h2>
          <p class="intro-text">{{ detail.intro || '暂无介绍' }}</p>
        </section>
      </div>
    </div>
  </template>
</template>

<style scoped>
.detail-loading { padding: 4rem; text-align: center; color: var(--muted-foreground); }
.detail-error { padding: 4rem; text-align: center; color: var(--muted-foreground); }
.detail-error p { margin-bottom: 1rem; }

.course-hero-wrap {
  padding-top: 2rem;
  background: linear-gradient(180deg, var(--background-50), var(--background-200));
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--muted-foreground);
  text-decoration: none;
  margin-bottom: 1rem;
  transition: color 0.18s ease;
}
.back-link:hover { color: var(--primary); }

.course-hero {
  display: grid;
  grid-template-columns: minmax(280px, 420px) 1fr;
  gap: 2.5rem;
  padding: 2rem 0 3rem;
  align-items: center;
}
@media (max-width: 991px) {
  .course-hero { grid-template-columns: 1fr; }
}
.preview-media {
  aspect-ratio: 16 / 10;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--brand-400), var(--brand-700));
  position: relative;
  cursor: pointer;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}
.play-button {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
  transition: transform 0.18s ease;
}
.preview-media:hover .play-button { transform: scale(1.1); }

.hero-info .eyebrow {
  color: var(--muted-foreground);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 0.75rem;
}
.hero-info h1 {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  margin: 0 0 1rem;
}
.tagline {
  color: var(--muted-foreground);
  font-size: 1rem;
  margin: 0 0 1.5rem;
}
.hero-info .price {
  font-size: 2rem;
  font-weight: 700;
  color: var(--destructive);
  margin-bottom: 1.5rem;
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.rating-row {
  font-size: 14px;
  color: var(--muted-foreground);
}

.page-layout {
  max-width: 800px;
  padding: 3rem 1.5rem 5rem;
}

.block {
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.block-title {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 1rem;
}
.hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted-foreground);
  margin-left: 8px;
}

.chapter { margin-bottom: 1rem; }
.chapter-title {
  font-size: 14px;
  color: var(--muted-foreground);
  font-weight: 600;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
  margin-bottom: 0.5rem;
}
.lesson {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 8px;
  border-radius: var(--radius-sm);
  color: var(--muted-foreground);
  font-size: 14px;
  transition: background 0.15s;
}
.lesson.playable { color: var(--foreground); cursor: pointer; }
.lesson.playable:hover { background: var(--secondary); }
.l-icon { flex-shrink: 0; color: var(--icon-muted); }
.lesson.playable .l-icon { color: var(--primary); }
.l-title { flex: 1; }
.l-dur { font-size: 13px; color: var(--muted-foreground); }

.materials { display: flex; flex-direction: column; gap: 10px; }
.material {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
}
.material svg { color: var(--primary); flex-shrink: 0; }
.material span:nth-child(2) { flex: 1; }
.m-preview { font-size: 12px; color: var(--primary); }

.intro-text { color: var(--text-600, var(--foreground)); line-height: 1.8; margin: 0; }
</style>
