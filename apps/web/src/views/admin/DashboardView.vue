<script setup lang="ts">
const stats = [
  { label: '课程数', value: 0 },
  { label: '用户数', value: 0 },
  { label: '订单数', value: 0 },
]
</script>

<template>
  <div>
    <h1 class="page-title">工作台</h1>
    <div class="stats">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 20px; letter-spacing: -0.01em; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
@media (max-width: 640px) {
  .stats { grid-template-columns: 1fr; }
}
.stat-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.18s ease;
}
.stat-card:hover { box-shadow: var(--shadow-md); }
.stat-value { font-size: 2rem; font-weight: 700; color: var(--primary); line-height: 1.1; }
.stat-label { font-size: 13px; color: var(--muted-foreground); margin-top: 6px; }
</style>
