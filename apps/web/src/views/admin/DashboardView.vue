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
.page-title { font-size: 22px; margin-bottom: 20px; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.stat-card {
  background: #fff; border: 1px solid var(--line); border-radius: var(--r-lg);
  padding: 24px; box-shadow: var(--shadow-sm);
}
.stat-value { font-size: 32px; font-weight: 700; color: var(--brand-600); }
.stat-label { font-size: 14px; color: var(--text-3); margin-top: 6px; }
</style>
