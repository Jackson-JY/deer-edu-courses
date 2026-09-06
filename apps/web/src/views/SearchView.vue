<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CourseCard from '@/components/CourseCard.vue'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'

const route = useRoute()
const router = useRouter()
const q = computed(() => (route.query.q as string) || '')

const hot = ['口语 7分', '听力技巧', '大作文', 'Part 2', '基础语法']

const all: CourseCardType[] = [
  { id: 1, title: '雅思口语冲分班 · 7分突破', cover: '', teacherName: '李老师', targetScore: '7+', lessonCount: 16, price: 39900, originalPrice: 59900, inSubscription: false, sales: 1280 },
  { id: 2, title: '雅思写作 7 分进阶', cover: '', teacherName: '王老师', targetScore: '7+', lessonCount: 20, price: 49900, originalPrice: 69900, inSubscription: false, sales: 860 },
  { id: 5, title: '雅思口语 Part 2 话题库', cover: '', teacherName: '李老师', targetScore: '6.5', lessonCount: 24, price: 25900, originalPrice: 35900, inSubscription: false, sales: 980 },
]
const results = computed(() => (q.value ? all.filter((c) => c.title.includes(q.value) || q.value.length < 3) : []))

function search(k: string) {
  router.push({ path: '/search', query: { q: k } })
}
</script>

<template>
  <div class="container search-page">
    <h1 class="page-title">搜索 “{{ q }}”</h1>

    <div class="hot">
      <span class="hot-label">热门搜索：</span>
      <button v-for="h in hot" :key="h" class="hot-tag" @click="search(h)">{{ h }}</button>
    </div>

    <template v-if="results.length">
      <div class="result-meta">找到 {{ results.length }} 门相关课程</div>
      <div class="grid">
        <CourseCard v-for="c in results" :key="c.id" :course="c" />
      </div>
    </template>
    <el-empty v-else description="没有找到相关课程，换个关键词试试" />
  </div>
</template>

<style scoped>
.search-page { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 16px; }
.hot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; }
.hot-label { font-size: 14px; color: var(--text-3); }
.hot-tag {
  border: 1px solid var(--line-strong); background: var(--bg); color: var(--text-2);
  padding: 5px 14px; border-radius: 999px; font-size: 13px; transition: all 0.15s;
}
.hot-tag:hover { border-color: var(--brand-300); color: var(--brand-600); }
.result-meta { font-size: 13px; color: var(--text-3); margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
@media (max-width: 980px) { .grid { grid-template-columns: repeat(2, 1fr); } }
</style>
