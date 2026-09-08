<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCourseDetail } from '@/api/courses'
import type { CourseDetail } from '@ielts/shared-types'

const route = useRoute()
const router = useRouter()
const detail = ref<CourseDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const agreed = ref(false)

const payAmount = computed(() => detail.value?.price ?? 0)

function yuan(fen: number) {
  return (fen / 100).toFixed(fen % 100 === 0 ? 0 : 2)
}

const validityText = computed(() => {
  const d = detail.value
  if (!d) return ''
  if (d.validityType === 'PERMANENT') return '永久有效'
  if (d.validityType === 'DAYS') return `购买后 ${d.validityDays} 天有效`
  return '固定到期'
})

onMounted(async () => {
  const itemId = Number(route.query.itemId)
  if (!itemId) {
    errorMsg.value = '缺少课程参数'
    loading.value = false
    return
  }
  try {
    detail.value = await fetchCourseDetail(itemId)
  } catch (e: any) {
    errorMsg.value = e?.message || '课程信息加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container checkout">
    <h1 class="page-title">确认订单</h1>

    <div v-if="loading" class="muted-center">加载中…</div>
    <div v-else-if="errorMsg" class="muted-center">
      <p>{{ errorMsg }}</p>
      <button class="btn btn-primary" @click="router.back()">返回</button>
    </div>
    <div v-else-if="detail" class="ck-grid">
      <div class="ck-main">
        <!-- 商品 -->
        <div class="card">
          <div class="goods">
            <div class="goods-cover">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
            </div>
            <div class="goods-info">
              <div class="goods-title">{{ detail.title }}</div>
              <div class="goods-validity">{{ validityText }}</div>
            </div>
            <div class="goods-price">¥{{ yuan(detail.price) }}</div>
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="card">
          <div class="card-title">支付方式</div>
          <div class="pay-method active">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#09bb07"><path d="M8.5 12a1 1 0 1 1 0-2 1 1 0 0 1 0 2zm5 0a1 1 0 1 1 0-2 1 1 0 0 1 0 2zM12 2C6.48 2 2 5.58 2 10c0 2.43 1.22 4.6 3.14 6.05L4 21l4.8-2.4c.99.25 2.04.4 3.2.4 5.52 0 10-3.58 10-8S17.52 2 12 2z"/></svg>
            <span>微信支付</span>
            <span class="pay-tag">PC 扫码 · H5 自动 JSAPI</span>
            <svg class="pay-check" viewBox="0 0 24 24" width="18" height="18" fill="var(--primary)"><path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
          </div>
        </div>
      </div>

      <!-- 价格明细 + 扫码 -->
      <aside class="ck-aside">
        <div class="card pay-summary">
          <div class="sum-title">价格明细</div>
          <div class="sum-row"><span>原价</span><span>¥{{ yuan(detail.originalPrice || detail.price) }}</span></div>
          <div class="sum-row" v-if="detail.originalPrice > detail.price">
            <span>优惠</span>
            <span class="cut">- ¥{{ yuan(detail.originalPrice - detail.price) }}</span>
          </div>
          <div class="sum-total">
            <span>应付</span>
            <span class="total-amt"><i>¥</i>{{ yuan(payAmount) }}</span>
          </div>
          <div class="sum-note">金额由服务端计算，前端仅展示</div>

          <div class="qr">
            <div class="qr-box">
              <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3M21 14v.01M14 21h.01M21 21v-3"/></svg>
            </div>
            <div class="qr-tip">微信扫码支付，支付后自动跳转</div>
          </div>

          <label class="agree">
            <input type="checkbox" v-model="agreed" />
            <span>我已阅读并同意《购买须知》与退款规则</span>
          </label>
          <button class="btn btn-primary btn-lg pay-btn" :disabled="!agreed">
            确认支付 ¥{{ yuan(payAmount) }}
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.checkout { padding: 2rem 1.5rem 4rem; }
.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 1.5rem;
}
.ck-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  align-items: start;
}
@media (max-width: 991px) {
  .ck-grid { grid-template-columns: 1fr; }
}
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1.25rem 1.5rem;
  margin-bottom: 1rem;
  box-shadow: var(--shadow-sm);
}
.card-title { font-weight: 600; margin-bottom: 1rem; }
.card-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted-foreground);
  margin-left: 8px;
}
.goods { display: flex; align-items: center; gap: 14px; }
.goods-cover {
  width: 72px;
  height: 52px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.goods-info { flex: 1; }
.goods-title { font-weight: 600; }
.goods-validity {
  font-size: 13px;
  color: var(--muted-foreground);
  margin-top: 4px;
}
.goods-price { font-weight: 600; }

.muted-center { padding: 2rem; text-align: center; color: var(--muted-foreground); }
.muted-center p { margin-bottom: 1rem; }

.pay-method {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.pay-method.active {
  border-color: var(--primary);
  background: var(--primary-50, rgba(0, 122, 255, 0.06));
}
.pay-tag { font-size: 12px; color: var(--muted-foreground); margin-left: auto; margin-right: 8px; }
.pay-check { flex-shrink: 0; }

.pay-summary { position: sticky; top: 60px; }
@media (max-width: 991px) { .pay-summary { position: static; } }
.sum-title { font-weight: 600; margin-bottom: 1rem; }
.sum-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--text-600, var(--foreground));
  padding: 5px 0;
}
.sum-row .cut { color: var(--success, #10b981); font-weight: 600; }
.sum-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-top: 1px solid var(--border);
  margin-top: 8px;
  padding-top: 12px;
}
.total-amt {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--destructive);
}
.total-amt i { font-size: 15px; font-style: normal; }
.sum-note { font-size: 12px; color: var(--muted-foreground); margin-top: 6px; }
.qr {
  margin: 1.25rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.qr-box {
  width: 130px;
  height: 130px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--secondary);
  color: var(--icon-muted);
}
.qr-tip { font-size: 12px; color: var(--muted-foreground); }
.agree {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: var(--muted-foreground);
  margin-bottom: 1rem;
  cursor: pointer;
}
.agree input { accent-color: var(--primary); margin-top: 3px; }
.pay-btn { width: 100%; }
.pay-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
