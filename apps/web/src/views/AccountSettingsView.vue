<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const nickname = ref(auth.user?.nickname || '雅思学员')
const phone = ref(auth.user?.phone || '138****6021')

function rebindPhone() {
  ElMessage.info('换绑手机号需短信验证（demo）')
}
function deleteAccount() {
  ElMessageBox.confirm(
    '注销后账号数据将被清除且不可恢复，已购课程将无法继续观看。确定要注销吗？',
    '注销账号',
    { confirmButtonText: '确认注销', cancelButtonText: '再想想', type: 'warning' },
  )
    .then(() => ElMessage.success('已提交注销申请（demo）'))
    .catch(() => {})
}
</script>

<template>
  <div class="container settings">
    <h1 class="page-title">账号设置</h1>

    <div class="card">
      <div class="card-title">个人信息</div>
      <div class="row">
        <span class="label">头像</span>
        <div class="avatar">{{ nickname[0] }}</div>
      </div>
      <div class="row">
        <span class="label">昵称</span>
        <span class="value">{{ nickname }} <span class="from">来自微信</span></span>
      </div>
      <div class="row">
        <span class="label">手机号</span>
        <span class="value">{{ phone }}</span>
        <el-button text type="primary" @click="rebindPhone">换绑</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">账号与安全</div>
      <div class="row">
        <span class="label">登录方式</span>
        <span class="value"><el-icon color="#09bb07"><ChatRound /></el-icon> 微信已绑定</span>
      </div>
      <div class="row">
        <span class="label">单端在线</span>
        <span class="value">为保护账号安全，同一时间仅允许一台设备观看</span>
      </div>
    </div>

    <div class="card danger">
      <div class="card-title">危险操作</div>
      <div class="row">
        <span class="label">注销账号</span>
        <span class="value">永久删除账号及相关数据</span>
        <el-button type="danger" plain round @click="deleteAccount">注销账号</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings { padding: 28px 20px 48px; max-width: 760px; }
.page-title { font-size: 24px; margin-bottom: 18px; }
.card {
  background: var(--bg); border: 1px solid var(--line); border-radius: var(--r-lg);
  box-shadow: var(--shadow-sm); padding: 18px 22px; margin-bottom: 18px;
}
.card-title { font-weight: 600; margin-bottom: 8px; }
.row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0; border-bottom: 1px solid var(--line);
}
.row:last-child { border-bottom: none; }
.label { width: 88px; flex-shrink: 0; color: var(--text-3); font-size: 14px; }
.value { flex: 1; font-size: 14px; display: flex; align-items: center; gap: 8px; }
.value .el-icon { vertical-align: -2px; }
.from { font-size: 12px; color: var(--text-3); }
.avatar {
  width: 46px; height: 46px; border-radius: 50%; background: var(--brand-500);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 600;
}
.card.danger { border-color: rgba(224, 83, 61, 0.3); }
.card.danger .card-title { color: var(--danger); }
</style>
