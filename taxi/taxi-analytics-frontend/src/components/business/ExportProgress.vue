<template>
  <el-dialog v-model="visible" title="导出进度" width="400px" :close-on-click-modal="false">
    <div class="export-progress">
      <div class="progress-info">
        <span class="status-icon" :class="{ 'is-loading': task?.status === 'RUNNING' }">{{ statusIconText }}</span>
        <span class="status-text">{{ statusText }}</span>
      </div>
      
      <el-progress
        :percentage="task?.progress || 0"
        :status="progressStatus"
        :stroke-width="8"
      />
      
      <div v-if="task?.totalRows" class="row-info">
        已处理 {{ (task.progress / 100 * task.totalRows).toFixed(0) }} / {{ task.totalRows }} 条
      </div>
      
      <div v-if="task?.errorMessage" class="error-message">
        {{ task.errorMessage }}
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button v-if="task?.status === 'RUNNING'" @click="handleCancel">取消</el-button>
        <el-button v-if="task?.status === 'SUCCESS'" type="primary" @click="handleDownload">下载</el-button>
        <el-button v-if="task?.status === 'FAILED'" @click="handleRetry">重试</el-button>
        <el-button @click="handleClose">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExportTask } from '@/composables/useExport'

const props = defineProps<{
  modelValue: boolean
  task: ExportTask | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'download'): void
  (e: 'cancel'): void
  (e: 'retry'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const statusIconText = computed(() => {
  switch (props.task?.status) {
    case 'PENDING':
    case 'RUNNING':
      return '○'
    case 'SUCCESS':
      return '✓'
    case 'FAILED':
    case 'CANCELLED':
      return '✗'
    default:
      return '📄'
  }
})

const statusText = computed(() => {
  switch (props.task?.status) {
    case 'PENDING': return '等待中...'
    case 'RUNNING': return '导出中...'
    case 'SUCCESS': return '导出完成'
    case 'FAILED': return '导出失败'
    case 'CANCELLED': return '已取消'
    case 'EXPIRED': return '已过期'
    default: return '未知状态'
  }
})

const progressStatus = computed(() => {
  if (props.task?.status === 'SUCCESS') return 'success'
  if (props.task?.status === 'FAILED') return 'exception'
  return undefined
})

const handleDownload = () => emit('download')
const handleCancel = () => emit('cancel')
const handleRetry = () => emit('retry')
const handleClose = () => {
  visible.value = false
  emit('cancel')
}
</script>

<style scoped>
.export-progress {
  padding: 20px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-info .el-icon {
  font-size: 24px;
}

.progress-info .is-loading {
  animation: rotating 2s linear infinite;
}

.status-text {
  font-size: 16px;
  font-weight: 500;
}

.row-info {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.error-message {
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  font-size: 12px;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>