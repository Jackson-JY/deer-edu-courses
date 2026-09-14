<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { grantEntitlement, listAdminCourses, listUserEntitlements, type AdminCourse, type UserEntitlement } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const userId = Number(route.params.id)
const courses = ref<AdminCourse[]>([])
const selectedCourse = ref<number | null>(null)
const expireAt = ref('')
const loading = ref(false)
const entitlements = ref<UserEntitlement[]>([])

async function load() {
  const [courseList, ents] = await Promise.all([listAdminCourses(), listUserEntitlements(userId)])
  courses.value = courseList
  entitlements.value = ents
}

async function submit() {
  if (!selectedCourse.value) {
    ElMessage.warning('请选择课程')
    return
  }
  loading.value = true
  try {
    await grantEntitlement(userId, {
      courseId: selectedCourse.value,
      source: 'MANUAL',
      expireAt: expireAt.value || undefined,
    })
    ElMessage.success('开通成功')
    await load()
    selectedCourse.value = null
    expireAt.value = ''
  } finally {
    loading.value = false
  }
}

function fmtExpire(e: UserEntitlement) {
  if (!e.expireAt) return '永久有效'
  return e.expireAt.replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="page-title">开通课程权限</h1>
    <p class="tip">给用户 ID: {{ userId }} 开通指定课程的学习权限</p>

    <section class="section">
      <h2 class="section-title">已开通课程</h2>
      <el-table :data="entitlements" stripe style="width: 100%" empty-text="暂无已开通课程">
        <el-table-column prop="title" label="课程" min-width="200" />
        <el-table-column label="到期时间" width="200">
          <template #default="{ row }">{{ fmtExpire(row as UserEntitlement) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120" />
      </el-table>
    </section>

    <section class="section">
      <h2 class="section-title">新增开通</h2>
      <el-form label-width="80px" class="form">
        <el-form-item label="选择课程">
          <el-select v-model="selectedCourse" placeholder="请选择课程" style="width: 320px">
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.title"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="expireAt"
            type="datetime"
            placeholder="留空表示永久有效"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">确认开通</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; letter-spacing: -0.01em; }
.tip { color: var(--muted-foreground); font-size: 13px; margin-bottom: 20px; }
.section { margin-bottom: 28px; }
.section-title { font-size: 16px; font-weight: 600; margin: 0 0 12px; }
.form { max-width: 480px; }
@media (max-width: 768px) {
  .form { max-width: 100%; }
  .page-title { font-size: 18px; }
}
</style>
