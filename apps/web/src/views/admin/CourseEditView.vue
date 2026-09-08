<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getAdminCourse, createAdminCourse, updateAdminCourse,
  createChapter, updateChapter, deleteChapter,
  createLesson, updateLesson, deleteLesson,
  createMaterial, deleteMaterial,
  type AdminCourse, type AdminChapter, type AdminLesson,
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

// 课件管理
const materials = ref<any[]>([])
const materialForm = ref({ name: '', type: 'PDF', cosKey: '', previewOnly: true })

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
  if (editingChapterId.value) {
    await updateChapter(courseId, editingChapterId.value, chapterForm.value)
  } else {
    await createChapter(courseId, chapterForm.value)
  }
  chapterForm.value = { title: '' }
  editingChapterId.value = null
  load()
}

function editChapter(c: AdminChapter) {
  chapterForm.value = { ...c }
  editingChapterId.value = c.id!
}

async function removeChapter(c: AdminChapter) {
  if (!courseId) return
  await deleteChapter(courseId, c.id!)
  load()
}

async function saveLesson() {
  if (!lessonForm.value.title || activeChapterId.value == null) return
  if (!courseId) return
  if (editingLessonId.value) {
    await updateLesson(courseId, editingLessonId.value, lessonForm.value)
  } else {
    await createLesson(courseId, activeChapterId.value, lessonForm.value)
  }
  lessonForm.value = { title: '', freeTrial: false }
  editingLessonId.value = null
  activeChapterId.value = null
  load()
}

function editLesson(l: AdminLesson, chapterId: number) {
  lessonForm.value = { ...l }
  editingLessonId.value = l.id!
  activeChapterId.value = chapterId
}

async function removeLesson(l: AdminLesson) {
  if (!courseId) return
  await deleteLesson(courseId, l.id!)
  load()
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

async function addMaterial() {
  if (!courseId) {
    ElMessage.warning('请先保存课程')
    return
  }
  if (!materialForm.value.name || !materialForm.value.cosKey) {
    ElMessage.warning('请填写课件名称和文件路径')
    return
  }
  await createMaterial(courseId, materialForm.value)
  materialForm.value = { name: '', type: 'PDF', cosKey: '', previewOnly: true }
  load()
  ElMessage.success('课件已添加')
}

async function removeMaterial(m: any) {
  if (!courseId) return
  await deleteMaterial(courseId, m.id)
  load()
  ElMessage.success('课件已删除')
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
        <el-input v-model="course.cover" placeholder="封面图片 URL" />
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
          <div v-for="l in c.lessons" :key="l.id" class="lesson-row">
            <span>
              {{ l.title }}
              <el-tag v-if="l.freeTrial" size="small" type="success">试看</el-tag>
              <el-tag v-if="l.vodFileId" size="small" type="info" style="margin-left: 4px">已传</el-tag>
            </span>
            <span class="lesson-actions">
              <el-button link type="primary" @click="editLesson(l, c.id!)">编辑</el-button>
              <el-button
                link
                type="primary"
                :loading="uploading && uploadLessonId === l.id"
                @click="triggerUpload(l)"
              >
                {{ l.vodFileId ? '重新上传' : '上传视频' }}
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

    <div v-if="!isCreate" class="section">
      <h2 class="section-title">课件管理</h2>
      <p class="section-tip">课件仅支持在线预览，禁止下载</p>
      <div class="material-form">
        <el-input v-model="materialForm.name" placeholder="课件名称" style="width: 180px" />
        <el-select v-model="materialForm.type" style="width: 120px">
          <el-option label="PDF" value="PDF" />
          <el-option label="图片" value="IMAGE" />
          <el-option label="音频" value="AUDIO" />
        </el-select>
        <el-input v-model="materialForm.cosKey" placeholder="COS 文件路径 / Key" style="width: 220px" />
        <el-checkbox v-model="materialForm.previewOnly">仅预览</el-checkbox>
        <el-button type="primary" @click="addMaterial">添加</el-button>
      </div>
      <el-table :data="materials" border style="margin-top: 12px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column prop="cosKey" label="文件路径" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 隐藏的视频文件选择 -->
    <input
      ref="videoFileRef"
      type="file"
      accept="video/mp4,video/quicktime,video/x-msvideo,video/x-matroska,.mp4,.mov,.avi,.mkv,.flv"
      style="display: none"
      @change="onVideoFileChange"
    >
  </div>
</template>

<style scoped>
.course-form { max-width: 640px; }
.section { margin-top: 32px; }
.section-title { font-size: 18px; font-weight: 700; margin-bottom: 16px; }
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
.lesson-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}
.lesson-actions { display: flex; gap: 4px; align-items: center; flex-wrap: wrap; }
.lesson-form { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
.lesson-form .el-input { flex: 1; }
.section-tip { font-size: 12px; color: var(--muted-foreground); margin: 0 0 12px; }
.material-form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
@media (max-width: 768px) {
  .course-form { max-width: 100%; }
  .chapter-form { flex-direction: column; max-width: 100%; }
  .lesson-form { flex-wrap: wrap; }
  .lesson-row { flex-direction: column; align-items: flex-start; gap: 6px; }
  .material-form { flex-direction: column; align-items: stretch; }
}
</style>
