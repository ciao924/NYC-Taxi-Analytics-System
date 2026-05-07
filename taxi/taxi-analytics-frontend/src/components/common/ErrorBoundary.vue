<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <el-result
        :icon="errorIcon"
        :title="errorTitle"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="handleRetry">重试</el-button>
          <el-button @click="handleReset">返回首页</el-button>
        </template>
      </el-result>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const hasError = ref(false)
const errorTitle = ref('页面出错了')
const errorMessage = ref('')
const errorIcon = ref<"success" | "warning" | "info" | "error">('error')

onErrorCaptured((err, _instance, info) => {
  console.error('ErrorBoundary caught:', err, info)
  hasError.value = true
  errorMessage.value = err.message || '发生未知错误'
  
  // 根据错误类型设置图标
  if (err.message?.includes('network') || err.message?.includes('Network')) {
    errorIcon.value = 'warning'
    errorTitle.value = '网络连接异常'
  }
  
  return false // 阻止错误继续传播
})

const handleRetry = () => {
  hasError.value = false
  window.location.reload()
}

const handleReset = () => {
  hasError.value = false
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
</style>