<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { PlayCredential } from '@ielts/shared-types'
import { fetchPlayCredential, heartbeat } from '@/api/courses'

const route = useRoute()
const cred = ref<PlayCredential | null>(null)
const error = ref('')
const loading = ref(true)
const wmText = ref('')
const playerContainer = ref<HTMLDivElement | null>(null)
const videoEl = ref<HTMLVideoElement | null>(null)
let player: any = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let observer: MutationObserver | null = null

onMounted(async () => {
  const lessonId = Number(route.params.lessonId)
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
})

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
          <h1>课程播放</h1>
          <span v-if="cred?.lastPosition" class="resume">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
            上次看到 {{ Math.floor(cred.lastPosition / 60) }}:{{ String(cred.lastPosition % 60).padStart(2, '0') }}
          </span>
        </div>
        <div class="toolbar">
          <button class="btn btn-ghost btn-sm">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            本节课件
          </button>
          <button class="btn btn-ghost btn-sm">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
            笔记
          </button>
          <button class="btn btn-ghost btn-sm">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            收藏
          </button>
        </div>
        <p class="wm-note">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          视频含动态水印（UID + 时间），加密播放、禁止下载与转发。
        </p>
      </div>
    </div>

    <aside class="toc">
      <div class="toc-head">
        <span>课程目录</span>
      </div>
      <div class="toc-empty">请在课程详情页选择课时</div>
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
.player-watermark {
  position: absolute;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.28);
  transform: rotate(-14deg);
  pointer-events: none;
  user-select: none;
  white-space: nowrap;
  z-index: 10;
}
.wm-1 { top: 20%; left: 10%; animation: drift 8s linear infinite alternate; }
.wm-2 { top: 55%; left: 55%; animation: drift 10s linear infinite alternate-reverse; }
.wm-3 { top: 78%; left: 25%; animation: drift 12s linear infinite alternate; }
@keyframes drift {
  from { transform: rotate(-14deg) translateX(0); }
  to { transform: rotate(-14deg) translateX(40px); }
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
@media (max-width: 991px) {
  .player-wrap { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .player-wrap { gap: 12px; padding: 12px; }
  .stage { min-height: 200px; }
  .toc { max-height: 300px; }
}
</style>
