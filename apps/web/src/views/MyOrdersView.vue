<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tab = ref('all')

interface OrderRow {
  orderNo: string
  title: string
  amount: number
  status: 'UNPAID' | 'PAID' | 'CLOSED' | 'REFUNDED'
  createdAt: string
}

const orders: OrderRow[] = [
  { orderNo: 'IE202606200001', title: '雅思口语冲分班 · 7分突破', amount: 34900, status: 'PAID', createdAt: '2026-06-20 14:32' },
  { orderNo: 'IE202606180042', title: '雅思听力基础精讲', amount: 19900, status: 'PAID', createdAt: '2026-06-18 09:10' },
  { orderNo: 'IE202606170088', title: '雅思写作 7 分进阶', amount: 49900, status: 'UNPAID', createdAt: '2026-06-17 21:45' },
  { orderNo: 'IE202605300012', title: '雅思基础语法', amount: 9900, status: 'REFUNDED', createdAt: '2026-05-30 16:20' },
]

const statusMap: Record<string, { text: string; type: 'success' | 'info' | 'warning' | 'danger' }> = {
  UNPAID: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  CLOSED: { text: '已关闭', type: 'info' },
  REFUNDED: { text: '已退款', type: 'danger' },
}

const filtered = computed(() => (tab.value === 'all' ? orders : orders.filter((o) => o.status === tab.value)))
function yuan(fen: number) {
  return (fen / 100).toFixed(2)
}
</script>

<template>
  <div class="container my-page">
    <h1 class="page-title">我的订单</h1>
    <el-tabs v-model="tab">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待支付" name="UNPAID" />
      <el-tab-pane label="已支付" name="PAID" />
      <el-tab-pane label="已退款" name="REFUNDED" />
    </el-tabs>

    <div v-if="filtered.length" class="orders">
      <div v-for="o in filtered" :key="o.orderNo" class="order card">
        <div class="order-cover"><el-icon :size="20" color="#fff"><Reading /></el-icon></div>
        <div class="order-info">
          <div class="order-title">{{ o.title }}</div>
          <div class="order-meta">订单号 {{ o.orderNo }} · {{ o.createdAt }}</div>
        </div>
        <div class="order-amount">¥{{ yuan(o.amount) }}</div>
        <el-tag :type="statusMap[o.status].type" effect="light" round>{{ statusMap[o.status].text }}</el-tag>
        <div class="order-actions">
          <el-button v-if="o.status === 'UNPAID'" type="primary" size="small" round @click="router.push('/checkout')">去支付</el-button>
          <el-button v-else-if="o.status === 'PAID'" size="small" round @click="router.push('/my/learning')">去学习</el-button>
          <el-button v-else size="small" round disabled>已完成</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无订单" />
  </div>
</template>

<style scoped>
.my-page { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 8px; }
.orders { display: flex; flex-direction: column; gap: 12px; margin-top: 8px; }
.card { background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-lg); box-shadow: var(--shadow-sm); }
.order { display: flex; align-items: center; gap: 14px; padding: 14px 18px; }
.order-cover {
  width: 56px; height: 42px; border-radius: var(--r-sm); flex-shrink: 0;
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  display: flex; align-items: center; justify-content: center;
}
.order-info { flex: 1; min-width: 0; }
.order-title { font-weight: 600; }
.order-meta { font-size: 13px; color: var(--text-3); margin-top: 4px; }
.order-amount { font-weight: 600; color: var(--danger); width: 90px; text-align: right; }
.order-actions { width: 92px; text-align: right; }
@media (max-width: 720px) {
  .order { flex-wrap: wrap; }
  .order-amount, .order-actions { width: auto; }
}
</style>
