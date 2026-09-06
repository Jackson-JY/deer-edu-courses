<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
// status: success | processing | failed（真实以服务端订单状态为准）
const status = computed(() => (route.query.status as string) || 'success')
const courseId = computed(() => route.query.courseId as string)
</script>

<template>
  <div class="container result">
    <div class="result-card">
      <template v-if="status === 'success'">
        <el-icon :size="60" color="#1d9e75"><CircleCheckFilled /></el-icon>
        <h1>支付成功</h1>
        <p>课程已加入「我的学习」，可立即开始观看。</p>
        <div class="result-actions">
          <el-button type="primary" round size="large" @click="router.push(courseId ? `/learn/${courseId}/1` : '/my/learning')">立即学习</el-button>
          <el-button round size="large" @click="router.push('/my/orders')">查看订单</el-button>
        </div>
      </template>

      <template v-else-if="status === 'processing'">
        <el-icon :size="60" color="#e8943a"><Clock /></el-icon>
        <h1>支付处理中</h1>
        <p>正在确认支付结果，请稍候。若已扣款请勿重复支付。</p>
        <div class="result-actions">
          <el-button type="primary" round size="large" @click="router.push('/my/orders')">查看订单状态</el-button>
        </div>
      </template>

      <template v-else>
        <el-icon :size="60" color="#e0533d"><CircleCloseFilled /></el-icon>
        <h1>支付未完成</h1>
        <p>订单尚未支付成功，你可以重新发起支付。</p>
        <div class="result-actions">
          <el-button type="primary" round size="large" @click="router.back()">重新支付</el-button>
          <el-button round size="large" @click="router.push('/courses')">返回课程</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.result { padding: 60px 20px; display: flex; justify-content: center; }
.result-card {
  background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-lg);
  box-shadow: var(--shadow-sm); padding: 48px 56px; text-align: center; max-width: 480px;
}
.result-card h1 { font-size: 24px; margin: 18px 0 8px; }
.result-card p { color: var(--text-2); margin: 0 0 28px; }
.result-actions { display: flex; gap: 12px; justify-content: center; }
</style>
