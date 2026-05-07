<template>
  <div class="export-button-wrapper">
    <el-button
      :loading="isExporting"
      :disabled="disabled"
      @click="handleExport"
    >
      <el-icon><Download /></el-icon>
      导出
    </el-button>
    
    <ExportProgress
      v-model="progressVisible"
      :task="currentTask"
      @download="handleDownload"
      @cancel="handleCancel"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { useExport } from '@/composables/useExport'
import ExportProgress from './ExportProgress.vue'

const props = defineProps<{
  params: any
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'error', message: string): void
}>()

const { currentTask, isExporting, createExport, download, cancel, reset } = useExport()
const progressVisible = ref(false)

// 监听任务完成，显示进度弹窗
watch(currentTask, (newTask) => {
  if (newTask && (newTask.status === 'PENDING' || newTask.status === 'RUNNING')) {
    progressVisible.value = true
  }
  if (newTask?.status === 'SUCCESS') {
    emit('success')
  }
  if (newTask?.status === 'FAILED') {
    emit('error', newTask.errorMessage || '导出失败')
  }
})

const handleExport = async () => {
  const success = await createExport(props.params)
  if (!success) {
    emit('error', '创建导出任务失败')
  }
}

const handleDownload = () => {
  download()
  progressVisible.value = false
}

const handleCancel = () => {
  cancel()
  progressVisible.value = false
}

const handleRetry = () => {
  reset()
  handleExport()
}
</script>

<style scoped>
.export-button-wrapper {
  display: inline-block;
}
.export-button-wrapper .el-icon {
  margin-right: 4px;
}
</style>