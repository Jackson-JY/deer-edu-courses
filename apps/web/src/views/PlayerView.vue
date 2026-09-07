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
  if (!playerContainer.value) return
  const vodFileId = cred.value?.vodFileId
  const psign = cred.value?.psign
  const appId = cred.value?.appId

  if (vodFileId && psign && appId && appId > 0) {
    await loadTcPlayer()
    player = new (window as any).TcPlayer(playerContainer.value, {
      fileID: vodFileId,
      appID: String(appId),
      psign: psign,
      autoplay: false,
      width: '100%',
      height: '100%',
      controls: true,
      systemFullscreen: false,
      plugins: {
        ContinuePlay: { auto: false },
      },
    })
  } else {
    // Fallback：未配置 VOD 时给出提示，同时保留水印与防下载 UI
    playerContainer.value.innerHTML = '<div class="fallback">播放器未配置，请联系管理员上传视频</div>'
  }
}

function loadTcPlayer(): Promise<void> {
  return new Promise((resolve) => {
    if ((window as any).TcPlayer) {
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
  <div class="container player-wrap">
    <div class="player-main">
      <div
        ref="playerContainer"
        class="stage"
        @contextmenu.prevent
      >
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
            <el-icon><RefreshLeft /></el-icon> 上次看到 {{ Math.floor(cred.lastPosition / 60) }}:{{ String(cred.lastPosition % 60).padStart(2, '0') }}
          </span>
        </div>
        <div class="toolbar">
          <el-button text><el-icon><Document /></el-icon> 本节课件</el-button>
          <el-button text><el-icon><EditPen /></el-icon> 笔记</el-button>
          <el-button text><el-icon><Star /></el-icon> 收藏</el-button>
        </div>
        <p class="wm-note">
          <el-icon><Lock /></el-icon>
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
  gap: 20px;
  padding-top: 20px;
  padding-bottom: 40px;
  align-items: start;
}
.stage {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #0b1320;
  border-radius: var(--r-lg);
  overflow: hidden;
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
.below { padding-top: 16px; }
.lesson-head {
  display: flex; align-items: center; justify-content: space-between;
}
.lesson-head h1 { font-size: 20px; }
.resume {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--brand-600);
  background: var(--brand-50); padding: 6px 12px; border-radius: 999px;
}
.toolbar { display: flex; gap: 6px; margin: 14px 0; }
.wm-note {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--text-3);
  background: var(--bg-soft); border: 1px solid var(--line);
  border-radius: var(--r-sm); padding: 10px 12px; margin: 0;
}
.toc {
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.toc-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; font-weight: 600;
  border-bottom: 1px solid var(--line); background: var(--bg-soft);
}
.toc-empty { padding: 40px 16px; text-align: center; color: var(--text-3); font-size: 13px; }
@media (max-width: 980px) {
  .player-wrap { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .player-wrap { gap: 12px; padding: 12px; }
  .player-area { min-height: 200px; }
  .toc { max-height: 300px; }
}
</style>
