<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { CourseCard } from '@ielts/shared-types'

defineProps<{ course: CourseCard }>()
</script>

<template>
  <RouterLink :to="`/course/${course.id}`" class="card media-card course-card">
    <div class="media">
      <img v-if="course.cover" :src="course.cover" :alt="course.title" />
      <div v-else class="media-placeholder">
        <span>{{ course.title || '未命名课程' }}</span>
      </div>
    </div>
    <div class="card-body">
      <div class="eyebrow">{{ course.teacherName }} · 目标 {{ course.targetScore }}</div>
      <h3 class="title">{{ course.title }}</h3>
      <div class="course-meta">
        <span>{{ course.lessonCount }} 课时</span>
        <span class="dot">·</span>
        <span>{{ course.sales }} 人在学</span>
      </div>
      <div class="card-foot">
        <span class="cta">查看详情</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.course-card {
  overflow: hidden;
  text-decoration: none;
  color: inherit;
}
.course-card .media {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, var(--secondary), var(--accent));
  border-bottom: 1px solid var(--border);
  overflow: hidden;
}
.course-card .media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.media-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.media-placeholder span {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--brand-500);
  opacity: 0.85;
  padding: 0 18px;
  text-align: center;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.course-meta {
  font-size: 12px;
  color: var(--muted-foreground);
  display: flex;
  align-items: center;
  gap: 6px;
}
.course-meta .dot { opacity: 0.5; }
.card-foot {
  margin-top: auto;
  padding-top: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style>
