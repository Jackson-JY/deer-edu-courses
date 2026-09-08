<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminUsers, assignUserRole, type AdminUser } from '@/api/admin'

const router = useRouter()
const users = ref<AdminUser[]>([])
const loading = ref(false)
const phone = ref('')

type RoleValue = 'STUDENT' | 'TEACHER' | 'ADMIN'
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const ROLE_OPTIONS: { value: RoleValue; label: string; tag: TagType }[] = [
  { value: 'STUDENT', label: '学生', tag: 'info' },
  { value: 'TEACHER', label: '老师', tag: 'warning' },
  { value: 'ADMIN', label: '管理员', tag: 'danger' },
]

async function load() {
  loading.value = true
  try {
    users.value = await listAdminUsers(phone.value || undefined)
  } finally {
    loading.value = false
  }
}

function goEntitlements(user: any) {
  router.push(`/admin/users/${user.id}/entitlements`)
}

function primaryRole(roles: string[]): RoleValue {
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('TEACHER')) return 'TEACHER'
  return 'STUDENT'
}

function roleLabel(role: string): string {
  return ROLE_OPTIONS.find((r) => r.value === role)?.label || role
}

function roleTag(role: string): TagType {
  return ROLE_OPTIONS.find((r) => r.value === role)?.tag || 'info'
}

async function changeRole(user: any, newRole: string) {
  const cur = primaryRole(user.roles || [])
  if (newRole === cur) return
  try {
    await ElMessageBox.confirm(
      `确认将「${user.nickname || user.phone}」的角色改为「${roleLabel(newRole)}」？`,
      '角色变更',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await assignUserRole(user.id, newRole as RoleValue)
    user.roles = [newRole]
    ElMessage.success('角色已更新')
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
    await load()
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="page-title">用户管理</h1>
    <div class="search-bar">
      <el-input v-model="phone" placeholder="输入手机号搜索" maxlength="11" style="width: 220px" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="users" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-tag :type="roleTag(primaryRole(row.roles))" effect="dark" size="small">
            {{ roleLabel(primaryRole(row.roles)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-select
            :model-value="primaryRole(row.roles)"
            size="small"
            style="width: 110px"
            @change="(v: any) => changeRole(row, v)"
          >
            <el-option v-for="r in ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
          <el-button link type="primary" @click="goEntitlements(row)">开通课程</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 20px; letter-spacing: -0.01em; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
@media (max-width: 768px) {
  .page-title { font-size: 18px; }
  .search-bar { flex-wrap: wrap; }
}
</style>
