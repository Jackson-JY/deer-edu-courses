<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CourseCard from '@/components/CourseCard.vue'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'
import { fetchCourses } from '@/api/courses'

const filters = ref({ cat: 'all', score: 'all', type: 'all', sort: 'default' })
const allCourses = ref<CourseCardType[]>([])
const loading = ref(true)

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

onMounted(async () => {
  try {
    const page = await fetchCourses({ size: 100 })
    allCourses.value = page.list.length ? page.list : []
  } catch {
    allCourses.value = []
  } finally {
    loading.value = false
  }
})

const list = computed(() => {
  let r = [...allCourses.value]
  if (filters.value.score !== 'all') r = r.filter((c) => c.targetScore.startsWith(filters.value.score))
  if (filters.value.sort === 'sales') r.sort((a, b) => b.sales - a.sales)
  if (filters.value.sort === 'price') r.sort((a, b) => a.price - b.price)
  return r
})
</script>

<template>
  <div class="list-page section">
    <div class="section-header">
      <h1>全部课程</h1>
    </div>

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
    <div v-if="loading" class="grid-loading">加载中…</div>
    <div v-else class="grid grid-4">
      <CourseCard v-for="c in list" :key="c.id" :course="c" />
    </div>
  </div>
</template>

<style scoped>
.list-page { padding: 3rem 1.5rem 5rem; }
.section-header h1 {
  font-size: clamp(1.75rem, 4vw, 2.5rem);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
  margin: 0;
}
.filters { padding: 8px 20px; margin-bottom: 1.5rem; }
.frow { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--border); }
.frow:last-child { border-bottom: none; }
.flabel { width: 56px; flex-shrink: 0; font-size: 14px; color: var(--muted-foreground); }
.fopts { display: flex; flex-wrap: wrap; gap: 8px; }
.fopt {
  border: none; background: transparent; color: var(--text-600, var(--foreground));
  padding: 6px 16px; border-radius: 999px; font-size: 14px; transition: all 0.15s; cursor: pointer;
}
.fopt:hover { color: var(--primary); background: var(--secondary); }
.fopt.on { background: var(--primary); color: var(--primary-foreground); }
.result-meta { font-size: 13px; color: var(--muted-foreground); margin-bottom: 1rem; }
.grid-loading { padding: 3rem; text-align: center; color: var(--muted-foreground); }
.grid { display: grid; gap: 1.5rem; }
.grid-4 { grid-template-columns: repeat(1, minmax(0, 1fr)); }
@media (min-width: 768px) { .grid-4 { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (min-width: 1024px) { .grid-4 { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
@media (max-width: 767px) {
  .frow { flex-direction: column; align-items: flex-start; gap: 8px; }
  .flabel { width: auto; }
}
</style>
