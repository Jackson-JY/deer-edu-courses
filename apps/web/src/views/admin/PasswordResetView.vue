<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  issueResetCode,
  listPasswordResets,
  rejectResetRequest,
  type PasswordResetItem,
} from '@/api/admin'

const list = ref<PasswordResetItem[]>([])
const total = ref(0)
const pendingCount = ref(0)
const loading = ref(false)
const status = ref('')
const page = ref(1)
const size = 20

const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'CODE_ISSUED', label: '已发码' },
  { value: 'USED', label: '已完成' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'EXPIRED', label: '已过期' },
]

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待处理',
  CODE_ISSUED: '已发码',
  USED: '已完成',
  REJECTED: '已拒绝',
  EXPIRED: '已过期',
}

const STATUS_TAG: Record<string, 'warning' | 'primary' | 'success' | 'info' | 'danger'> = {
  PENDING: 'warning',
  CODE_ISSUED: 'primary',
  USED: 'success',
  REJECTED: 'info',
  EXPIRED: 'danger',
}

// ===== 发码弹窗 =====
const codeDialog = ref(false)
const issued = ref<{ phone: string; nickname: string | null; code: string; expireAt: string } | null>(null)

const pendingTip = computed(() => (pendingCount.value > 0 ? `${pendingCount.value} 条待处理` : ''))

async function load() {
  loading.value = true
  try {
    const res = await listPasswordResets({
      status: status.value || undefined,
      page: page.value - 1,
      size,
    })
    list.value = res.list
    total.value = res.total
    pendingCount.value = res.pendingCount
  } finally {
    loading.value = false
  }
}

function onFilter() {
  page.value = 1
  load()
}

function onPageChange(p: number) {
  page.value = p
  load()
}

async function issue(row: any) {
  try {
    await ElMessageBox.confirm(
      `请先线下核实「${row.nickname || row.phone}」（${row.phone}）的身份，确认是本人申请后签发重置码。`,
      '签发重置码',
      { type: 'warning', confirmButtonText: '确认签发', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    const res = await issueResetCode(row.id)
    issued.value = res
    codeDialog.value = true
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '签发失败')
  }
}

async function reject(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认拒绝「${row.nickname || row.phone}」的密码重置申请？`,
      '拒绝申请',
      { type: 'warning', confirmButtonText: '确认拒绝', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await rejectResetRequest(row.id)
    ElMessage.success('已拒绝')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function fmt(dt: string | null): string {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

async function copyCode() {
  if (!issued.value) return
  try {
    await navigator.clipboard.writeText(issued.value.code)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动记录')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="header-row">
      <h1 class="page-title">
        密码重置申请
        <el-tag v-if="pendingTip" type="warning" size="small" effect="plain">{{ pendingTip }}</el-tag>
      </h1>
      <el-select v-model="status" style="width: 130px" @change="onFilter">
        <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small">
            {{ STATUS_LABEL[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="重置码有效期" width="160">
        <template #default="{ row }">{{ fmt(row.codeExpireAt) }}</template>
      </el-table-column>
      <el-table-column prop="requestIp" label="提交 IP" min-width="130" />
      <el-table-column label="处理人" width="120">
        <template #default="{ row }">{{ row.handledByName || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING' || row.status === 'CODE_ISSUED'">
            <el-button link type="primary" @click="issue(row)">
              {{ row.status === 'PENDING' ? '签发重置码' : '重新签发' }}
            </el-button>
            <el-button link type="danger" @click="reject(row)">拒绝</el-button>
          </template>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>

    <!-- 重置码展示（仅显示一次） -->
    <el-dialog v-model="codeDialog" title="重置码已生成" width="400px" @closed="issued = null">
      <div v-if="issued" class="code-body">
        <p class="target">
          用户：<b>{{ issued.nickname || issued.phone }}</b>
          <span class="muted">（{{ issued.phone }}）</span>
        </p>
        <div class="code-box">{{ issued.code }}</div>
        <p class="muted note">
          有效期至 {{ fmt(issued.expireAt) }}，仅可使用一次。<br />
          请立即通过电话等可信渠道告知学员，关闭后将无法再次查看。
        </p>
      </div>
      <template #footer>
        <el-button @click="codeDialog = false">关闭</el-button>
        <el-button type="primary" @click="copyCode">复制重置码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.code-body { display: flex; flex-direction: column; gap: 12px; }
.code-body .target { margin: 0; font-size: 14px; }
.code-body .note { margin: 0; font-size: 12px; line-height: 1.6; }
.code-box {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 0.3em;
  text-align: center;
  padding: 18px;
  border-radius: var(--radius-sm);
  background: var(--secondary);
  color: var(--foreground);
}
.muted { color: var(--muted-foreground); }
@media (max-width: 768px) {
  .page-title { font-size: 18px; }
}
</style>
