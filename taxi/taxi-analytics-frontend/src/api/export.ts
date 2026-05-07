import request from './request'

// 导出模块 API
export const exportApi = {
  /**
   * 创建异步导出任务
   */
  createTask: (data: any) => {
    return request.post('/export/task', data)
  },

  /**
   * 查询导出任务状态
   * @param taskId 任务ID
   */
  getTaskStatus: (taskId: string) => {
    return request.get(`/export/task/${taskId}`)
  },

  /**
   * 取消导出任务
   * @param taskId 任务ID
   */
  cancelTask: (taskId: string) => {
    return request.request({
      url: `/export/task/${taskId}`,
      method: 'delete'
    })
  },

  /**
   * 下载导出文件 (直接打开或处理Blob)
   * 也可以直接使用 window.open(`/api/export/download/${taskId}`)
   * @param taskId 任务ID
   */
  downloadFileUrl: (taskId: string) => {
    return `${import.meta.env.VITE_API_BASE_URL || '/api'}/export/download/${taskId}`
  }
}