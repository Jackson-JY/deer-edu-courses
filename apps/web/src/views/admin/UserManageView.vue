<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminUsers, assignUserRole, type AdminUser } from '@/api/admin'
import { grantWriting, revokeWriting } from '@/api/writing'

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

// ===== 写作权限弹窗 =====
const grantDialog = ref(false)
const grantUser = ref<AdminUser | null>(null)
const grantMode = ref<'permanent' | 'custom'>('permanent')
const grantDate = ref('')
const grantSaving = ref(false)

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

function openGrantDialog(user: any) {
  grantUser.value = user
  grantMode.value = user.writingGranted && !user.writingExpireAt ? 'permanent' : 'permanent'
  grantDate.value = ''
  grantDialog.value = true
}

async function saveGrant() {
  if (!grantUser.value) return
  let expireAt: string | null = null
  if (grantMode.value === 'custom') {
    if (!grantDate.value) {
      ElMessage.warning('请选择有效期截止日期')
      return
    }
    expireAt = `${grantDate.value} 23:59:59`
  }
  grantSaving.value = true
  try {
    await grantWriting(grantUser.value.id, expireAt)
    ElMessage.success('写作批改权限已开通')
    grantDialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '开通失败')
  } finally {
    grantSaving.value = false
  }
}

async function revoke(user: any) {
  try {
    await ElMessageBox.confirm(
      `确认撤销「${user.nickname || user.phone}」的写作批改权限？撤销后该学生将无法提交新作文。`,
      '撤销写作权限',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await revokeWriting(user.id)
    ElMessage.success('已撤销')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function writingLabel(user: any): string {
  if (!user.writingGranted) return '未开通'
  return user.writingExpireAt ? `有效期至 ${user.writingExpireAt.substring(0, 10)}` : '永久有效'
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
      <el-button plain @click="router.push('/admin/password-resets')">密码重置申请</el-button>
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
      <el-table-column label="写作批改" width="170">
        <template #default="{ row }">
          <el-tag :type="row.writingGranted ? 'success' : 'info'" size="small">
            {{ writingLabel(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="330">
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
          <el-button v-if="!row.writingGranted" link type="primary" @click="openGrantDialog(row)">开通写作</el-button>
          <el-button v-else link type="danger" @click="revoke(row)">撤销写作</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 写作权限开通弹窗 -->
    <el-dialog v-model="grantDialog" title="开通写作批改权限" width="420px">
      <div v-if="grantUser" class="grant-body">
        <p class="target">
          用户：<b>{{ grantUser.nickname || grantUser.phone }}</b>
          <span class="muted">（{{ grantUser.phone }}）</span>
        </p>
        <p class="muted note">开通后学生可提交作文进行批改，每月限 3 篇。</p>
        <el-radio-group v-model="grantMode">
          <el-radio value="permanent">永久有效</el-radio>
          <el-radio value="custom">设置截止日期</el-radio>
        </el-radio-group>
        <el-date-picker
          v-if="grantMode === 'custom'"
          v-model="grantDate"
          type="date"
          placeholder="选择截止日期"
          value-format="YYYY-MM-DD"
          style="width: 100%; margin-top: 12px"
        />
      </div>
      <template #footer>
        <el-button @click="grantDialog = false">取消</el-button>
        <el-button type="primary" :loading="grantSaving" @click="saveGrant">确认开通</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 20px; letter-spacing: -0.01em; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.grant-body { display: flex; flex-direction: column; gap: 12px; }
.grant-body .target { margin: 0; font-size: 14px; }
.grant-body .note { margin: 0; font-size: 12px; }
.muted { color: var(--muted-foreground); }
@media (max-width: 768px) {
  .page-title { font-size: 18px; }
  .search-bar { flex-wrap: wrap; }
}
</style>
