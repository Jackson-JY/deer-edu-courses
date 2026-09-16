<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWritingQuota, submitWriting, type WritingQuota } from '@/api/writing'

const router = useRouter()
const taskPrompt = ref('')
const content = ref('')
const submitting = ref(false)
const quota = ref<WritingQuota | null>(null)
const quotaLoading = ref(true)

onMounted(async () => {
  try {
    quota.value = await getWritingQuota()
  } catch {
    // 额度加载失败不阻塞表单（提交时后端仍会校验）
  } finally {
    quotaLoading.value = false
  }
})

const wordCount = computed(() => {
  const t = content.value.trim()
  if (!t) return 0
  return t.split(/\s+/).filter(Boolean).length
})

const tooShort = computed(() => content.value.trim().length > 0 && wordCount.value < 250)

async function onSubmit() {
  if (!taskPrompt.value.trim()) {
    ElMessage.warning('请粘贴写作题目')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请粘贴作文正文')
    return
  }
  submitting.value = true
  try {
    const res = await submitWriting({
      taskPrompt: taskPrompt.value,
      content: content.value,
    })
    ElMessage.success('提交成功，AI 批改生成后教师将尽快审核')
    router.push(`/writing/${res.id}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container submit-page">
    <div class="page-head">
      <h1 class="page-title">写作批改</h1>
      <p class="page-desc">粘贴雅思写作题目与你的作文，AI 先生成批改草稿，教师审核后发布。</p>
    </div>

    <!-- 未开通状态 -->
    <div v-if="!quotaLoading && quota && !quota.granted" class="locked card">
      <h2>写作批改未开通</h2>
      <p>该功能需要管理员开通后才能使用，请联系老师/管理员为你开通写作批改权限。</p>
      <el-button @click="router.push('/my/writing')">查看已提交的作文</el-button>
    </div>

    <template v-else>
      <!-- 额度卡 -->
      <div v-if="!quotaLoading && quota && quota.granted" class="quota-bar card">
        <span>
          本月额度
          <b :class="{ exhausted: quota.remaining === 0 }">{{ quota.used }}/{{ quota.limit }}</b> 篇
        </span>
        <span class="muted">每月 {{ quota.limit }} 篇，下月自动恢复</span>
        <el-tag v-if="quota.remaining === 0" type="warning" size="small">本月额度已用完</el-tag>
      </div>

      <div class="form card">
        <label class="field-label">
          写作题目
          <span class="hint">完整粘贴原题（含题目要求）</span>
        </label>
        <el-input
          v-model="taskPrompt"
          type="textarea"
          :rows="4"
          placeholder="例如：In some countries, the proportion of older people is steadily increasing..."
          resize="vertical"
        />

        <label class="field-label">
          作文正文
          <span class="hint" :class="{ warn: tooShort }">
            当前 {{ wordCount }} 词<span v-if="tooShort">（Task 2 建议不少于 250 词）</span>
          </span>
        </label>
        <el-input
          v-model="content"
          type="textarea"
          :rows="16"
          placeholder="在此粘贴你的作文…"
          resize="vertical"
        />

        <div class="actions">
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!!quota && quota.granted && quota.remaining === 0"
            @click="onSubmit"
          >提交作文</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.submit-page { padding: 2.5rem 1.5rem 4rem; max-width: 860px; margin: 0 auto; }
.page-head { margin-bottom: 1.5rem; }
.page-title { font-size: 1.75rem; font-weight: 700; margin: 0 0 0.375rem; letter-spacing: -0.02em; }
.page-desc { margin: 0; font-size: 14px; color: var(--muted-foreground); }

.locked { padding: 3rem 2rem; text-align: center; }
.locked h2 { margin: 0 0 10px; font-size: 18px; }
.locked p { margin: 0 0 20px; font-size: 14px; color: var(--muted-foreground); }

.quota-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0.85rem 1.25rem;
  margin-bottom: 1rem;
  font-size: 14px;
}
.quota-bar b { margin: 0 3px; }
.quota-bar b.exhausted { color: var(--el-color-warning); }
.quota-bar .muted { color: var(--muted-foreground); font-size: 12px; margin-right: auto; }

.form { padding: 1.5rem; display: flex; flex-direction: column; gap: 10px; }
.field-label {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  margin-top: 6px;
}
.field-label .hint { font-weight: 400; font-size: 12px; color: var(--muted-foreground); }
.field-label .hint.warn { color: var(--el-color-warning); }
.actions { display: flex; justify-content: flex-end; margin-top: 10px; }
</style>
