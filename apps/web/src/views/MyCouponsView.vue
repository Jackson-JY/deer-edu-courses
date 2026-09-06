<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tab = ref('AVAILABLE')

interface CouponRow {
  id: number
  name: string
  amount: string
  cond: string
  scope: string
  end: string
  status: 'AVAILABLE' | 'USED' | 'EXPIRED'
}

const coupons: CouponRow[] = [
  { id: 1, name: '满300减50', amount: '¥50', cond: '满 ¥300 可用', scope: '全部课程', end: '2026-07-31', status: 'AVAILABLE' },
  { id: 2, name: '新人立减20', amount: '¥20', cond: '无门槛', scope: '指定课程', end: '2026-06-30', status: 'AVAILABLE' },
  { id: 3, name: '满500减100', amount: '¥100', cond: '满 ¥500 可用', scope: '全部课程', end: '2026-05-20', status: 'USED' },
  { id: 4, name: '暑期8折券', amount: '8折', cond: '满 ¥200 可用', scope: '冲分课程', end: '2026-05-01', status: 'EXPIRED' },
]

const counts = computed(() => ({
  AVAILABLE: coupons.filter((c) => c.status === 'AVAILABLE').length,
  USED: coupons.filter((c) => c.status === 'USED').length,
  EXPIRED: coupons.filter((c) => c.status === 'EXPIRED').length,
}))
const filtered = computed(() => coupons.filter((c) => c.status === tab.value))
</script>

<template>
  <div class="container my-page">
    <h1 class="page-title">我的优惠券</h1>
    <el-tabs v-model="tab">
      <el-tab-pane :label="`可用 (${counts.AVAILABLE})`" name="AVAILABLE" />
      <el-tab-pane :label="`已使用 (${counts.USED})`" name="USED" />
      <el-tab-pane :label="`已过期 (${counts.EXPIRED})`" name="EXPIRED" />
    </el-tabs>

    <div v-if="filtered.length" class="coupons">
      <div v-for="c in filtered" :key="c.id" :class="['coupon', c.status.toLowerCase()]">
        <div class="coupon-left">
          <div class="coupon-amount">{{ c.amount }}</div>
          <div class="coupon-cond">{{ c.cond }}</div>
        </div>
        <div class="coupon-right">
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-scope">适用：{{ c.scope }}</div>
          <div class="coupon-end">有效期至 {{ c.end }}</div>
        </div>
        <div class="coupon-action">
          <el-button v-if="c.status === 'AVAILABLE'" type="primary" size="small" round @click="router.push('/courses')">去使用</el-button>
          <span v-else-if="c.status === 'USED'" class="stamp">已使用</span>
          <span v-else class="stamp">已过期</span>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无优惠券" />
  </div>
</template>

<style scoped>
.my-page { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 8px; }
.coupons { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 8px; }
.coupon {
  display: flex; align-items: center; background: var(--bg);
  border: 1px solid var(--line); border-radius: var(--r-lg); overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.coupon-left {
  width: 130px; flex-shrink: 0; padding: 18px 16px; text-align: center;
  background: linear-gradient(135deg, var(--brand-500), var(--brand-700)); color: #fff;
  position: relative;
}
.coupon.used .coupon-left, .coupon.expired .coupon-left {
  background: var(--bg-mute); color: var(--text-3);
}
.coupon-amount { font-size: 26px; font-weight: 700; }
.coupon-cond { font-size: 12px; opacity: 0.9; margin-top: 4px; }
.coupon-right { flex: 1; padding: 14px 16px; }
.coupon-name { font-weight: 600; }
.coupon-scope { font-size: 13px; color: var(--text-2); margin-top: 4px; }
.coupon-end { font-size: 12px; color: var(--text-3); margin-top: 4px; }
.coupon-action { padding-right: 16px; }
.stamp {
  font-size: 13px; color: var(--text-3); border: 1px solid var(--line-strong);
  padding: 3px 10px; border-radius: var(--r-sm);
}
@media (max-width: 720px) { .coupons { grid-template-columns: 1fr; } }
</style>
