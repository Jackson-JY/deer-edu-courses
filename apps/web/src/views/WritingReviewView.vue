<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnnotationText from '@/components/AnnotationText.vue'
import {
  getTeacherSubmission,
  saveCorrection,
  publishCorrection,
  regenerateDraft,
  createBlankCorrection,
  type AnnotationCategory,
  type ModelIdeasColumn,
  type TeacherSubmissionDetail,
  type WritingAnnotation,
  type WritingCorrection,
} from '@/api/writing'

const route = useRoute()
const router = useRouter()

const detail = ref<TeacherSubmissionDetail | null>(null)
const loading = ref(true)
let pollTimer: number | undefined

// ===== 可编辑批改数据 =====
const correctionId = ref<number | null>(null)
const published = ref(false)
const scores = reactive({ tr: 6, cc: 6, lr: 6, ga: 6 })
const comments = reactive({ tr: '', cc: '', lr: '', ga: '' })
const closingRemark = ref('')
const modelEssay = ref('')
const ideas = ref<ModelIdeasColumn[]>([
  { heading: 'NEGATIVE', points: [''] },
  { heading: 'POSITIVE', points: [''] },
])

interface EditableAnn {
  key: number
  category: AnnotationCategory
  startPos: number
  endPos: number
  quote: string
  note: string
  suggestion: string
  seq?: number
}
const annotations = ref<EditableAnn[]>([])
let annKeySeq = 1
const pickedKey = ref<number | null>(null)
const cardRefs = new Map<number, HTMLElement>()

const CATEGORIES: { value: AnnotationCategory; label: string }[] = [
  { value: 'G', label: 'G 语法' },
  { value: 'V', label: 'V 词汇' },
  { value: 'C', label: 'C 衔接' },
  { value: 'T', label: 'T 任务' },
]

const computedOverall = computed(() => snapHalf((scores.tr + scores.cc + scores.lr + scores.ga) / 4))

function snapHalf(v: number): number {
  return Math.round(v * 2) / 2
}

function stepScore(field: keyof typeof scores, delta: number) {
  if (published.value) return
  const v = snapHalf(scores[field] + delta)
  scores[field] = Math.min(9, Math.max(4, v))
}

// ===== 加载与初始化 =====

async function load() {
  try {
    detail.value = await getTeacherSubmission(route.params.id as string)
    initFromDetail()
    if (detail.value.status === 'AI_DRAFTING' || detail.value.status === 'PENDING') {
      pollTimer = window.setTimeout(load, 6000)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function initFromDetail() {
  const c = detail.value?.correction
  if (!c) {
    correctionId.value = null
    return
  }
  fillCorrection(c)
}

function fillCorrection(c: WritingCorrection) {
  correctionId.value = c.id
  published.value = c.status === 'PUBLISHED'
  scores.tr = c.scores.tr ?? 6
  scores.cc = c.scores.cc ?? 6
  scores.lr = c.scores.lr ?? 6
  scores.ga = c.scores.ga ?? 6
  comments.tr = c.comments.tr ?? ''
  comments.cc = c.comments.cc ?? ''
  comments.lr = c.comments.lr ?? ''
  comments.ga = c.comments.ga ?? ''
  closingRemark.value = c.closingRemark ?? ''
  modelEssay.value = c.modelEssay ?? ''

  const cols = (c.modelIdeas ?? []).slice(0, 2).map((col) => ({
    heading: col.heading,
    points: col.points.length ? [...col.points] : [''],
  }))
  while (cols.length < 2) cols.push({ heading: cols.length ? 'POSITIVE' : 'NEGATIVE', points: [''] })
  ideas.value = cols

  annotations.value = c.annotations.map((a: WritingAnnotation) => ({
    key: annKeySeq++,
    category: a.category,
    startPos: a.startPos,
    endPos: a.endPos,
    quote: a.quote,
    note: a.note,
    suggestion: a.suggestion,
    seq: a.seq,
  }))
}

onMounted(load)
onBeforeUnmount(() => { if (pollTimer) clearTimeout(pollTimer) })

// ===== 原文交互 =====

function onRange(payload: { start: number; end: number; text: string }) {
  if (published.value) return
  const key = annKeySeq++
  annotations.value.push({
    key,
    category: 'V',
    startPos: payload.start,
    endPos: payload.end,
    quote: payload.text,
    note: '',
    suggestion: '',
  })
  pickCard(key)
}

function onPick(ann: WritingAnnotation) {
  const hit = annotations.value.find(
    (a) => a.startPos === ann.startPos && a.endPos === ann.endPos,
  )
  if (hit) pickCard(hit.key)
}

function pickCard(key: number) {
  pickedKey.value = key
  nextTick(() => cardRefs.get(key)?.scrollIntoView({ behavior: 'smooth', block: 'center' }))
}

function setCardRef(key: number, el: Element | unknown) {
  if (el instanceof HTMLElement) cardRefs.set(key, el)
  else cardRefs.delete(key)
}

function removeAnn(key: number) {
  annotations.value = annotations.value.filter((a) => a.key !== key)
  if (pickedKey.value === key) pickedKey.value = null
}

// ===== 思路提纲编辑 =====

function addPoint(col: ModelIdeasColumn) {
  col.points.push('')
}
function removePoint(col: ModelIdeasColumn, i: number) {
  col.points.splice(i, 1)
  if (!col.points.length) col.points.push('')
}

// ===== 保存/发布/重生成 =====

function buildPayload() {
  return {
    scores: {
      tr: snapHalf(scores.tr),
      cc: snapHalf(scores.cc),
      lr: snapHalf(scores.lr),
      ga: snapHalf(scores.ga),
    },
    comments: {
      tr: comments.tr.trim(),
      cc: comments.cc.trim(),
      lr: comments.lr.trim(),
      ga: comments.ga.trim(),
    },
    closingRemark: closingRemark.value.trim(),
    modelIdeas: ideas.value.map((col) => ({
      heading: col.heading.trim() || 'IDEAS',
      points: col.points.map((p) => p.trim()).filter(Boolean),
    })),
    modelEssay: modelEssay.value.trim(),
    annotations: annotations.value.map((a) => ({
      category: a.category,
      startPos: a.startPos,
      endPos: a.endPos,
      quote: a.quote,
      note: a.note.trim(),
      suggestion: a.suggestion.trim(),
    })),
  }
}

async function ensureCorrectionExists(): Promise<number | null> {
  if (correctionId.value != null) return correctionId.value
  const c = await createBlankCorrection(route.params.id as string)
  fillCorrection(c)
  return c.id
}

const saving = ref(false)

async function onSave() {
  saving.value = true
  try {
    const id = await ensureCorrectionExists()
    if (id == null) return
    await saveCorrection(id, buildPayload())
    ElMessage.success('草稿已保存')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onPublish() {
  try {
    await ElMessageBox.confirm(
      '确认发布批改？发布后学生将立即看到，且发布内容不可再编辑。',
      '发布确认',
      { type: 'warning', confirmButtonText: '确认发布', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    const id = await ensureCorrectionExists()
    if (id == null) return
    await saveCorrection(id, buildPayload())
    await publishCorrection(id)
    ElMessage.success('批改已发布')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '发布失败')
  } finally {
    saving.value = false
  }
}

async function onRegenerate() {
  try {
    await ElMessageBox.confirm(
      '将重新调用 AI 生成草稿，当前未保存的编辑会被覆盖，是否继续？',
      '重新生成 AI 草稿',
      { type: 'warning', confirmButtonText: '重新生成', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await regenerateDraft(route.params.id as string)
    ElMessage.success('已触发，请稍候，页面将自动刷新')
    loading.value = true
    pollTimer = window.setTimeout(load, 8000)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const statusLabel = computed(() => {
  switch (detail.value?.status) {
    case 'PENDING': return '等待处理'
    case 'AI_DRAFTING': return 'AI 生成中'
    case 'AWAITING_REVIEW': return '待审核'
    case 'CORRECTED': return '已批改'
    case 'FAILED': return '生成失败'
    default: return ''
  }
})
</script>

<template>
  <div class="review-page" v-loading="loading">
    <!-- 顶部操作条 -->
    <div class="topbar">
      <el-button link @click="router.push('/writing/teacher')">← 返回列表</el-button>
      <div class="topbar-title">
        <template v-if="detail">
          <b>{{ detail.studentName }}</b>
          <span class="muted">作文 #{{ detail.id }} · {{ detail.wordCount }} 词</span>
          <el-tag size="small" :type="published ? 'success' : 'info'">
            {{ published ? '已发布' : statusLabel }}
          </el-tag>
        </template>
      </div>
      <div class="topbar-actions">
        <el-button :disabled="published || saving" @click="onRegenerate">重新生成 AI 草稿</el-button>
        <el-button :disabled="published || saving" @click="onSave">保存草稿</el-button>
        <el-button type="primary" :disabled="published || saving" @click="onPublish">发布批改</el-button>
      </div>
    </div>

    <template v-if="detail">
      <div class="workbench">
        <!-- 左：原文 -->
        <div class="left-col">
          <div class="block">
            <div class="block-title">题目</div>
            <div class="prompt-box">{{ detail.taskPrompt }}</div>
          </div>

          <div class="block">
            <div class="block-title">
              作文原文
              <span v-if="!published" class="hint">在原文上划选即可添加批注</span>
            </div>
            <div class="legend">
              <span v-for="c in CATEGORIES" :key="c.value">
                <i class="dot" :class="`d-${c.value.toLowerCase()}`" />{{ c.label }}
              </span>
            </div>
            <div class="essay-box">
              <AnnotationText
                :content="detail.content"
                :annotations="annotations"
                :selectable="!published"
                @range="onRange"
                @pick="onPick"
              />
            </div>
          </div>
        </div>

        <!-- 右：编辑面板 -->
        <div class="right-col">
          <!-- 无草稿提示 -->
          <div v-if="correctionId === null" class="no-draft card">
            <p>当前还没有批改草稿。</p>
            <el-button type="primary" @click="onRegenerate">生成 AI 草稿</el-button>
            <el-button @click="onSave">手工新建批改</el-button>
          </div>

          <template v-else>
            <!-- 评分 -->
            <div class="block card">
              <div class="block-title">
                评分
                <span class="overall-tag">总分 {{ computedOverall }}</span>
              </div>
              <div class="score-grid">
                <div v-for="c in [
                  { f: 'tr' as const, label: 'TR 任务' },
                  { f: 'cc' as const, label: 'CC 衔接' },
                  { f: 'lr' as const, label: 'LR 词汇' },
                  { f: 'ga' as const, label: 'GRA 语法' },
                ]" :key="c.f" class="score-cell">
                  <span class="score-label">{{ c.label }}</span>
                  <div class="stepper">
                    <button :disabled="published" @click="stepScore(c.f, -0.5)">−</button>
                    <span class="score-val">{{ scores[c.f] }}</span>
                    <button :disabled="published" @click="stepScore(c.f, 0.5)">+</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 四项评语 -->
            <div class="block card">
              <div class="block-title">分项评语（中文）</div>
              <div class="comment-list">
                <label v-for="c in [
                  { f: 'tr' as const, label: 'TR 任务回应' },
                  { f: 'cc' as const, label: 'CC 衔接连贯' },
                  { f: 'lr' as const, label: 'LR 词汇' },
                  { f: 'ga' as const, label: 'GRA 语法' },
                ]" :key="c.f">
                  {{ c.label }}
                  <el-input
                    v-model="comments[c.f]"
                    type="textarea"
                    :rows="2"
                    :disabled="published"
                  />
                </label>
              </div>
            </div>

            <!-- 批注列表 -->
            <div class="block card">
              <div class="block-title">
                批注列表（{{ annotations.length }}）
                <span class="hint">英文说明，点击原文高亮可定位</span>
              </div>
              <div v-if="annotations.length === 0" class="muted empty">暂无批注</div>
              <div
                v-for="a in annotations"
                :key="a.key"
                :ref="(el) => setCardRef(a.key, el)"
                class="ann-card"
                :class="{ picked: pickedKey === a.key }"
              >
                <div class="ann-card-head">
                  <div class="cat-btns">
                    <button
                      v-for="cat in CATEGORIES"
                      :key="cat.value"
                      :class="{ active: a.category === cat.value }"
                      :disabled="published"
                      @click="a.category = cat.value"
                    >{{ cat.value }}</button>
                  </div>
                  <span class="ann-del" @click="removeAnn(a.key)">删除</span>
                </div>
                <div class="ann-quote" :title="a.quote">{{ a.quote }}</div>
                <el-input
                  v-model="a.note"
                  type="textarea"
                  :rows="2"
                  placeholder="问题说明（英文）"
                  :disabled="published"
                />
                <el-input
                  v-model="a.suggestion"
                  placeholder="建议改写（英文）"
                  :disabled="published"
                />
              </div>
            </div>

            <!-- 结语 -->
            <div class="block card">
              <div class="block-title">简短结语</div>
              <el-input v-model="closingRemark" :disabled="published" placeholder="一句温暖的鼓励" />
            </div>

            <!-- 范文思路 -->
            <div class="block card">
              <div class="block-title">范文思路提纲</div>
              <div class="ideas-grid">
                <div v-for="(col, ci) in ideas" :key="ci" class="idea-col">
                  <el-input v-model="col.heading" :disabled="published" placeholder="栏目标题" />
                  <div v-for="pi in col.points.length" :key="pi" class="point-row">
                    <el-input v-model="col.points[pi - 1]" :disabled="published" placeholder="要点" />
                    <el-button
                      link
                      type="danger"
                      :disabled="published"
                      @click="removePoint(col, pi - 1)"
                    >删</el-button>
                  </div>
                  <el-button link type="primary" :disabled="published" @click="addPoint(col)">+ 添加要点</el-button>
                </div>
              </div>
            </div>

            <!-- 范文 -->
            <div class="block card">
              <div class="block-title">Model Essay 范文</div>
              <el-input
                v-model="modelEssay"
                type="textarea"
                :rows="12"
                :disabled="published"
              />
            </div>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.review-page { min-height: 100vh; background: var(--background); }
.muted { color: var(--muted-foreground); font-size: 12px; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border);
}
.topbar-title { display: flex; align-items: center; gap: 10px; flex: 1; }
.topbar-actions { display: flex; gap: 8px; }

.workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 1fr);
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}
@media (max-width: 1024px) {
  .workbench { grid-template-columns: 1fr; }
}

.left-col, .right-col { display: flex; flex-direction: column; gap: 16px; min-width: 0; }

.block { padding: 16px 18px; }
.block-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
}
.block-title .hint { font-weight: 400; font-size: 12px; color: var(--muted-foreground); }
.overall-tag {
  margin-left: auto;
  font-size: 13px;
  color: var(--primary);
}

.prompt-box {
  background: var(--secondary);
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  font-size: 13.5px;
  line-height: 1.8;
  white-space: pre-wrap;
}
.legend { display: flex; gap: 14px; flex-wrap: wrap; font-size: 12px; margin-bottom: 10px; }
.dot { width: 9px; height: 9px; border-radius: 2px; display: inline-block; margin-right: 4px; }
.d-g { background: #ef4444; }
.d-v { background: #8b5cf6; }
.d-c { background: #22c55e; }
.d-t { background: #3b82f6; }
.essay-box {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  background: var(--card);
}

.no-draft { padding: 2rem; text-align: center; display: flex; flex-direction: column; gap: 10px; align-items: center; }
.no-draft p { margin: 0 0 8px; }

/* 评分 */
.score-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.score-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
}
.score-label { font-size: 13px; color: var(--muted-foreground); }
.stepper { display: flex; align-items: center; gap: 10px; }
.stepper button {
  width: 26px; height: 26px;
  border: 1px solid var(--border);
  background: var(--card);
  border-radius: 6px;
  cursor: pointer;
  font-size: 15px;
}
.stepper button:disabled { opacity: 0.4; cursor: not-allowed; }
.score-val { width: 34px; text-align: center; font-weight: 700; }

.comment-list { display: flex; flex-direction: column; gap: 10px; }
.comment-list label { font-size: 13px; display: flex; flex-direction: column; gap: 6px; }

/* 批注卡 */
.ann-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: box-shadow 0.15s ease;
}
.ann-card.picked { box-shadow: 0 0 0 2px var(--primary); }
.ann-card-head { display: flex; align-items: center; justify-content: space-between; }
.cat-btns { display: flex; gap: 4px; }
.cat-btns button {
  border: 1px solid var(--border);
  background: var(--card);
  border-radius: 5px;
  font-size: 12px;
  padding: 3px 9px;
  cursor: pointer;
}
.cat-btns button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.cat-btns button:disabled { opacity: 0.5; }
.ann-del { font-size: 12px; color: var(--el-color-danger); cursor: pointer; }
.ann-quote {
  font-size: 12.5px;
  background: var(--secondary);
  border-radius: 4px;
  padding: 5px 8px;
  line-height: 1.5;
  max-height: 52px;
  overflow: hidden;
}
.empty { padding: 12px 0; text-align: center; }

/* 思路 */
.ideas-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.idea-col { display: flex; flex-direction: column; gap: 8px; }
.point-row { display: flex; align-items: center; gap: 6px; }
</style>
