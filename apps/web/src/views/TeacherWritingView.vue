<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listTeacherSubmissions,
  type TeacherSubmissionRow,
  type WritingStatus,
} from '@/api/writing'

const router = useRouter()
const rows = ref<TeacherSubmissionRow[]>([])
const loading = ref(true)
const statusFilter = ref('')

const STATUS_LABEL: Record<WritingStatus, string> = {
  PENDING: '等待处理',
  AI_DRAFTING: 'AI 生成中',
  AWAITING_REVIEW: '待审核',
  CORRECTED: '已批改',
  FAILED: '生成失败',
}

function statusType(s: WritingStatus): 'success' | 'info' | 'warning' | 'danger' {
  if (s === 'CORRECTED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'AI_DRAFTING' || s === 'PENDING') return 'warning'
  return 'info'
}

const FILTERS = [
  { value: '', label: '待处理' },
  { value: 'AWAITING_REVIEW', label: '待审核' },
  { value: 'AI_DRAFTING', label: 'AI 生成中' },
  { value: 'FAILED', label: '生成失败' },
  { value: 'CORRECTED', label: '已批改' },
]

async function load() {
  loading.value = true
  try {
    rows.value = await listTeacherSubmissions(statusFilter.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container teacher-list-page">
    <div class="page-head">
      <h1 class="page-title">写作批改工作台</h1>
    </div>

    <div class="seg">
      <button
        v-for="f in FILTERS"
        :key="f.value"
        :class="['seg-btn', { active: statusFilter === f.value }]"
        @click="statusFilter = f.value; load()"
      >{{ f.label }}</button>
    </div>

    <div v-if="loading" class="muted-center">加载中…</div>
    <el-table
      v-else
      :data="rows"
      stripe
      style="width: 100%"
      empty-text="暂无数据"
      @row-click="(r: TeacherSubmissionRow) => router.push(`/writing/review/${r.id}`)"
    >
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="学生" width="160">
        <template #default="{ row }">{{ row.studentName }}</template>
      </el-table-column>
      <el-table-column label="题目" min-width="240">
        <template #default="{ row }">{{ row.promptPreview }}</template>
      </el-table-column>
      <el-table-column label="字数" prop="wordCount" width="80" align="center" />
      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ STATUS_LABEL[row.status as WritingStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="90" align="center">
        <template #default="{ row }">
          <b v-if="row.overall != null">{{ row.overall }}</b>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.teacher-list-page { padding: 2.5rem 1.5rem 4rem; max-width: 1100px; margin: 0 auto; }
.page-head { margin-bottom: 1.25rem; }
.page-title { font-size: 1.75rem; font-weight: 700; margin: 0; letter-spacing: -0.02em; }

.seg {
  display: inline-flex;
  background: var(--secondary);
  border-radius: var(--radius);
  padding: 4px;
  gap: 4px;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}
.seg-btn {
  border: none;
  background: transparent;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--muted-foreground);
  cursor: pointer;
  transition: all 0.18s ease;
}
.seg-btn.active {
  background: var(--card);
  color: var(--foreground);
  box-shadow: var(--shadow-sm);
  font-weight: 600;
}
.muted-center { padding: 2rem; text-align: center; color: var(--muted-foreground); }
.muted { color: var(--muted-foreground); }
</style>
