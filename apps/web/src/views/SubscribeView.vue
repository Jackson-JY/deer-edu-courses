<script setup lang="ts">
import { ref } from 'vue'
import CourseCard from '@/components/CourseCard.vue'
import type { CourseCard as CourseCardType } from '@ielts/shared-types'

const plans = [
  { id: 1, title: '月度会员', period: '月', price: 2900, daily: 97, tag: '' },
  { id: 2, title: '季度会员', period: '季', price: 7900, daily: 88, tag: '最受欢迎' },
  { id: 3, title: '年度会员', period: '年', price: 25900, daily: 71, tag: '最划算' },
]
const selected = ref(2)

const benefits = [
  { icon: 'Film', t: '专区课程畅学', d: '订阅专区全部课程不限次观看' },
  { icon: 'Refresh', t: '持续更新', d: '专区新课上架即可学' },
  { icon: 'Document', t: '配套课件', d: '课件在线预览，随课学习' },
  { icon: 'TrendCharts', t: '进度同步', d: '断点续播，学习记录云端保存' },
]

const zoneCourses: CourseCardType[] = [
  { id: 1, title: '雅思口语冲分班 · 7分突破', cover: '', teacherName: '李老师', targetScore: '7+', lessonCount: 16, price: 39900, originalPrice: 59900, inSubscription: true, sales: 1280 },
  { id: 3, title: '雅思听力基础精讲', cover: '', teacherName: '张老师', targetScore: '6.0', lessonCount: 30, price: 19900, originalPrice: 29900, inSubscription: true, sales: 2100 },
  { id: 5, title: '雅思口语 Part 2 话题库', cover: '', teacherName: '李老师', targetScore: '6.5', lessonCount: 24, price: 25900, originalPrice: 35900, inSubscription: true, sales: 980 },
  { id: 6, title: '雅思大作文论证模板', cover: '', teacherName: '王老师', targetScore: '6.5', lessonCount: 12, price: 0, originalPrice: 0, inSubscription: true, sales: 3200 },
]

function yuan(fen: number) {
  return (fen / 100).toFixed(fen % 100 === 0 ? 0 : 2)
}
</script>

<template>
  <section class="sub-hero">
    <div class="container">
      <span class="hero-eyebrow">订阅会员</span>
      <h1>一次订阅，专区课程畅学</h1>
      <p>覆盖听说读写核心课程，持续更新，按周期订阅更划算。</p>
    </div>
  </section>

  <div class="container">
    <div class="benefits">
      <div v-for="b in benefits" :key="b.t" class="benefit">
        <div class="b-icon"><el-icon :size="20"><component :is="b.icon" /></el-icon></div>
        <div class="b-t">{{ b.t }}</div>
        <div class="b-d">{{ b.d }}</div>
      </div>
    </div>

    <div class="section-head"><h2>选择订阅周期</h2></div>
    <div class="plans">
      <div
        v-for="p in plans"
        :key="p.id"
        :class="['plan', { active: selected === p.id }]"
        @click="selected = p.id"
      >
        <span v-if="p.tag" class="plan-tag">{{ p.tag }}</span>
        <div class="plan-title">{{ p.title }}</div>
        <div class="plan-price"><span class="cur">¥</span>{{ yuan(p.price) }}<span class="per">/{{ p.period }}</span></div>
        <div class="plan-daily">日均 ¥{{ (p.daily / 100).toFixed(2) }}</div>
        <el-radio :model-value="selected" :label="p.id" class="plan-radio">&nbsp;</el-radio>
      </div>
    </div>

    <div class="sub-action">
      <el-button type="primary" size="large" round>开通会员</el-button>
      <span class="sub-action-note">开通即同意《会员服务协议》，虚拟商品不支持退款</span>
    </div>

    <div class="section-head"><h2>订阅专区课程</h2><span class="more">仅会员可看</span></div>
    <div class="grid">
      <CourseCard v-for="c in zoneCourses" :key="c.id" :course="c" />
    </div>
  </div>
</template>

<style scoped>
.sub-hero {
  background: linear-gradient(160deg, #0f3c69, #1f6fc4);
  color: #fff;
  padding: 48px 0;
  margin-bottom: 8px;
}
.sub-hero .hero-eyebrow {
  display: inline-block; font-size: 13px; color: #fff;
  background: rgba(255,255,255,0.18); padding: 4px 12px; border-radius: 999px; margin-bottom: 14px;
}
.sub-hero h1 { font-size: 34px; }
.sub-hero p { color: rgba(255,255,255,0.82); margin: 12px 0 0; }
.benefits {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 28px;
}
.benefit {
  background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-md); padding: 18px;
}
.b-icon {
  width: 42px; height: 42px; border-radius: var(--r-md);
  background: var(--brand-50); color: var(--brand-500);
  display: flex; align-items: center; justify-content: center; margin-bottom: 10px;
}
.b-t { font-weight: 600; }
.b-d { font-size: 13px; color: var(--text-3); margin-top: 4px; }
.plans { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.plan {
  position: relative; background: var(--bg); border: 2px solid var(--line);
  border-radius: var(--r-lg); padding: 22px; cursor: pointer; text-align: center;
  transition: all 0.18s;
}
.plan:hover { border-color: var(--brand-300); }
.plan.active { border-color: var(--brand-500); box-shadow: var(--shadow-md); }
.plan-tag {
  position: absolute; top: -11px; left: 50%; transform: translateX(-50%);
  background: var(--accent); color: #fff; font-size: 12px; padding: 3px 12px; border-radius: 999px;
}
.plan-title { font-size: 16px; font-weight: 600; }
.plan-price { font-size: 30px; font-weight: 700; color: var(--brand-700); margin: 10px 0 2px; }
.plan-price .cur { font-size: 16px; }
.plan-price .per { font-size: 14px; font-weight: 400; color: var(--text-3); }
.plan-daily { font-size: 13px; color: var(--text-3); }
.plan-radio { margin-top: 12px; pointer-events: none; }
.sub-action {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  margin: 28px 0 8px;
}
.sub-action-note { font-size: 13px; color: var(--text-3); }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
@media (max-width: 980px) {
  .benefits, .grid { grid-template-columns: repeat(2, 1fr); }
  .plans { grid-template-columns: 1fr; }
}
</style>
