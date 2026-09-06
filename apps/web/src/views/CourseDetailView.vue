<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { CourseDetail } from '@ielts/shared-types'
import { fetchCourseDetail } from '@/api/courses'
import { features } from '@/config'

const route = useRoute()
const router = useRouter()
const detail = ref<CourseDetail | null>(null)
const loading = ref(true)

const MOCK: CourseDetail = {
  id: 1,
  title: '雅思口语冲分班 · 7分突破',
  cover: '',
  intro: '本课程由前雅思口语考官领衔教研，围绕评分四维度（流利度、词汇、语法、发音）系统拆解，配合 Part 1/2/3 高频话题库与高分范例，帮助你在 8 周内实现口语 7+ 的稳定输出。',
  teacher: { id: 1, name: '李老师', avatar: '', intro: '前雅思口语考官，10 年教龄，累计授课 5000+ 小时。' },
  targetScore: '7+',
  price: 39900,
  originalPrice: 59900,
  validityType: 'DAYS',
  validityDays: 365,
  fixedExpireAt: null,
  inSubscription: true,
  sales: 1280,
  ctaState: 'BUY',
  chapters: [
    { id: 1, title: '第一章 · 口语评分标准', lessons: [
      { id: 11, title: '1.1 评分四维度全解', duration: 500, freeTrial: true, locked: false, progress: 0 },
      { id: 12, title: '1.2 高分范例拆解', duration: 845, freeTrial: false, locked: true, progress: 0 },
    ]},
    { id: 2, title: '第二章 · Part 1 高频话题', lessons: [
      { id: 21, title: '2.1 Work & Study', duration: 1000, freeTrial: false, locked: true, progress: 0 },
      { id: 22, title: '2.2 Hometown & Hobbies', duration: 920, freeTrial: false, locked: true, progress: 0 },
    ]},
  ],
  materials: [
    { id: 1, name: '口语评分标准详解.pdf', type: 'PDF', previewOnly: true },
    { id: 2, name: '高分范例音频合集', type: 'AUDIO', previewOnly: true },
  ],
}

const ctaText = computed(() => {
  switch (detail.value?.ctaState) {
    case 'LEARN': return '开始 / 继续学习'
    case 'EXPIRED': return '已过期 · 去续费'
    case 'SUBSCRIBE': return '订阅会员可看'
    default: return '立即购买'
  }
})

const validityText = computed(() => {
  const d = detail.value
  if (!d) return ''
  if (d.validityType === 'PERMANENT') return '永久有效'
  if (d.validityType === 'DAYS') return `购买后 ${d.validityDays} 天有效`
  return '固定到期'
})

function fmtDur(s: number) {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}
function yuan(fen: number) {
  return (fen / 100).toFixed(fen % 100 === 0 ? 0 : 2)
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    detail.value = await fetchCourseDetail(id)
    if (!detail.value?.title) detail.value = MOCK
  } catch {
    detail.value = MOCK
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
  } else {
    router.push({ path: '/checkout', query: { itemType: 'COURSE', itemId: String(d.id) } })
  }
}
function playLesson(lessonId: number, locked: boolean, trial: boolean) {
  if (!locked || trial) router.push(`/learn/${detail.value!.id}/${lessonId}`)
}
</script>

<template>
  <div v-if="loading" class="container loading"><el-skeleton :rows="8" animated /></div>
  <template v-else-if="detail">
    <!-- 头图区 -->
    <section class="head-band">
      <div class="container head">
        <div class="head-media">
          <div class="media-ph">
            <el-icon :size="40"><VideoPlay /></el-icon>
            <span>试看预览</span>
          </div>
        </div>
        <div class="head-info">
          <div class="badges">
            <span class="tag tag-sub" v-if="features.subscription && detail.inSubscription">订阅可看</span>
            <span class="tag tag-hot">目标 {{ detail.targetScore }}</span>
          </div>
          <h1>{{ detail.title }}</h1>
          <p class="sub">{{ detail.intro.slice(0, 48) }}…</p>
          <div class="teacher">
            <div class="avatar">{{ detail.teacher.name.slice(0, 1) }}</div>
            <div>
              <div class="tname">{{ detail.teacher.name }}</div>
              <div class="tintro">{{ detail.teacher.intro }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="container body">
      <div class="main">
        <!-- 课程大纲 -->
        <section class="card">
          <h2 class="block-title">课程大纲</h2>
          <div v-for="ch in detail.chapters" :key="ch.id" class="chapter">
            <div class="chapter-title">{{ ch.title }}</div>
            <div
              v-for="ls in ch.lessons"
              :key="ls.id"
              :class="['lesson', { playable: !ls.locked || ls.freeTrial }]"
              @click="playLesson(ls.id, ls.locked, ls.freeTrial)"
            >
              <el-icon class="l-icon"><VideoPlay v-if="!ls.locked || ls.freeTrial" /><Lock v-else /></el-icon>
              <span class="l-title">{{ ls.title }}</span>
              <span v-if="ls.freeTrial" class="tag tag-trial">试看</span>
              <span class="l-dur">{{ fmtDur(ls.duration) }}</span>
            </div>
          </div>
        </section>

        <!-- 课件 -->
        <section class="card">
          <h2 class="block-title">配套课件 <span class="hint-inline">仅在线预览，不支持下载</span></h2>
          <div class="materials">
            <div v-for="m in detail.materials" :key="m.id" class="material">
              <el-icon :size="20"><Document v-if="m.type==='PDF'" /><Headset v-else-if="m.type==='AUDIO'" /><Picture v-else /></el-icon>
              <span>{{ m.name }}</span>
              <span class="m-preview">预览</span>
            </div>
          </div>
        </section>

        <!-- 介绍 -->
        <section class="card">
          <h2 class="block-title">课程介绍</h2>
          <p class="intro-text">{{ detail.intro }}</p>
        </section>
      </div>

      <!-- 侧边购买卡 -->
      <aside class="buy-card">
        <div class="buy-price">
          <span class="cur">¥</span>{{ yuan(detail.price) }}
          <span v-if="detail.originalPrice > detail.price" class="price-origin">¥{{ yuan(detail.originalPrice) }}</span>
        </div>
        <div class="buy-meta">
          <div><el-icon><Clock /></el-icon> {{ validityText }}</div>
          <div><el-icon><User /></el-icon> {{ detail.sales }} 人在学</div>
          <div><el-icon><Collection /></el-icon> {{ detail.chapters.reduce((n, c) => n + c.lessons.length, 0) }} 课时</div>
        </div>
        <el-button type="primary" size="large" round class="buy-btn" @click="onCta">{{ ctaText }}</el-button>
        <el-button v-if="features.subscription && detail.inSubscription" size="large" round class="buy-sub" @click="router.push('/subscribe')">
          订阅会员可看
        </el-button>
        <p class="buy-note">购买后支持断点续播 · 加密播放保护版权</p>
      </aside>
    </div>
  </template>
</template>

<style scoped>
.loading { padding: 40px 20px; }
.head-band {
  background: linear-gradient(160deg, var(--brand-50), #fff);
  border-bottom: 1px solid var(--line);
}
.head {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 32px;
  padding: 32px 20px;
}
.media-ph {
  aspect-ratio: 16 / 10;
  border-radius: var(--r-lg);
  background: linear-gradient(135deg, var(--brand-400), var(--brand-700));
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: var(--shadow-md);
}
.media-ph span { font-size: 14px; opacity: 0.9; }
.head-info { padding-top: 4px; }
.badges { display: flex; gap: 8px; margin-bottom: 12px; }
.head-info h1 { font-size: 28px; line-height: 1.3; }
.head-info .sub { color: var(--text-2); margin: 10px 0 20px; }
.teacher { display: flex; gap: 12px; align-items: center; }
.avatar {
  width: 46px; height: 46px; border-radius: 50%;
  background: var(--brand-500); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 600; flex-shrink: 0;
}
.tname { font-weight: 600; }
.tintro { font-size: 13px; color: var(--text-3); }

.body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 28px;
  padding: 28px 20px 40px;
  align-items: start;
}
.card {
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  padding: 20px 22px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}
.block-title { font-size: 18px; margin-bottom: 16px; }
.hint-inline { font-size: 12px; font-weight: 400; color: var(--text-3); margin-left: 8px; }
.chapter { margin-bottom: 14px; }
.chapter-title {
  font-size: 14px; color: var(--text-2); font-weight: 600;
  padding: 8px 0; border-bottom: 1px dashed var(--line);
}
.lesson {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 8px; border-radius: var(--r-sm);
  color: var(--text-3); font-size: 14px;
  transition: background 0.15s;
}
.lesson.playable { color: var(--text-1); cursor: pointer; }
.lesson.playable:hover { background: var(--brand-50); }
.l-icon { color: var(--text-3); }
.lesson.playable .l-icon { color: var(--brand-500); }
.l-title { flex: 1; }
.l-dur { font-size: 13px; color: var(--text-3); }
.materials { display: flex; flex-direction: column; gap: 10px; }
.material {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 1px solid var(--line);
  border-radius: var(--r-sm); font-size: 14px;
}
.material .el-icon { color: var(--brand-500); }
.material span:nth-child(2) { flex: 1; }
.m-preview { font-size: 12px; color: var(--brand-600); }
.intro-text { color: var(--text-2); line-height: 1.8; margin: 0; }

.buy-card {
  position: sticky;
  top: 84px;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  padding: 22px;
  box-shadow: var(--shadow-md);
}
.buy-price { font-size: 32px; color: var(--danger); font-weight: 700; }
.buy-price .cur { font-size: 18px; }
.buy-meta {
  margin: 16px 0 18px;
  display: flex; flex-direction: column; gap: 10px;
  font-size: 14px; color: var(--text-2);
}
.buy-meta .el-icon { color: var(--text-3); margin-right: 6px; vertical-align: -2px; }
.buy-btn { width: 100%; }
.buy-sub { width: 100%; margin: 10px 0 0 0; }
.buy-note { font-size: 12px; color: var(--text-3); text-align: center; margin: 14px 0 0; }
@media (max-width: 980px) {
  .head, .body { grid-template-columns: 1fr; }
  .buy-card { position: static; }
}
</style>
