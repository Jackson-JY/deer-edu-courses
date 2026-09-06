<script setup lang="ts">
import { computed, ref } from 'vue'

const item = {
  title: '雅思口语冲分班 · 7分突破',
  validity: '购买后 365 天有效',
  price: 39900,
}
const coupons = [
  { id: 1, name: '满300减50', cut: 5000, usable: true },
  { id: 2, name: '9折券（不可叠加）', cut: 0, usable: false },
]
const selectedCoupon = ref(1)
const agreed = ref(false)

const discount = computed(() => coupons.find((c) => c.id === selectedCoupon.value && c.usable)?.cut ?? 0)
const payAmount = computed(() => item.price - discount.value)
function yuan(fen: number) {
  return (fen / 100).toFixed(fen % 100 === 0 ? 0 : 2)
}
</script>

<template>
  <div class="container checkout">
    <h1 class="page-title">确认订单</h1>
    <div class="ck-grid">
      <div class="ck-main">
        <!-- 商品 -->
        <div class="card">
          <div class="goods">
            <div class="goods-cover"><el-icon :size="22" color="#fff"><Reading /></el-icon></div>
            <div class="goods-info">
              <div class="goods-title">{{ item.title }}</div>
              <div class="goods-validity">{{ item.validity }}</div>
            </div>
            <div class="goods-price">¥{{ yuan(item.price) }}</div>
          </div>
        </div>

        <!-- 优惠券 -->
        <div class="card">
          <div class="card-title">优惠券 <span class="card-hint">不可叠加，自动取最优</span></div>
          <el-radio-group v-model="selectedCoupon" class="coupon-group">
            <label
              v-for="c in coupons"
              :key="c.id"
              :class="['coupon', { disabled: !c.usable }]"
            >
              <el-radio :value="c.id" :disabled="!c.usable">
                <span class="coupon-name">{{ c.name }}</span>
                <span v-if="!c.usable" class="coupon-reason">不可用</span>
                <span v-else class="coupon-cut">- ¥{{ yuan(c.cut) }}</span>
              </el-radio>
            </label>
          </el-radio-group>
        </div>

        <!-- 支付方式 -->
        <div class="card">
          <div class="card-title">支付方式</div>
          <div class="pay-method active">
            <el-icon :size="20" color="#09bb07"><ChatRound /></el-icon>
            <span>微信支付</span>
            <span class="pay-tag">PC 扫码 · H5 自动 JSAPI</span>
            <el-icon class="pay-check"><CircleCheckFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 价格明细 + 扫码 -->
      <aside class="ck-aside">
        <div class="card pay-summary">
          <div class="sum-title">价格明细</div>
          <div class="sum-row"><span>原价</span><span>¥{{ yuan(item.price) }}</span></div>
          <div class="sum-row"><span>优惠券</span><span class="cut">- ¥{{ yuan(discount) }}</span></div>
          <div class="sum-total">
            <span>应付</span>
            <span class="total-amt"><i>¥</i>{{ yuan(payAmount) }}</span>
          </div>
          <div class="sum-note">金额由服务端计算，前端仅展示</div>

          <div class="qr">
            <div class="qr-box"><el-icon :size="44" color="#1a2230"><Grid /></el-icon></div>
            <div class="qr-tip">微信扫码支付，支付后自动跳转</div>
          </div>

          <el-checkbox v-model="agreed" class="agree">
            我已阅读并同意《购买须知》与退款规则
          </el-checkbox>
          <el-button type="primary" size="large" round class="pay-btn" :disabled="!agreed">
            确认支付 ¥{{ yuan(payAmount) }}
          </el-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.checkout { padding: 28px 20px 48px; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.ck-grid { display: grid; grid-template-columns: 1fr 340px; gap: 24px; align-items: start; }
.card {
  background: var(--bg); border: 1px solid var(--line);
  border-radius: var(--r-lg); padding: 18px 20px; margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
}
.card-title { font-weight: 600; margin-bottom: 14px; }
.card-hint { font-size: 12px; font-weight: 400; color: var(--text-3); margin-left: 8px; }
.goods { display: flex; align-items: center; gap: 14px; }
.goods-cover {
  width: 72px; height: 52px; border-radius: var(--r-sm);
  background: linear-gradient(135deg, var(--brand-400), var(--brand-600));
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.goods-info { flex: 1; }
.goods-title { font-weight: 600; }
.goods-validity { font-size: 13px; color: var(--text-3); margin-top: 4px; }
.goods-price { font-weight: 600; }
.coupon-group { display: flex; flex-direction: column; gap: 4px; width: 100%; }
.coupon {
  display: block; padding: 10px 12px; border: 1px solid var(--line);
  border-radius: var(--r-sm);
}
.coupon.disabled { background: var(--bg-soft); }
.coupon-name { margin-right: 8px; }
.coupon-cut { color: var(--danger); font-size: 13px; }
.coupon-reason { color: var(--text-3); font-size: 12px; }
.pay-method {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; border: 1px solid var(--line); border-radius: var(--r-sm);
}
.pay-method.active { border-color: var(--brand-400); background: var(--brand-50); }
.pay-tag { font-size: 12px; color: var(--text-3); }
.pay-check { margin-left: auto; color: var(--brand-500); font-size: 18px; }
.pay-summary { position: sticky; top: 84px; }
.sum-title { font-weight: 600; margin-bottom: 14px; }
.sum-row { display: flex; justify-content: space-between; font-size: 14px; color: var(--text-2); padding: 5px 0; }
.sum-row .cut { color: var(--success); }
.sum-total {
  display: flex; justify-content: space-between; align-items: baseline;
  border-top: 1px solid var(--line); margin-top: 8px; padding-top: 12px;
}
.total-amt { font-size: 26px; font-weight: 700; color: var(--danger); }
.total-amt i { font-size: 15px; font-style: normal; }
.sum-note { font-size: 12px; color: var(--text-3); margin-top: 6px; }
.qr { margin: 16px 0; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.qr-box {
  width: 130px; height: 130px; border: 1px solid var(--line); border-radius: var(--r-md);
  display: flex; align-items: center; justify-content: center; background: var(--bg-soft);
}
.qr-tip { font-size: 12px; color: var(--text-3); }
.agree { margin-bottom: 12px; }
.pay-btn { width: 100%; }
@media (max-width: 980px) {
  .ck-grid { grid-template-columns: 1fr; }
  .pay-summary { position: static; }
}
</style>
