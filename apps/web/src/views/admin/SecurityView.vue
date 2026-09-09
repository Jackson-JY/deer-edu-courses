<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSecurityConfig, saveSecurityConfig } from '@/api/admin'

const watermarkTemplate = ref('')
const credentialTtl = ref(300)
const previewTtl = ref(300)
const saving = ref(false)

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

async function save() {
  if (!watermarkTemplate.value.trim()) {
    ElMessage.warning('水印模板不能为空')
    return
  }
  saving.value = true
  try {
    await saveSecurityConfig({
      watermarkTemplate: watermarkTemplate.value.trim(),
      credentialTtl: credentialTtl.value,
      previewTtl: previewTtl.value,
    })
    ElMessage.success('安全策略已保存，立即生效')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 class="page-title">安全策略</h1>
    <el-form label-width="120px" class="form">
      <el-form-item label="水印模板">
        <el-input v-model="watermarkTemplate" style="width: 480px" />
        <div class="hint">
          支持占位符：
          <code>{nickname}</code> 昵称 ·
          <code>{phone}</code> 脱敏手机号 ·
          <code>{phoneTail}</code> 手机尾号 ·
          <code>{uid}</code> 用户ID ·
          <code>{time}</code> 时间 ·
          <code>{lesson}</code> 课时名
        </div>
      </el-form-item>
      <el-form-item label="播放凭证 TTL(秒)">
        <el-input-number v-model="credentialTtl" :min="60" :step="60" />
      </el-form-item>
      <el-form-item label="课件预览 TTL(秒)">
        <el-input-number v-model="previewTtl" :min="60" :step="60" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 20px; letter-spacing: -0.01em; }
.form { max-width: 640px; }
.hint { font-size: 12px; color: var(--muted-foreground); margin-top: 6px; line-height: 1.8; }
.hint code {
  background: var(--accent);
  padding: 1px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: var(--primary);
}
</style>
