<script setup lang="ts">
import { computed, ref } from 'vue'
import type { WritingAnnotation } from '@/api/writing'

const props = defineProps<{
  content: string
  annotations: WritingAnnotation[]
  /** 教师模式：允许划选添加批注 */
  selectable?: boolean
  activeSeq?: number
}>()

const emit = defineEmits<{
  (e: 'pick', annotation: WritingAnnotation, ev: MouseEvent): void
  (e: 'range', payload: { start: number; end: number; text: string }): void
}>()

const containerEl = ref<HTMLElement | null>(null)

interface Segment {
  text: string
  ann?: WritingAnnotation
}

/** 将原文按批注区间切段（批注间重叠时先到先得）。 */
const segments = computed<Segment[]>(() => {
  const anns = [...props.annotations].sort((a, b) => a.startPos - b.startPos)
  const out: Segment[] = []
  let cursor = 0
  for (const a of anns) {
    if (a.startPos < cursor || a.endPos > props.content.length) continue
    if (a.startPos > cursor) out.push({ text: props.content.slice(cursor, a.startPos) })
    out.push({ text: props.content.slice(a.startPos, a.endPos), ann: a })
    cursor = a.endPos
  }
  if (cursor < props.content.length) out.push({ text: props.content.slice(cursor) })
  return out
})

/** 教师划选结束：把选区映射为原文全局字符偏移。 */
function onMouseUp() {
  if (!props.selectable || !containerEl.value) return
  const sel = window.getSelection()
  if (!sel || sel.isCollapsed || sel.rangeCount === 0) return
  const range = sel.getRangeAt(0)
  if (!containerEl.value.contains(range.startContainer) || !containerEl.value.contains(range.endContainer)) return

  const offsetMap = buildOffsetMap(containerEl.value)
  const s = globalOffset(offsetMap, range.startContainer, range.startOffset)
  const e = globalOffset(offsetMap, range.endContainer, range.endOffset)
  if (s == null || e == null || e <= s) return

  emit('range', { start: s, end: e, text: props.content.slice(s, e) })
  sel.removeAllRanges()
}

/** 遍历容器内文本节点，记录每个文本节点在原文中的起始偏移。 */
function buildOffsetMap(root: HTMLElement): { node: Text; start: number }[] {
  const map: { node: Text; start: number }[] = []
  let acc = 0
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  let node: Node | null
  while ((node = walker.nextNode())) {
    map.push({ node: node as Text, start: acc })
    acc += node.textContent?.length || 0
  }
  return map
}

function globalOffset(
  map: { node: Text; start: number }[],
  container: Node,
  offset: number,
): number | null {
  const hit = map.find((m) => m.node === container)
  return hit ? hit.start + offset : null
}

function onAnnClick(ev: MouseEvent, ann: WritingAnnotation) {
  ev.stopPropagation()
  emit('pick', ann, ev)
}
</script>

<template>
  <div
    ref="containerEl"
    class="annotation-text"
    :class="{ selectable }"
    @mouseup="onMouseUp"
  >
    <template v-for="(seg, i) in segments" :key="i">
      <span
        v-if="seg.ann"
        class="ann"
        :class="[`ann-${seg.ann.category.toLowerCase()}`, { active: seg.ann.seq === activeSeq }]"
        @click="onAnnClick($event, seg.ann)"
      >{{ seg.text }}<sup class="ann-seq">{{ seg.ann.seq }}</sup></span>
      <template v-else>{{ seg.text }}</template>
    </template>
  </div>
</template>

<style scoped>
.annotation-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 15px;
  line-height: 2;
  color: var(--foreground);
}
.annotation-text.selectable {
  user-select: text;
}
.ann {
  border-radius: 3px;
  padding: 1px 0;
  cursor: pointer;
  transition: background 0.15s ease;
}
.ann-g { background: rgba(239, 68, 68, 0.16); border-bottom: 2px solid #ef4444; }
.ann-v { background: rgba(139, 92, 246, 0.16); border-bottom: 2px solid #8b5cf6; }
.ann-c { background: rgba(34, 197, 94, 0.16); border-bottom: 2px solid #22c55e; }
.ann-t { background: rgba(59, 130, 246, 0.16); border-bottom: 2px solid #3b82f6; }
.ann.active { box-shadow: 0 0 0 2px var(--primary); }
.ann-seq {
  font-size: 10px;
  font-weight: 700;
  margin-left: 1px;
  color: var(--muted-foreground);
}
</style>
