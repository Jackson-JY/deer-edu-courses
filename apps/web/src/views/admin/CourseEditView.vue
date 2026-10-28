<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCourse, createAdminCourse, updateAdminCourse,
  createChapter, updateChapter, deleteChapter,
  createLesson, updateLesson, deleteLesson, sortLessons,
  deleteMaterial, uploadCover, uploadMaterialFile,
  type AdminCourse, type AdminChapter, type AdminLesson, type AdminMaterial,
} from '@/api/admin'
import { uploadVideoToVod } from '@/utils/vod-upload'

const route = useRoute()
const router = useRouter()
const isCreate = route.params.id === 'create'
const courseId = isCreate ? undefined : Number(route.params.id)

const course = ref<AdminCourse>({
  title: '',
  cover: '',
  intro: '',
  price: 0,
  originalPrice: 0,
  validityType: 'PERMANENT',
  inSubscription: false,
  status: 'DRAFT',
})
const chapters = ref<AdminChapter[]>([])
const loading = ref(false)
const saving = ref(false)

const chapterForm = ref<AdminChapter>({ title: '' })
const lessonForm = ref<AdminLesson>({ title: '', freeTrial: false })
const editingChapterId = ref<number | null>(null)
const editingLessonId = ref<number | null>(null)
const activeChapterId = ref<number | null>(null)

// 视频上传状态
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadLessonId = ref<number | null>(null)
const videoFileRef = ref<HTMLInputElement | null>(null)
const pendingUploadLesson = ref<AdminLesson | null>(null)

// 封面上传状态
const uploadingCover = ref(false)
const coverFileRef = ref<HTMLInputElement | null>(null)

// 课件管理（按课时）
const materials = ref<AdminMaterial[]>([])
const uploadingMaterialLessonId = ref<number | null>(null)
const materialFileRef = ref<HTMLInputElement | null>(null)
const pendingMaterialLesson = ref<AdminLesson | null>(null)

function materialsOf(lessonId: number) {
  return materials.value.filter((m) => m.lessonId === lessonId)
}

async function load() {
  if (isCreate || !courseId) return
  loading.value = true
  try {
    const detail = await getAdminCourse(courseId)
    course.value = detail
    chapters.value = detail.chapters || []
    materials.value = detail.materials || []
  } finally {
    loading.value = false
  }
}

async function saveCourse() {
  saving.value = true
  try {
    if (isCreate) {
      const created = await createAdminCourse(course.value)
      ElMessage.success('创建成功')
      router.replace(`/admin/courses/${created.id}`)
    } else {
      await updateAdminCourse(courseId!, course.value)
      ElMessage.success('保存成功')
    }
  } catch (err: any) {
    ElMessage.error('保存课程失败：' + (err?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

async function saveChapter() {
  if (!chapterForm.value.title) return
  if (!courseId) {
    ElMessage.warning('请先保存课程')
    return
  }
  try {
    if (editingChapterId.value) {
      await updateChapter(courseId, editingChapterId.value, chapterForm.value)
    } else {
      await createChapter(courseId, chapterForm.value)
    }
    chapterForm.value = { title: '' }
    editingChapterId.value = null
    await load()
  } catch (err: any) {
    ElMessage.error('保存章节失败：' + (err?.message || '未知错误'))
  }
}

function editChapter(c: AdminChapter) {
  chapterForm.value = { ...c }
  editingChapterId.value = c.id!
}

async function removeChapter(c: AdminChapter) {
  if (!courseId) return
  try {
    await ElMessageBox.confirm(
      `确定删除章节「${c.title}」？该章节下的全部课时、课件及学习记录将一并删除，且不可恢复。`,
      '删除章节',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteChapter(courseId, c.id!)
    await load()
    ElMessage.success('章节已删除')
  } catch (err: any) {
    ElMessage.error('删除章节失败：' + (err?.message || '未知错误'))
  }
}

async function saveLesson() {
  if (!lessonForm.value.title || activeChapterId.value == null) return
  if (!courseId) return
  try {
    if (editingLessonId.value) {
      await updateLesson(courseId, editingLessonId.value, lessonForm.value)
    } else {
      await createLesson(courseId, activeChapterId.value, lessonForm.value)
    }
    lessonForm.value = { title: '', freeTrial: false }
    editingLessonId.value = null
    activeChapterId.value = null
    await load()
  } catch (err: any) {
    ElMessage.error('保存课时失败：' + (err?.message || '未知错误'))
  }
}

function editLesson(l: AdminLesson, chapterId: number) {
  lessonForm.value = { ...l }
  editingLessonId.value = l.id!
  activeChapterId.value = chapterId
}

async function removeLesson(l: AdminLesson) {
  if (!courseId) return
  try {
    await ElMessageBox.confirm(
      `确定删除课时「${l.title}」？该课时的课件及学习记录将一并删除，且不可恢复。`,
      '删除课时',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteLesson(courseId, l.id!)
    await load()
    ElMessage.success('课时已删除')
  } catch (err: any) {
    ElMessage.error('删除课时失败：' + (err?.message || '未知错误'))
  }
}

// ===== 课时排序：拖拽（桌面）+ 上下移按钮（移动端）=====
const draggingLessonId = ref<number | null>(null)
const dragChapterId = ref<number | null>(null)
const dragOverLessonId = ref<number | null>(null)

function lessonIdsOf(c: AdminChapter) {
  return (c.lessons || []).map((l) => l.id!)
}

async function persistLessonOrder(c: AdminChapter) {
  if (!courseId) return
  try {
    await sortLessons(courseId, c.id!, lessonIdsOf(c))
  } catch (err: any) {
    ElMessage.error('保存排序失败：' + (err?.message || '未知错误'))
    await load() // 失败则回滚到服务端顺序
  }
}

/** 上下移按钮：与相邻课时交换位置后保存 */
async function moveLesson(c: AdminChapter, index: number, direction: -1 | 1) {
  const list = c.lessons || []
  const target = index + direction
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
  await persistLessonOrder(c)
}

function onLessonDragStart(c: AdminChapter, l: AdminLesson) {
  draggingLessonId.value = l.id!
  dragChapterId.value = c.id!
}

function onLessonDragOver(c: AdminChapter, l: AdminLesson) {
  // 仅允许同章节内排序
  if (draggingLessonId.value == null || dragChapterId.value !== c.id) return
  if (draggingLessonId.value === l.id) return
  dragOverLessonId.value = l.id!
}

function onLessonDrop(c: AdminChapter, target: AdminLesson) {
  if (draggingLessonId.value == null || dragChapterId.value !== c.id) return
  const list = c.lessons || []
  const from = list.findIndex((l) => l.id === draggingLessonId.value)
  const to = list.findIndex((l) => l.id === target.id)
  if (from >= 0 && to >= 0 && from !== to) {
    const [item] = list.splice(from, 1)
    list.splice(to, 0, item)
    persistLessonOrder(c)
  }
  onLessonDragEnd()
}

function onLessonDragEnd() {
  draggingLessonId.value = null
  dragChapterId.value = null
  dragOverLessonId.value = null
}

// 触发文件选择
function triggerUpload(l: AdminLesson) {
  pendingUploadLesson.value = l
  videoFileRef.value?.click()
}

// 文件选择后上传到 VOD
async function onVideoFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 重置，允许重复选择
  if (!file || !pendingUploadLesson.value || !courseId) return

  const lesson = pendingUploadLesson.value
  pendingUploadLesson.value = null

  // 验证文件类型
  const validTypes = ['video/mp4', 'video/quicktime', 'video/x-msvideo', 'video/x-matroska']
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!validTypes.includes(file.type) && !['mp4', 'mov', 'avi', 'mkv', 'flv'].includes(ext || '')) {
    ElMessage.warning('仅支持 MP4/MOV/AVI/MKV 视频格式')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  uploadLessonId.value = lesson.id!

  try {
    ElMessage.info(`正在上传「${file.name}」...`)
    const result = await uploadVideoToVod(file, {
      onProgress: (p) => {
        uploadProgress.value = p
      },
    })

    // 回写 fileId 到课时
    await updateLesson(courseId, lesson.id!, {
      ...lesson,
      vodFileId: result.fileId,
    })

    ElMessage.success(`视频上传成功（fileId: ${result.fileId}）`)
    load()
  } catch (err: any) {
    ElMessage.error('视频上传失败：' + (err?.message || '未知错误'))
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    uploadLessonId.value = null
  }
}

// 封面上传
function triggerCoverUpload() {
  coverFileRef.value?.click()
}

async function onCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploadingCover.value = true
  try {
    const res = await uploadCover(file)
    course.value.cover = res.url
    ElMessage.success('封面上传成功，点击「保存课程」后生效')
  } catch (err: any) {
    ElMessage.error('封面上传失败：' + (err?.message || '未知错误'))
  } finally {
    uploadingCover.value = false
  }
}

// 课件上传（关联到具体课时）
function triggerMaterialUpload(lesson: AdminLesson) {
  if (!courseId) {
    ElMessage.warning('请先保存课程')
    return
  }
  pendingMaterialLesson.value = lesson
  materialFileRef.value?.click()
}

async function onMaterialFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  const lesson = pendingMaterialLesson.value
  pendingMaterialLesson.value = null
  if (!file || !lesson?.id || !courseId) return
  uploadingMaterialLessonId.value = lesson.id
  try {
    ElMessage.info(`正在上传「${file.name}」...`)
    await uploadMaterialFile(courseId, file, lesson.id)
    ElMessage.success('课件上传成功')
    await load()
  } catch (err: any) {
    ElMessage.error('课件上传失败：' + (err?.message || '未知错误'))
  } finally {
    uploadingMaterialLessonId.value = null
  }
}

async function removeMaterial(m: AdminMaterial) {
  if (!courseId) return
  try {
    await ElMessageBox.confirm(
      `确定删除课件「${m.name}」？删除后不可恢复。`,
      '删除课件',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteMaterial(courseId, m.id)
    await load()
    ElMessage.success('课件已删除')
  } catch (err: any) {
    ElMessage.error('删除课件失败：' + (err?.message || '未知错误'))
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <h1 class="page-title">{{ isCreate ? '新建课程' : '编辑课程' }}</h1>
    <el-form :model="course" label-width="80px" class="course-form">
      <el-form-item label="标题">
        <el-input v-model="course.title" placeholder="课程标题" />
      </el-form-item>
      <el-form-item label="封面">
        <div class="cover-row">
          <div class="cover-preview">
            <img v-if="course.cover" :src="course.cover" alt="封面预览" />
            <span v-else>无封面</span>
          </div>
          <el-input v-model="course.cover" placeholder="上传后自动填充，也可手动粘贴图片 URL" />
          <el-button type="primary" :loading="uploadingCover" @click="triggerCoverUpload">
            {{ uploadingCover ? '上传中…' : '上传封面' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="目标分">
        <el-input v-model="course.targetScore" placeholder="6.5 / 7.0" />
      </el-form-item>
      <el-form-item label="价格">
        <el-input-number v-model="course.price" :min="0" :step="100" />
      </el-form-item>
      <el-form-item label="有效期">
        <el-select v-model="course.validityType">
          <el-option label="永久" value="PERMANENT" />
          <el-option label="购买后 N 天" value="DAYS" />
          <el-option label="固定到期日" value="FIXED" />
        </el-select>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="course.intro" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存课程</el-button>
      </el-form-item>
    </el-form>

    <div v-if="!isCreate" class="section">
      <h2 class="section-title">章节与课时</h2>
      <p class="section-tip">
        每个课时可单独「上传课件」（PDF / Word doc·docx / 图片 / 音频），学生仅可在该课时在线预览。
      </p>
      <div class="chapter-form">
        <el-input v-model="chapterForm.title" placeholder="章节标题" />
        <el-button type="primary" @click="saveChapter">
          {{ editingChapterId ? '更新章节' : '添加章节' }}
        </el-button>
      </div>

      <div v-for="c in chapters" :key="c.id" class="chapter-card">
        <div class="chapter-header">
          <strong>{{ c.title }}</strong>
          <span>
            <el-button link type="primary" @click="editChapter(c)">编辑</el-button>
            <el-button link type="danger" @click="removeChapter(c)">删除</el-button>
          </span>
        </div>
        <div class="lessons">
          <div
            v-for="(l, li) in c.lessons"
            :key="l.id"
            class="lesson-block"
            :class="{ dragging: draggingLessonId === l.id, 'drag-over': dragOverLessonId === l.id }"
            draggable="true"
            @dragstart="onLessonDragStart(c, l)"
            @dragover.prevent="onLessonDragOver(c, l)"
            @drop="onLessonDrop(c, l)"
            @dragend="onLessonDragEnd"
          >
            <div class="lesson-row">
              <span class="lesson-title">
                <span class="drag-handle" title="拖拽调整顺序">⠿</span>
                {{ l.title }}
                <el-tag v-if="l.freeTrial" size="small" type="success">试看</el-tag>
                <el-tag v-if="l.vodFileId" size="small" type="info" style="margin-left: 4px">已传视频</el-tag>
                <el-tag v-if="materialsOf(l.id!).length" size="small" type="warning" style="margin-left: 4px">
                  课件 {{ materialsOf(l.id!).length }}
                </el-tag>
              </span>
              <span class="lesson-actions">
                <el-button link type="primary" :disabled="li === 0" @click="moveLesson(c, li, -1)">上移</el-button>
                <el-button
                  link
                  type="primary"
                  :disabled="li === (c.lessons?.length ?? 1) - 1"
                  @click="moveLesson(c, li, 1)"
                >下移</el-button>
                <el-button link type="primary" @click="editLesson(l, c.id!)">编辑</el-button>
                <el-button
                  link
                  type="primary"
                  :loading="uploading && uploadLessonId === l.id"
                  @click="triggerUpload(l)"
                >
                  {{ l.vodFileId ? '重新上传' : '上传视频' }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  :loading="uploadingMaterialLessonId === l.id"
                  @click="triggerMaterialUpload(l)"
                >
                  {{ uploadingMaterialLessonId === l.id ? '上传中…' : '上传课件' }}
                </el-button>
                <el-button link type="danger" @click="removeLesson(l)">删除</el-button>
              </span>
              <el-progress
                v-if="uploading && uploadLessonId === l.id"
                :percentage="uploadProgress"
                :stroke-width="6"
                style="margin-top: 4px"
              />
            </div>
            <!-- 当前课时的课件列表 -->
            <div v-if="materialsOf(l.id!).length" class="lesson-materials">
              <div v-for="m in materialsOf(l.id!)" :key="m.id" class="lm-item">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lm-icon">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                <span class="lm-name" :title="m.name">{{ m.name }}</span>
                <el-tag size="small" effect="plain">{{ m.type }}</el-tag>
                <el-button link type="danger" size="small" @click="removeMaterial(m)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-if="activeChapterId === c.id" class="lesson-form">
            <el-input v-model="lessonForm.title" placeholder="课时标题" />
            <el-checkbox v-model="lessonForm.freeTrial">试看</el-checkbox>
            <el-button type="primary" size="small" @click="saveLesson">
              {{ editingLessonId ? '更新课时' : '添加课时' }}
            </el-button>
            <el-button size="small" @click="activeChapterId = null; editingLessonId = null">取消</el-button>
          </div>
          <el-button v-else link type="primary" @click="activeChapterId = c.id ?? null">+ 添加课时</el-button>
        </div>
      </div>
    </div>

    <!-- 隐藏的视频文件选择 -->
    <input
      ref="videoFileRef"
      type="file"
      accept="video/mp4,video/quicktime,video/x-msvideo,video/x-matroska,.mp4,.mov,.avi,.mkv,.flv"
      style="display: none"
      @change="onVideoFileChange"
    >

    <!-- 隐藏的封面文件选择 -->
    <input
      ref="coverFileRef"
      type="file"
      accept="image/png,image/jpeg,image/webp,image/gif,.png,.jpg,.jpeg,.webp,.gif"
      style="display: none"
      @change="onCoverFileChange"
    >

    <!-- 隐藏的课件文件选择（按课时上传） -->
    <input
      ref="materialFileRef"
      type="file"
      accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.gif,.webp,.mp3,.aac,.m4a,.wav"
      style="display: none"
      @change="onMaterialFileChange"
    >
  </div>
</template>

<style scoped>
.course-form { max-width: 720px; }
.cover-row { display: flex; gap: 12px; align-items: center; width: 100%; }
.cover-preview {
  width: 88px;
  height: 60px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--secondary);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted-foreground);
  font-size: 12px;
}
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.section { margin-top: 32px; }
.section-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; }
.chapter-form { display: flex; gap: 10px; margin-bottom: 16px; max-width: 480px; }
.chapter-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
}
.chapter-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.lessons { padding-left: 16px; }
.lesson-block { border-bottom: 1px solid var(--border); }
.lesson-block:last-child { border-bottom: none; }
.lesson-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0;
  flex-wrap: wrap;
}
/* 课时下的课件子列表 */
.lesson-materials {
  margin: 0 0 8px;
  padding: 8px 10px;
  background: var(--secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  display: flex; flex-direction: column; gap: 4px;
}
.lm-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.lm-icon { color: var(--primary); flex-shrink: 0; }
.lm-name {
  flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  color: var(--foreground);
}
.lesson-actions { display: flex; gap: 4px; align-items: center; flex-wrap: wrap; }
.lesson-title { display: inline-flex; align-items: center; }
.drag-handle {
  cursor: grab;
  color: var(--muted-foreground);
  margin-right: 6px;
  user-select: none;
  font-size: 14px;
}
.drag-handle:active { cursor: grabbing; }
.lesson-block.dragging { opacity: 0.45; }
.lesson-block.drag-over { box-shadow: inset 0 2px 0 var(--primary); }
.lesson-form { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
.lesson-form .el-input { flex: 1; }
.section-tip { font-size: 12px; color: var(--muted-foreground); margin: 0 0 12px; }
@media (max-width: 768px) {
  .course-form { max-width: 100%; }
  .chapter-form { flex-direction: column; max-width: 100%; }
  .lesson-form { flex-wrap: wrap; }
  .lesson-row { flex-direction: column; align-items: flex-start; gap: 6px; }
  .cover-row { flex-wrap: wrap; }
}
</style>
