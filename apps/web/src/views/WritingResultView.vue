<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AnnotationText from '@/components/AnnotationText.vue'
import {
  getWritingSubmission,
  type WritingAnnotation,
  type WritingStatus,
} from '@/api/writing'

const route = useRoute()
const data = ref<Awaited<ReturnType<typeof getWritingSubmission>> | null>(null)
const loading = ref(true)

const STATUS_LABEL: Record<WritingStatus, string> = {
  PENDING: '等待处理',
  AI_DRAFTING: 'AI 批改生成中，请稍候…',
  AWAITING_REVIEW: 'AI 草稿已生成，等待教师审核发布',
  CORRECTED: '已批改',
  FAILED: 'AI 生成失败，请联系教师重新生成',
}

let timer: number | undefined

async function load() {
  try {
    data.value = await getWritingSubmission(route.params.id as string)
    if (data.value.status !== 'CORRECTED') {
      timer = window.setTimeout(load, 8000)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })

const correction = computed(() => data.value?.correction ?? null)

// ===== 批注浮动详情 =====
const activeAnn = ref<WritingAnnotation | null>(null)
const popPos = ref({ x: 0, y: 0 })

function onPick(ann: WritingAnnotation, ev: MouseEvent) {
  activeAnn.value = ann
  const cardW = 320
  const x = Math.min(ev.clientX + 12, window.innerWidth - cardW - 16)
  const y = Math.max(12, Math.min(ev.clientY - 10, window.innerHeight - 220))
  popPos.value = { x, y }
}

const CATEGORY_LABEL: Record<string, string> = {
  G: '语法 Grammar',
  V: '词汇 Vocabulary',
  C: '衔接 Cohesion',
  T: '任务 Task',
}

// ===== 评分描述符简表（雅思官方公开标准摘要） =====
const DESCRIPTORS: { band: number; tr: string; cc: string; lr: string; ga: string }[] = [
  {
    band: 9,
    tr: '全面回应，论点充分扩展',
    cc: '衔接自然，毫无痕迹',
    lr: '词汇运用自如精准',
    ga: '语法完全灵活准确',
  },
  {
    band: 8,
    tr: '充分回应，论点相关且扩展',
    cc: '信息安排合乎逻辑，衔接得当',
    lr: '词汇广泛，灵活达意，偶有小误',
    ga: '句式丰富，绝大多数无错',
  },
  {
    band: 7,
    tr: '回应各部分，立场清晰，论点扩展',
    cc: '逻辑清晰，衔接手段多样',
    lr: '词汇足够，灵活精准，搭配意识',
    ga: '复杂句多样，常无错误，控制力好',
  },
  {
    band: 6,
    tr: '回应各部分但详略不均，立场相关',
    cc: '整体连贯，衔接有效但有误',
    lr: '词汇量足够，非常用词使用不准',
    ga: '简单复杂句混用，错误不阻碍理解',
  },
  {
    band: 5,
    tr: '仅部分回应，立场表达有限',
    cc: '有推进但分段/衔接不足',
    lr: '词汇有限，错误造成阅读困难',
    ga: '句式有限，错误较多',
  },
]

function cellClass(field: 'tr' | 'cc' | 'lr' | 'ga', band: number): string {
  const score = correction.value?.scores[field]
  if (score == null) return ''
  if (score === band || score === band + 0.5) return 'hl'
  if (score - band === 0.5) return 'hl-half'
  return ''
}
</script>

<template>
  <div class="page-container result-page">
    <div v-if="loading" class="muted-center">加载中…</div>
    <template v-else-if="data">
      <!-- 未发布：状态提示 -->
      <div v-if="!correction" class="status-box card">
        <div class="spinner" v-if="data.status === 'AI_DRAFTING' || data.status === 'PENDING'" />
        <h2>{{ STATUS_LABEL[data.status] }}</h2>
        <p v-if="data.aiError" class="error">{{ data.aiError }}</p>
        <p class="muted">本页面会自动刷新，批改发布后即可查看。</p>
      </div>

      <template v-else>
        <!-- 总分卡 -->
        <div class="score-head card">
          <div class="overall">
            <div class="overall-num">{{ correction.scores.overall }}</div>
            <div class="overall-label">总分 Overall</div>
          </div>
          <div class="criteria">
            <div class="crit">
              <span class="crit-label">任务回应 TR</span>
              <span class="crit-num">{{ correction.scores.tr }}</span>
            </div>
            <div class="crit">
              <span class="crit-label">衔接连贯 CC</span>
              <span class="crit-num">{{ correction.scores.cc }}</span>
            </div>
            <div class="crit">
              <span class="crit-label">词汇 LR</span>
              <span class="crit-num">{{ correction.scores.lr }}</span>
            </div>
            <div class="crit">
              <span class="crit-label">语法 GRA</span>
              <span class="crit-num">{{ correction.scores.ga }}</span>
            </div>
          </div>
        </div>

        <!-- 题目 + 原文批注 -->
        <div class="section">
          <h2 class="section-title">题目与批改原文</h2>
          <div class="prompt-box">{{ data.taskPrompt }}</div>

          <div class="legend">
            <span><i class="dot dg" />G 语法</span>
            <span><i class="dot dv" />V 词汇</span>
            <span><i class="dot dc" />C 衔接</span>
            <span><i class="dot dt" />T 任务</span>
            <span class="muted">点击高亮处查看批注</span>
          </div>

          <div class="essay-box">
            <AnnotationText
              :content="data.content"
              :annotations="correction.annotations"
              @pick="onPick"
            />
          </div>
        </div>

        <!-- 四项评语 -->
        <div class="section">
          <h2 class="section-title">分项评语</h2>
          <div class="comment-grid">
            <div class="comment card">
              <b>任务回应 TR</b>
              <p>{{ correction.comments.tr }}</p>
            </div>
            <div class="comment card">
              <b>衔接连贯 CC</b>
              <p>{{ correction.comments.cc }}</p>
            </div>
            <div class="comment card">
              <b>词汇 LR</b>
              <p>{{ correction.comments.lr }}</p>
            </div>
            <div class="comment card">
              <b>语法 GRA</b>
              <p>{{ correction.comments.ga }}</p>
            </div>
          </div>
        </div>

        <!-- 评分描述符表 -->
        <div class="section">
          <h2 class="section-title">雅思写作评分标准参照</h2>
          <div class="table-wrap">
            <table class="descriptor-table">
              <thead>
                <tr>
                  <th>分数</th>
                  <th>任务回应</th>
                  <th>衔接连贯</th>
                  <th>词汇</th>
                  <th>语法</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in DESCRIPTORS" :key="d.band">
                  <td class="band-cell">{{ d.band }}</td>
                  <td :class="cellClass('tr', d.band)">{{ d.tr }}</td>
                  <td :class="cellClass('cc', d.band)">{{ d.cc }}</td>
                  <td :class="cellClass('lr', d.band)">{{ d.lr }}</td>
                  <td :class="cellClass('ga', d.band)">{{ d.ga }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Model Essay -->
        <div class="section">
          <h2 class="section-title">Model Essay 范文</h2>
          <div v-if="correction.modelIdeas.length" class="ideas-grid">
            <div v-for="col in correction.modelIdeas" :key="col.heading" class="ideas-col card">
              <b>{{ col.heading }}</b>
              <ul>
                <li v-for="(p, i) in col.points" :key="i">{{ p }}</li>
              </ul>
            </div>
          </div>
          <div class="model-essay card">{{ correction.modelEssay }}</div>
        </div>

        <!-- 结语 -->
        <div v-if="correction.closingRemark" class="closing-box">
          {{ correction.closingRemark }}
        </div>
      </template>
    </template>

    <!-- 批注浮动卡 -->
    <Teleport to="body">
      <div
        v-if="activeAnn"
        class="ann-pop-card"
        :style="{ left: popPos.x + 'px', top: popPos.y + 'px' }"
      >
        <div class="pop-head">
          <el-tag size="small">{{ CATEGORY_LABEL[activeAnn.category] }}</el-tag>
          <span class="muted">#{{ activeAnn.seq }}</span>
          <span class="pop-close" @click="activeAnn = null">×</span>
        </div>
        <p>{{ activeAnn.note }}</p>
        <div v-if="activeAnn.suggestion" class="pop-suggestion">
          <b>建议：</b>{{ activeAnn.suggestion }}
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.result-page { padding: 2.5rem 1.5rem 4rem; max-width: 900px; margin: 0 auto; }
.muted-center { padding: 2rem; text-align: center; color: var(--muted-foreground); }
.muted { color: var(--muted-foreground); font-size: 12px; }

.status-box { padding: 2.5rem; text-align: center; }
.status-box h2 { margin: 12px 0 8px; font-size: 18px; }
.status-box p { margin: 4px 0; font-size: 14px; color: var(--muted-foreground); }
.status-box .error { color: var(--el-color-danger); }
.spinner {
  width: 32px; height: 32px;
  margin: 0 auto;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 总分卡 */
.score-head {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 1.75rem 2rem;
  margin-bottom: 2rem;
}
.overall { text-align: center; padding-right: 2rem; border-right: 1px solid var(--border); }
.overall-num { font-size: 3.25rem; font-weight: 700; color: var(--primary); line-height: 1; }
.overall-label { font-size: 13px; color: var(--muted-foreground); margin-top: 6px; }
.criteria {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}
.crit { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.crit-label { font-size: 12px; color: var(--muted-foreground); }
.crit-num { font-size: 1.5rem; font-weight: 700; }
@media (max-width: 720px) {
  .score-head { flex-direction: column; gap: 1.25rem; }
  .overall { border-right: none; padding-right: 0; }
  .criteria { grid-template-columns: repeat(2, 1fr); }
}

.section { margin-bottom: 2.25rem; }
.section-title { font-size: 17px; font-weight: 700; margin: 0 0 14px; }
.prompt-box {
  background: var(--secondary);
  border-radius: var(--radius);
  padding: 14px 18px;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}
.legend {
  display: flex;
  gap: 18px;
  align-items: center;
  margin: 12px 0;
  font-size: 12px;
  flex-wrap: wrap;
}
.dot { width: 10px; height: 10px; border-radius: 2px; display: inline-block; margin-right: 4px; }
.dg { background: #ef4444; }
.dv { background: #8b5cf6; }
.dc { background: #22c55e; }
.dt { background: #3b82f6; }
.essay-box {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px 20px;
  background: var(--card);
}

/* 评语 */
.comment-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.comment { padding: 1rem 1.25rem; }
.comment b { font-size: 14px; }
.comment p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--muted-foreground);
}
@media (max-width: 640px) { .comment-grid { grid-template-columns: 1fr; } }

/* 描述符表 */
.table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius); }
.descriptor-table { width: 100%; border-collapse: collapse; font-size: 12.5px; min-width: 600px; }
.descriptor-table th {
  background: var(--secondary);
  padding: 10px;
  text-align: left;
  font-weight: 600;
}
.descriptor-table td { padding: 10px; border-top: 1px solid var(--border); line-height: 1.6; }
.band-cell { font-weight: 700; text-align: center; color: var(--primary); }
.descriptor-table td.hl { background: rgba(37, 99, 235, 0.14); font-weight: 600; }

/* 范文 */
.ideas-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}
.ideas-col { padding: 1rem 1.25rem; }
.ideas-col ul { margin: 8px 0 0; padding-left: 20px; line-height: 2; font-size: 13px; }
.model-essay {
  padding: 1.5rem 1.75rem;
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
}
@media (max-width: 640px) { .ideas-grid { grid-template-columns: 1fr; } }

.closing-box {
  margin-top: 2rem;
  padding: 1.1rem 1.5rem;
  border-left: 4px solid var(--success, #10b981);
  background: rgba(16, 185, 129, 0.08);
  border-radius: var(--radius);
  font-size: 15px;
  font-weight: 500;
}

.pop-anchor { position: fixed; width: 1px; height: 1px; }
</style>

<style>
.ann-pop-card {
  position: fixed;
  width: 320px;
  max-height: 260px;
  overflow: auto;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color, #ddd);
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  padding: 12px 14px;
  z-index: 3000;
}
.ann-pop-card .pop-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ann-pop-card .pop-close {
  margin-left: auto;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  color: var(--muted-foreground);
  padding: 0 4px;
}
.ann-pop-card p { margin: 8px 0; font-size: 13px; line-height: 1.7; }
.ann-pop-card .pop-suggestion {
  font-size: 13px;
  color: var(--el-color-success);
  line-height: 1.7;
}
</style>
