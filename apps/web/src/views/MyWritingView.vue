<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyWriting, type WritingSubmissionRow, type WritingStatus } from '@/api/writing'

const router = useRouter()
const rows = ref<WritingSubmissionRow[]>([])
const loading = ref(true)

const STATUS_LABEL: Record<WritingStatus, string> = {
  PENDING: '等待处理',
  AI_DRAFTING: 'AI 批改生成中',
  AWAITING_REVIEW: '等待教师审核',
  CORRECTED: '已批改',
  FAILED: '生成失败',
}

function statusType(s: WritingStatus): 'success' | 'info' | 'warning' | 'danger' {
  if (s === 'CORRECTED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'AI_DRAFTING' || s === 'PENDING') return 'warning'
  return 'info'
}

function fmt(t: string): string {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

onMounted(async () => {
  try {
    rows.value = await listMyWriting()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container my-page">
    <div class="page-head">
      <h1 class="page-title">我的写作</h1>
      <el-button type="primary" @click="router.push('/writing/submit')">提交新作文</el-button>
    </div>

    <div v-if="loading" class="muted-center">加载中…</div>
    <div v-else-if="rows.length" class="rows">
      <div
        v-for="r in rows"
        :key="r.id"
        class="row card"
        @click="router.push(`/writing/${r.id}`)"
      >
        <div class="row-main">
          <div class="row-title">{{ r.promptPreview || '未命名题目' }}</div>
          <div class="row-meta">
            <span>{{ r.wordCount }} 词</span>
            <span>{{ fmt(r.createdAt) }}</span>
          </div>
        </div>
        <div class="row-side">
          <el-tag v-if="r.status === 'CORRECTED'" :type="statusType(r.status)">
            {{ r.overall }} 分
          </el-tag>
          <el-tag v-else :type="statusType(r.status)">
            {{ STATUS_LABEL[r.status] }}
          </el-tag>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <p>还没有提交过作文，去提交第一篇吧</p>
      <el-button type="primary" @click="router.push('/writing/submit')">提交作文</el-button>
    </div>
  </div>
</template>

<style scoped>
.my-page { padding: 2.5rem 1.5rem 4rem; max-width: 860px; margin: 0 auto; }
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.page-title { font-size: 1.75rem; font-weight: 700; margin: 0; letter-spacing: -0.02em; }

.rows { display: flex; flex-direction: column; gap: 0.75rem; }
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: box-shadow 0.18s ease;
}
.row:hover { box-shadow: var(--shadow-md); }
.row-main { min-width: 0; flex: 1; }
.row-title {
  font-weight: 600;
  margin-bottom: 0.375rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--muted-foreground);
}
.row-side { flex-shrink: 0; margin-left: 1rem; }

.muted-center { padding: 2rem; text-align: center; color: var(--muted-foreground); }
.empty-state { text-align: center; padding: 3rem 1rem; color: var(--muted-foreground); }
.empty-state p { margin: 0 0 1.25rem; }
</style>
