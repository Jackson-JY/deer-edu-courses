<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="container nf">
    <div class="nf-code">404</div>
    <h1>页面走丢了</h1>
    <p>你访问的页面不存在或已下架。</p>
    <el-button type="primary" round size="large" @click="router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.nf { padding: 90px 20px; text-align: center; }
.nf-code { font-size: 88px; font-weight: 800; color: var(--brand-200); line-height: 1; }
.nf h1 { font-size: 24px; margin: 12px 0 8px; }
.nf p { color: var(--text-2); margin: 0 0 24px; }
</style>
