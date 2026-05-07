import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportApi } from '@/api/export'

export interface ExportParams {
  taskType: string
  params?: any
}

export interface ExportTask {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'EXPIRED'
  progress: number
  fileName?: string
  errorMessage?: string
  totalRows?: number
}

export function useExport() {
  const currentTask = ref<ExportTask | null>(null)
  const pollingTimer = ref<number | null>(null)
  const isExporting = ref(false)
  
  // 创建导出任务
  const createExport = async (params: ExportParams): Promise<boolean> => {
    // 前置校验
    if (isExporting.value) {
      ElMessage.warning('已有导出任务进行中，请稍后')
      return false
    }
    
    try {
      isExporting.value = true
      const res = await exportApi.createTask(params)
      currentTask.value = {
        taskId: res.taskId,
        status: res.status,
        progress: 0
      }
      
      // 开始轮询任务状态
      startPolling()
      return true
    } catch (error: any) {
      ElMessage.error(error.message || '创建导出任务失败')
      isExporting.value = false
      return false
    }
  }
  
  // 轮询任务状态
  const startPolling = () => {
    if (pollingTimer.value) clearInterval(pollingTimer.value)
    
    pollingTimer.value = window.setInterval(async () => {
      if (!currentTask.value) return
      
      try {
        const res = await exportApi.getTaskStatus(currentTask.value.taskId)
        currentTask.value = { ...currentTask.value, ...res }
        
        // 任务完成或失败，停止轮询
        if (res.status === 'SUCCESS' || res.status === 'FAILED' || res.status === 'CANCELLED') {
          stopPolling()
          isExporting.value = false
          
          if (res.status === 'SUCCESS') {
            ElMessage.success('导出任务完成')
          } else if (res.status === 'FAILED') {
            ElMessage.error(res.errorMessage || '导出失败')
          }
        }
      } catch (error) {
        console.error('查询任务状态失败', error)
      }
    }, 2000) // 每2秒轮询一次
  }
  
  // 停止轮询
  const stopPolling = () => {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }
  
  // 下载文件
  const download = () => {
    if (!currentTask.value || currentTask.value.status !== 'SUCCESS') {
      ElMessage.warning('文件未就绪')
      return
    }
    
    try {
      const url = exportApi.downloadFileUrl(currentTask.value.taskId)
      window.open(url, '_blank')
    } catch (error: any) {
      ElMessage.error(error.message || '下载失败')
    }
  }
  
  // 取消任务
  const cancel = async () => {
    if (!currentTask.value) return
    
    try {
      await exportApi.cancelTask(currentTask.value.taskId)
      stopPolling()
      isExporting.value = false
      ElMessage.success('已取消导出任务')
    } catch (error: any) {
      ElMessage.error(error.message || '取消失败')
    }
  }
  
  // 重置状态
  const reset = () => {
    stopPolling()
    currentTask.value = null
    isExporting.value = false
  }
  
  return {
    currentTask,
    isExporting,
    createExport,
    download,
    cancel,
    reset
  }
}