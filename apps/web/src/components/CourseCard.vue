<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { CourseCard } from '@ielts/shared-types'
import { features } from '@/config'

defineProps<{ course: CourseCard }>()

function yuan(fen: number) {
  return (fen / 100).toFixed(fen % 100 === 0 ? 0 : 2)
}
</script>

<template>
  <RouterLink :to="`/course/${course.id}`" class="cc">
    <div class="cc-cover">
      <img v-if="course.cover" :src="course.cover" :alt="course.title" />
      <div v-else class="cc-cover-ph">
        <span>{{ course.targetScore }}</span>
      </div>
      <span v-if="features.subscription && course.inSubscription" class="cc-sub">订阅可看</span>
    </div>
    <div class="cc-body">
      <h3 class="cc-title">{{ course.title }}</h3>
      <div class="cc-meta">
        <span>{{ course.teacherName }}</span>
        <span class="dot">·</span>
        <span>目标 {{ course.targetScore }}</span>
        <span class="dot">·</span>
        <span>{{ course.lessonCount }} 课时</span>
      </div>
      <div class="cc-foot">
        <div class="price">
          <span class="cur">¥</span>{{ yuan(course.price) }}
          <span v-if="course.originalPrice > course.price" class="price-origin">¥{{ yuan(course.originalPrice) }}</span>
        </div>
        <span class="cc-sales">{{ course.sales }} 人在学</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.cc {
  display: block;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.cc:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--brand-200);
}
.cc-cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: var(--bg-mute);
}
.cc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cc-cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  color: #fff;
}
.cc-cover-ph span {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
  opacity: 0.92;
}
.cc-sub {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 12px;
  color: #fff;
  background: var(--accent);
  padding: 3px 8px;
  border-radius: var(--r-sm);
}
.cc-body {
  padding: 12px 14px 14px;
}
.cc-title {
  font-size: 16px;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cc-meta {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-3);
}
.cc-meta .dot {
  margin: 0 5px;
}
.cc-foot {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.cc-foot .price {
  font-size: 19px;
}
.cc-sales {
  font-size: 12px;
  color: var(--text-3);
}
</style>
