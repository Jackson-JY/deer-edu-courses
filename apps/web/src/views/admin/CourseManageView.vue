<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminCourses, deleteAdminCourse, publishCourse, offlineCourse, type AdminCourse } from '@/api/admin'

const router = useRouter()
const courses = ref<AdminCourse[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    courses.value = await listAdminCourses()
  } finally {
    loading.value = false
  }
}

function goEdit(c: any) {
  router.push(`/admin/courses/${c.id}`)
}

function goCreate() {
  router.push('/admin/courses/create')
}

async function remove(c: any) {
  await ElMessageBox.confirm(`确定删除课程《${c.title}》？`, '提示', { type: 'warning' })
  await deleteAdminCourse(c.id)
  ElMessage.success('已删除')
  load()
}

async function toggleStatus(c: any) {
  if (c.status === 'ONLINE') {
    await offlineCourse(c.id)
  } else {
    await publishCourse(c.id)
  }
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">课程管理</h1>
      <el-button type="primary" @click="goCreate">新建课程</el-button>
    </div>
    <el-table v-loading="loading" :data="courses" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="课程标题" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price / 100 }}</template>
      </el-table-column>
      <el-table-column prop="targetScore" label="目标分" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 'ONLINE' ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ONLINE' ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; margin: 0; letter-spacing: -0.01em; }
@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .page-title { font-size: 18px; }
}
</style>
