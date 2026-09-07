<script setup lang="ts">
import { computed, ref } from 'vue'
import CourseCard from '@/components/CourseCard.vue'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'

const filters = ref({ cat: 'all', score: 'all', type: 'all', sort: 'default' })

const cats = [
  { k: 'all', l: '全部' }, { k: 'listening', l: '听力' }, { k: 'speaking', l: '口语' },
  { k: 'reading', l: '阅读' }, { k: 'writing', l: '写作' }, { k: 'sprint', l: '冲分' },
]
const scores = [
  { k: 'all', l: '不限' }, { k: '5.5', l: '5.5' }, { k: '6', l: '6.0' },
  { k: '6.5', l: '6.5' }, { k: '7', l: '7+' },
]
const types = [
  { k: 'all', l: '全部' }, { k: 'basic', l: '基础' }, { k: 'intensive', l: '强化' }, { k: 'sprint', l: '冲分' },
]
const sorts = [
  { k: 'default', l: '综合' }, { k: 'newest', l: '最新' }, { k: 'sales', l: '销量' }, { k: 'price', l: '价格' },
]

const all: CourseCardType[] = [
  { id: 1, title: '雅思口语冲分班 · 7分突破', cover: '', teacherName: '李老师', targetScore: '7+', lessonCount: 16, price: 39900, originalPrice: 59900, inSubscription: true, sales: 1280 },
  { id: 2, title: '雅思写作 7 分进阶', cover: '', teacherName: '王老师', targetScore: '7+', lessonCount: 20, price: 49900, originalPrice: 69900, inSubscription: false, sales: 860 },
  { id: 3, title: '雅思听力基础精讲', cover: '', teacherName: '张老师', targetScore: '6.0', lessonCount: 30, price: 19900, originalPrice: 29900, inSubscription: true, sales: 2100 },
  { id: 4, title: '雅思阅读技巧全解', cover: '', teacherName: '陈老师', targetScore: '6.5', lessonCount: 18, price: 29900, originalPrice: 0, inSubscription: false, sales: 540 },
  { id: 5, title: '雅思口语 Part 2 话题库', cover: '', teacherName: '李老师', targetScore: '6.5', lessonCount: 24, price: 25900, originalPrice: 35900, inSubscription: true, sales: 980 },
  { id: 7, title: '雅思听力 section 3-4 攻坚', cover: '', teacherName: '张老师', targetScore: '7+', lessonCount: 14, price: 32900, originalPrice: 0, inSubscription: false, sales: 410 },
  { id: 8, title: '雅思全程冲分班 8 周', cover: '', teacherName: '教研组', targetScore: '7+', lessonCount: 64, price: 129900, originalPrice: 169900, inSubscription: false, sales: 320 },
]

const list = computed(() => {
  let r = [...all]
  if (filters.value.score !== 'all') r = r.filter((c) => c.targetScore.startsWith(filters.value.score))
  if (filters.value.sort === 'sales') r.sort((a, b) => b.sales - a.sales)
  if (filters.value.sort === 'price') r.sort((a, b) => a.price - b.price)
  return r
})
</script>

<template>
  <div class="container list-page">
    <h1 class="page-title">全部课程</h1>

    <div class="filters card">
      <div class="frow">
        <span class="flabel">分类</span>
        <div class="fopts">
          <button v-for="c in cats" :key="c.k" :class="['fopt', { on: filters.cat === c.k }]" @click="filters.cat = c.k">{{ c.l }}</button>
        </div>
      </div>
      <div class="frow">
        <span class="flabel">目标分</span>
        <div class="fopts">
          <button v-for="s in scores" :key="s.k" :class="['fopt', { on: filters.score === s.k }]" @click="filters.score = s.k">{{ s.l }}</button>
        </div>
      </div>
      <div class="frow">
        <span class="flabel">班型</span>
        <div class="fopts">
          <button v-for="t in types" :key="t.k" :class="['fopt', { on: filters.type === t.k }]" @click="filters.type = t.k">{{ t.l }}</button>
        </div>
      </div>
      <div class="frow">
        <span class="flabel">排序</span>
        <div class="fopts">
          <button v-for="s in sorts" :key="s.k" :class="['fopt', { on: filters.sort === s.k }]" @click="filters.sort = s.k">{{ s.l }}</button>
        </div>
      </div>
    </div>

    <div class="result-meta">共 {{ list.length }} 门课程</div>
    <div class="grid">
      <CourseCard v-for="c in list" :key="c.id" :course="c" />
    </div>
  </div>
</template>

<style scoped>
.list-page { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 18px; }
.filters { padding: 8px 20px; margin-bottom: 18px; }
.card { background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-sm); }
.frow { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.frow:last-child { border-bottom: none; }
.flabel { width: 56px; flex-shrink: 0; font-size: 14px; color: var(--text-3); }
.fopts { display: flex; flex-wrap: wrap; gap: 8px; }
.fopt {
  border: none; background: transparent; color: var(--text-2);
  padding: 5px 14px; border-radius: 999px; font-size: 14px; transition: all 0.15s;
}
.fopt:hover { color: var(--brand-600); background: var(--brand-50); }
.fopt.on { background: var(--brand-500); color: #fff; }
.result-meta { font-size: 13px; color: var(--text-3); margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
@media (max-width: 980px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) {
  .grid { grid-template-columns: 1fr; }
  .list-page { padding: 20px 12px 36px; }
  .filters { padding: 8px 12px; }
  .frow { flex-direction: column; align-items: flex-start; gap: 6px; }
}
</style>
