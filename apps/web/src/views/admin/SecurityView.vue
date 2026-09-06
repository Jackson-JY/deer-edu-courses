<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSecurityConfig } from '@/api/admin'

const watermarkTemplate = ref('')
const credentialTtl = ref(300)
const previewTtl = ref(300)

async function load() {
  try {
    const config = await getSecurityConfig()
    watermarkTemplate.value = config.watermarkTemplate
    credentialTtl.value = config.credentialTtl
    previewTtl.value = config.previewTtl
  } catch {
    // 忽略，使用默认值
  }
}

function save() {
  // TODO: 接入保存接口
  ElMessage.success('安全策略已保存')
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="page-title">安全策略</h1>
    <el-form label-width="120px" class="form">
      <el-form-item label="水印模板">
        <el-input v-model="watermarkTemplate" style="width: 360px" />
        <div class="hint">支持占位符：{phoneTail} {uid} {time}</div>
      </el-form-item>
      <el-form-item label="播放凭证 TTL(秒)">
        <el-input-number v-model="credentialTtl" :min="60" :step="60" />
      </el-form-item>
      <el-form-item label="课件预览 TTL(秒)">
        <el-input-number v-model="previewTtl" :min="60" :step="60" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.form { max-width: 560px; }
.hint { font-size: 12px; color: var(--text-3); margin-top: 4px; }
</style>
