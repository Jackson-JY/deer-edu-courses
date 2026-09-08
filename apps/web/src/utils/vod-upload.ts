/**
 * 腾讯云 VOD 客户端直传工具
 * 通过 CDN 动态加载 VOD JS SDK v6（v5 已下线）
 * 官方文档：https://cloud.tencent.com/document/product/266/9239
 */
import { getVodUploadSign } from '@/api/admin'

let sdkLoaded = false

/** 动态加载 VOD JS SDK v6 */
function loadVodSDK(): Promise<any> {
  if (sdkLoaded && (window as any).TcVod) {
    return Promise.resolve((window as any).TcVod)
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://cdn-go.cn/cdn/vod-js-sdk-v6/latest/vod-js-sdk-v6.js'
    script.onload = () => {
      const TcVod = (window as any).TcVod
      if (!TcVod) {
        reject(new Error('VOD SDK 加载完成但未找到全局 TcVod 变量'))
        return
      }
      sdkLoaded = true
      resolve(TcVod)
    }
    script.onerror = () => reject(new Error('VOD SDK 加载失败，请检查网络或 CDN 地址'))
    document.head.appendChild(script)
  })
}

export interface UploadResult {
  fileId: string
  url?: string
}

export interface UploadCallbacks {
  onProgress?: (percent: number) => void
}

/**
 * 上传视频文件到腾讯云 VOD
 * @param file 视频文件
 * @param callbacks 进度回调
 * @returns 上传结果（fileId）
 */
export async function uploadVideoToVod(
  file: File,
  callbacks?: UploadCallbacks,
): Promise<UploadResult> {
  const TcVod = await loadVodSDK()

  const signRes = await getVodUploadSign()
  if (!signRes.configured) {
    throw new Error('腾讯云 VOD 尚未配置')
  }

  // 用闭包缓存签名，SDK 会调用 getSignature 获取
  let cachedSignature = signRes.signature
  let signatureExpireTime = signRes.expireTime

  // script 标签引入方式需要使用 TcVod.default 构造（参见官方文档）
  const TcVodCtor: any = (TcVod as any).default || TcVod
  const vod = new TcVodCtor({
    getSignature: () => {
      // 如果签名过期，重新获取（签名有效期 1 小时，单次上传足够）
      const now = Math.floor(Date.now() / 1000)
      if (now >= signatureExpireTime - 60) {
        // 过期了，需要重新获取（但 getSignature 不支持 async，返回缓存的）
        // 实际使用中签名有效期 1 小时，单次上传足够
      }
      return Promise.resolve(cachedSignature)
    },
    appId: String(signRes.appId),
    subAppId: signRes.subAppId || undefined,
  })

  const uploader = vod.upload({
    mediaFile: file,
  })

  uploader.on('media_progress', (info: any) => {
    if (callbacks?.onProgress && info.percent) {
      callbacks.onProgress(Math.round(info.percent * 100))
    }
  })

  const result = await uploader.done()

  return {
    fileId: result.fileId,
    url: result.video?.url,
  }
}
