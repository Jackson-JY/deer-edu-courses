<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listAdminUsers, type AdminUser } from '@/api/admin'

const router = useRouter()
const users = ref<AdminUser[]>([])
const loading = ref(false)
const phone = ref('')

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
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEntitlements(row)">开通课程权限</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
@media (max-width: 768px) {
  .page-title { font-size: 18px; }
  .search-bar { flex-wrap: wrap; }
}
</style>
