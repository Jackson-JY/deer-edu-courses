<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getAdminCourse, createAdminCourse, updateAdminCourse,
  createChapter, updateChapter, deleteChapter,
  createLesson, updateLesson, deleteLesson,
  createMaterial, deleteMaterial, getVodUploadSign,
  type AdminCourse, type AdminChapter, type AdminLesson,
} from '@/api/admin'

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

async function uploadVideo(chapterId: number, lesson: AdminLesson) {
  // chapterId 和 lesson 预留，后续接入 VOD 上传时使用
  void chapterId; void lesson
  try {
    const sign = await getVodUploadSign()
    if (!sign.configured) {
      ElMessage.warning('腾讯云 VOD 尚未配置，请在 application.yml 中填写 VOD 相关密钥')
      return
    }
    // TODO: 接入腾讯云 VOD 客户端上传 SDK，上传后获取 fileId 并回写 lesson.vodFileId
    ElMessage.info('VOD 上传功能需接入腾讯云 SDK 后启用')
  } catch {
    ElMessage.warning('无法获取 VOD 上传签名')
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
            <span>{{ l.title }} <el-tag v-if="l.freeTrial" size="small" type="success">试看</el-tag></span>
            <span>
              <el-button link type="primary" @click="editLesson(l, c.id!)">编辑</el-button>
              <el-button link type="primary" @click="uploadVideo(c.id!, l)">上传视频</el-button>
              <el-button link type="danger" @click="removeLesson(l)">删除</el-button>
            </span>
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
  </div>
</template>

<style scoped>
.course-form { max-width: 640px; }
.section { margin-top: 32px; }
.section-title { font-size: 18px; margin-bottom: 16px; }
.chapter-form { display: flex; gap: 10px; margin-bottom: 16px; max-width: 480px; }
.chapter-card {
  background: #fff; border: 1px solid var(--line); border-radius: var(--r-md);
  padding: 16px; margin-bottom: 12px;
}
.chapter-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.lessons { padding-left: 16px; }
.lesson-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid var(--line);
}
.lesson-form { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
.lesson-form .el-input { flex: 1; }
.section-tip { font-size: 12px; color: var(--text-3); margin: 0 0 12px; }
.material-form { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
@media (max-width: 768px) {
  .course-form { max-width: 100%; }
  .chapter-form { flex-direction: column; max-width: 100%; }
  .lesson-form { flex-wrap: wrap; }
  .lesson-row { flex-direction: column; align-items: flex-start; gap: 6px; }
  .material-form { flex-direction: column; align-items: stretch; }
}
</style>
