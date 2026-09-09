<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { CourseDetail, LessonBrief, MaterialMeta, PlayCredential } from '@ielts/shared-types'
import { fetchCourseDetail, fetchMaterialPreviewUrl, fetchPlayCredential, heartbeat } from '@/api/courses'

const route = useRoute()
const router = useRouter()
const lessonId = Number(route.params.lessonId)
const courseId = Number(route.params.courseId)
const cred = ref<PlayCredential | null>(null)
const error = ref('')
const loading = ref(true)
const wmText = ref('')
const playerContainer = ref<HTMLDivElement | null>(null)
const videoEl = ref<HTMLVideoElement | null>(null)
let player: any = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let observer: MutationObserver | null = null

// 课程详情（右侧目录 + 课件）
const detail = ref<CourseDetail | null>(null)
const showMaterials = ref(false)
const materialLoading = ref(false)

onMounted(async () => {
  try {
    cred.value = await fetchPlayCredential(lessonId)
    wmText.value = cred.value?.watermark?.text || ''
    await initPlayer()
    bindSecurity()
    startHeartbeat(lessonId)
  } catch (e: any) {
    error.value = e?.message || '播放凭证获取失败'
  } finally {
    loading.value = false
  }
  loadCourseDetail()
})

async function loadCourseDetail() {
  if (!courseId) return
  try {
    detail.value = await fetchCourseDetail(courseId)
  } catch {
    // 目录/课件加载失败不阻断播放
  }
}

function currentLessonTitle(): string {
  for (const ch of detail.value?.chapters ?? []) {
    const ls = ch.lessons.find((l) => l.id === lessonId)
    if (ls) return ls.title
  }
  return ''
}

function goLesson(ls: LessonBrief) {
  if (ls.id === lessonId) return
  if (ls.locked && !ls.freeTrial) {
    ElMessage.warning('该课时未解锁，请先开通课程')
    return
  }
  // RouterView 以 fullPath 为 key，跳转后整页重挂载、播放器重建
  router.push(`/learn/${courseId}/${ls.id}`)
}

async function openMaterial(m: MaterialMeta) {
  materialLoading.value = true
  try {
    const res = await fetchMaterialPreviewUrl(m.id)
    window.open(res.url, '_blank', 'noopener')
  } catch (e: any) {
    ElMessage.error('课件打开失败：' + (e?.message || '未知错误'))
  } finally {
    materialLoading.value = false
  }
}

onBeforeUnmount(() => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (observer) observer.disconnect()
  if (player && typeof player.destroy === 'function') player.destroy()
})

async function initPlayer() {
  if (!playerContainer.value || !videoEl.value) return
  const vodFileId = cred.value?.vodFileId
  const psign = cred.value?.psign
  const appId = cred.value?.appId

  if (vodFileId && psign && appId && appId > 0) {
    await loadTCPlayer()
    await loadTCPlayerCSS()
    // TCPlayer v4 的 getInfoV4 要求 options.t = currentTimeStamp（秒），
    // 它会把 t 放到 getplayinfo URL 参数里，VOD 服务端校验 URL.t 和 psign.currentTimeStamp 一致
    const t = parseCurrentTimeFromPsign(psign) || Math.floor(Date.now() / 1000)
    try {
      player = new (window as any).TCPlayer(videoEl.value, {
        fileID: vodFileId,
        appID: Number(appId),
        psign: psign,
        t: t,
        autoplay: false,
        controls: true,
        systemFullscreen: false,
        plugins: {
          ContinuePlay: { auto: false },
        },
      })
      // TCPlayer 构造函数内部异步调用 getplayinfo，需要等待 ready 事件
      await new Promise<void>((resolve) => {
        player.ready(() => {
          console.log('[TCPlayer] ready, duration =', player.duration())
          resolve()
        })
        // 超时保护
        setTimeout(() => resolve(), 10000)
      })
      // 监听错误事件，捕获 getplayinfo 失败、psign 校验失败等
      player.on('error', function (err: any) {
        console.error('[TCPlayer error]', err)
        error.value = `播放器错误: ${err?.message || err?.code || JSON.stringify(err)}`
      })
      player.on('loadedmetadata', function () {
        console.log('[TCPlayer] loadedmetadata, duration =', player.duration())
      })
      player.on('canplay', function () {
        console.log('[TCPlayer] canplay')
      })
      player.on('play', function () {
        console.log('[TCPlayer] play')
      })
    } catch (e: any) {
      console.error('[TCPlayer construct error]', e)
      error.value = `播放器初始化失败: ${e?.message || e}`
    }
  } else {
    playerContainer.value.innerHTML = '<div class="fallback">播放器未配置，请联系管理员上传视频</div>'
  }
}

function loadTCPlayer(): Promise<void> {
  return new Promise((resolve) => {
    if ((window as any).TCPlayer) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = 'https://web.sdk.qcloud.com/player/tcplayer/release/v4.9.0/tcplayer.v4.9.0.min.js'
    script.onload = () => resolve()
    script.onerror = () => {
      error.value = '播放器加载失败'
      resolve()
    }
    document.head.appendChild(script)
  })
}

function loadTCPlayerCSS(): Promise<void> {
  return new Promise((resolve) => {
    const id = 'tcplayer-css'
    if (document.getElementById(id)) {
      resolve()
      return
    }
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = 'https://web.sdk.qcloud.com/player/tcplayer/release/v4.9.0/tcplayer.min.css'
    link.id = id
    link.onload = () => resolve()
    link.onerror = () => resolve() // CSS 加载失败不阻断播放
    document.head.appendChild(link)
  })
}

/**
 * 从 psign（Base64(JSON+signature)）里解析出 currentTimeStamp
 * TCPlayer getInfoV4 要求 options.t 和 psign 里的 currentTimeStamp 一致
 */
function parseCurrentTimeFromPsign(psign: string): number | null {
  try {
    const jsonPart = atob(psign).split('}')[0] + '}'
    const obj = JSON.parse(jsonPart)
    return typeof obj.currentTimeStamp === 'number' ? obj.currentTimeStamp : null
  } catch {
    return null
  }
}

function bindSecurity() {
  // 禁用右键
  playerContainer.value?.addEventListener('contextmenu', (e) => e.preventDefault())
  // 禁用部分键盘快捷键
  document.addEventListener('keydown', preventShortcut)
  // 监听水印 DOM 被篡改
  const wmNodes = document.querySelectorAll('.player-watermark')
  if (wmNodes.length && window.MutationObserver) {
    observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        if (m.type === 'childList' && m.removedNodes.length) {
          pauseAndWarn()
          return
        }
        if (m.type === 'attributes') {
          const target = m.target as HTMLElement
          if (target.style.display === 'none' || target.style.visibility === 'hidden' || target.style.opacity === '0') {
            pauseAndWarn()
            return
          }
        }
      }
    })
    wmNodes.forEach((node) => {
      observer!.observe(node, { childList: true, attributes: true, attributeFilter: ['style', 'class'] })
    })
  }
}

function preventShortcut(e: KeyboardEvent) {
  if (e.ctrlKey && (e.key === 's' || e.key === 'S' || e.key === 'u' || e.key === 'U')) {
    e.preventDefault()
  }
  if (e.ctrlKey && e.shiftKey && (e.key === 'I' || e.key === 'i' || e.key === 'J' || e.key === 'j')) {
    e.preventDefault()
  }
}

function pauseAndWarn() {
  if (player && typeof player.pause === 'function') player.pause()
  ElMessage.warning('检测到播放环境异常，已暂停播放')
}

function startHeartbeat(lessonId: number) {
  heartbeatTimer = setInterval(async () => {
    try {
      const currentTime = player && typeof player.currentTime === 'function'
          ? Math.floor(player.currentTime())
          : 0
      const duration = player && typeof player.duration === 'function'
          ? Math.floor(player.duration())
          : 0
      await heartbeat(lessonId, currentTime, duration)
    } catch {
      // 心跳失败不阻断播放
    }
  }, 30000)
}
</script>

<template>
  <div class="page-container player-wrap">
    <div class="player-main">
      <div
        ref="playerContainer"
        class="stage"
        @contextmenu.prevent
      >
        <!-- TCPlayer v4 要求 video 元素，容器 div 负责水印/遮罩 -->
        <video ref="videoEl" class="tcplayer-video" playsinline webkit-playsinline></video>
        <div v-if="loading" class="stage-loading">正在初始化播放器…</div>
        <div v-else-if="error" class="stage-error">{{ error }}</div>
        <!-- 动态跑马灯水印 -->
        <div class="player-watermark wm wm-1">{{ wmText }}</div>
        <div class="player-watermark wm wm-2">{{ wmText }}</div>
        <div class="player-watermark wm wm-3">{{ wmText }}</div>
      </div>

      <div class="below">
        <div class="lesson-head">
          <h1>{{ currentLessonTitle() || '课程播放' }}</h1>
          <span v-if="cred?.lastPosition" class="resume">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
            上次看到 {{ Math.floor(cred.lastPosition / 60) }}:{{ String(cred.lastPosition % 60).padStart(2, '0') }}
          </span>
        </div>
        <div class="toolbar">
          <button
            class="btn btn-ghost btn-sm"
            :class="{ 'btn-active': showMaterials }"
            @click="showMaterials = !showMaterials"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            课件资料
            <span v-if="detail?.materials.length" class="mat-badge">{{ detail.materials.length }}</span>
          </button>
        </div>

        <!-- 课件列表面板 -->
        <div v-if="showMaterials" class="material-panel">
          <div v-if="!detail" class="mp-empty">课件加载中…</div>
          <div v-else-if="detail.materials.length === 0" class="mp-empty">本课程暂无课件</div>
          <template v-else>
            <div
              v-for="m in detail.materials"
              :key="m.id"
              class="mp-item"
              :class="{ disabled: materialLoading }"
              @click="openMaterial(m)"
            >
              <svg class="mp-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <span class="mp-name">{{ m.name }}</span>
              <span class="mp-type">{{ m.type }}</span>
              <span class="mp-open">在线预览</span>
            </div>
            <p class="mp-tip">课件仅支持在线预览，不支持下载，请勿录屏传播。</p>
          </template>
        </div>

        <p class="wm-note">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          视频含动态水印（昵称 + 手机尾号 + UID + 时间），加密播放、禁止下载与转发。
        </p>
      </div>
    </div>

    <aside class="toc">
      <div class="toc-head">
        <span>课程目录</span>
        <span v-if="detail" class="toc-course">{{ detail.title }}</span>
      </div>
      <div v-if="!detail" class="toc-empty">目录加载中…</div>
      <div v-else-if="detail.chapters.length === 0" class="toc-empty">暂无课时</div>
      <div v-else class="toc-body">
        <div v-for="ch in detail.chapters" :key="ch.id" class="toc-chapter">
          <div class="toc-ch-title">{{ ch.title }}</div>
          <div
            v-for="ls in ch.lessons"
            :key="ls.id"
            :class="['toc-lesson', { active: ls.id === lessonId, locked: ls.locked && !ls.freeTrial }]"
            @click="goLesson(ls)"
          >
            <svg class="tl-icon" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon v-if="!ls.locked || ls.freeTrial" points="5 3 19 12 5 21 5 3" fill="currentColor"/>
              <rect v-else x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path v-if="ls.locked && !ls.freeTrial" d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span class="tl-title">{{ ls.title }}</span>
            <span v-if="ls.freeTrial" class="tag-trial">试看</span>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.player-wrap {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.25rem;
  padding-top: 1.25rem;
  padding-bottom: 2.5rem;
  align-items: start;
}
.stage {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #0b1320;
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}
.tcplayer-video {
  width: 100%;
  height: 100%;
  display: block;
}
.stage-loading,
.stage-error,
.fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}
.stage-error { color: #ff9f9f; }
/* 动态水印：集中在画面顶部区域，水平轻微漂移；
   白色文字 + 深色描边阴影，在明暗画面上都清晰可辨，又不遮挡视频主体 */
.player-watermark {
  position: absolute;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.82);
  text-shadow:
    0 1px 3px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(0, 0, 0, 0.65);
  pointer-events: none;
  user-select: none;
  white-space: nowrap;
  z-index: 10;
}
/* wm-1/wm-2 左锚定（文字向右生长），wm-3 右锚定（文字向左生长，永不溢出右边缘） */
.wm-1 { top: 4%; left: 2%; animation: drift-x 9s ease-in-out infinite alternate; }
.wm-2 { top: 13%; left: 24%; animation: drift-x 11s ease-in-out infinite alternate-reverse; }
.wm-3 { top: 5%; right: 2%; animation: drift-x-l 13s ease-in-out infinite alternate; }
@keyframes drift-x {
  from { transform: translateX(0); }
  to { transform: translateX(24px); }
}
@keyframes drift-x-l {
  from { transform: translateX(0); }
  to { transform: translateX(-24px); }
}
.below { padding-top: 1rem; }
.lesson-head {
  display: flex; align-items: center; justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.lesson-head h1 { font-size: 1.25rem; font-weight: 700; margin: 0; }
.resume {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--primary);
  background: var(--primary-50, rgba(0, 122, 255, 0.08));
  padding: 6px 12px; border-radius: 999px;
}
.toolbar { display: flex; gap: 6px; margin: 14px 0; flex-wrap: wrap; }
.wm-note {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--muted-foreground);
  background: var(--secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px; margin: 0;
}
.toc {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.toc-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; font-weight: 600;
  border-bottom: 1px solid var(--border);
  background: var(--secondary);
}
.toc-empty { padding: 40px 16px; text-align: center; color: var(--muted-foreground); font-size: 13px; }
.toc-course {
  font-size: 12px; font-weight: 400; color: var(--muted-foreground);
  max-width: 170px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.toc-body { max-height: 600px; overflow-y: auto; padding: 8px 8px 12px; }
.toc-chapter { margin-bottom: 8px; }
.toc-ch-title {
  font-size: 12.5px; font-weight: 600; color: var(--muted-foreground);
  padding: 8px 8px 6px;
}
.toc-lesson {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 10px; border-radius: var(--radius-sm);
  font-size: 13.5px; line-height: 1.35; color: var(--foreground);
  cursor: pointer; transition: background 0.15s;
}
.toc-lesson:hover { background: var(--secondary); }
.tl-icon { flex-shrink: 0; color: var(--primary); }
.tl-title { flex: 1; word-break: break-all; }
.toc-lesson.locked { color: var(--muted-foreground); cursor: not-allowed; }
.toc-lesson.locked .tl-icon { color: var(--muted-foreground); }
.toc-lesson.active {
  background: var(--primary-50, rgba(0, 122, 255, 0.1));
  color: var(--primary); font-weight: 600;
}
.tag-trial {
  font-size: 10px; color: var(--primary); border: 1px solid var(--primary);
  border-radius: 4px; padding: 0 4px; flex-shrink: 0; line-height: 16px;
}

/* 课件按钮与面板 */
.mat-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px; margin-left: 2px;
  border-radius: 999px; background: var(--primary); color: #fff;
  font-size: 11px; line-height: 1;
}
.btn-active { border-color: var(--primary); color: var(--primary); }
.material-panel {
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--card); padding: 8px; margin: 0 0 14px;
}
.mp-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px; border-radius: var(--radius-sm);
  font-size: 13.5px; cursor: pointer; transition: background 0.15s;
}
.mp-item:hover { background: var(--secondary); }
.mp-item.disabled { opacity: 0.6; pointer-events: none; }
.mp-icon { color: var(--primary); flex-shrink: 0; }
.mp-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mp-type {
  font-size: 11px; color: var(--muted-foreground);
  border: 1px solid var(--border); border-radius: 4px; padding: 1px 6px; flex-shrink: 0;
}
.mp-open { font-size: 12px; color: var(--primary); flex-shrink: 0; }
.mp-empty { padding: 24px 12px; text-align: center; color: var(--muted-foreground); font-size: 13px; }
.mp-tip { margin: 6px 4px 2px; font-size: 12px; color: var(--muted-foreground); }

@media (max-width: 991px) {
  .player-wrap { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .player-wrap { gap: 12px; padding: 12px; }
  .stage { min-height: 200px; }
  .toc { max-height: 360px; }
  .toc-body { max-height: 320px; }
  /* 窄屏水印缩小、靠左两行排列，隐藏第 3 条，避免长文案溢出或相互重叠 */
  .player-watermark { font-size: 10.5px; }
  .wm-1 { top: 4%; left: 2%; }
  .wm-2 { top: 13%; left: 2%; }
  .wm-3 { display: none; }
}
</style>
