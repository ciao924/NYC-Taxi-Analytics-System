<template>
  <div class="message-bubble" :class="{ 'user-message': isUser, 'ai-message': !isUser }">
    <div class="message-avatar">
      <el-avatar v-if="isUser" :size="32" :src="userAvatar" />
      <el-avatar v-else :size="32" :src="aiAvatar">AI</el-avatar>
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="message-sender">{{ isUser ? '我' : 'AI' }}</span>
        <span class="message-time">{{ formatTime(timestamp) }}</span>
      </div>
      <div class="message-body">
        <template v-if="isTyping">
          <div class="typing-indicator">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </template>
        <template v-else>
          <div v-if="type === 'text'" class="text-message">{{ content }}</div>
          <div v-else-if="type === 'chart'" class="chart-message">
            <!-- 图表内容将由父组件通过 slot 传入 -->
            <slot name="chart"></slot>
          </div>
          <div v-else-if="type === 'table'" class="table-message">
            <!-- 表格内容将由父组件通过 slot 传入 -->
            <slot name="table"></slot>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  isUser: boolean
  content?: string
  type?: 'text' | 'chart' | 'table'
  timestamp?: number
  isTyping?: boolean
  userAvatar?: string
  aiAvatar?: string
}>(), {
  type: 'text',
  timestamp: () => Date.now(),
  isTyping: false,
  userAvatar: 'https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50?f=y',
  aiAvatar: 'https://www.gravatar.com/avatar/12802915160929270304b8f335453210?f=y'
})

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.message-bubble {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  flex-direction: row-reverse;
}

.ai-message {
  flex-direction: row;
}

.message-avatar {
  margin: 0 12px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.user-message .message-content {
  background-color: #409EFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-content {
  background-color: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.message-sender {
  font-weight: 500;
}

.message-time {
  margin-left: 12px;
}

.message-body {
  line-height: 1.5;
}

.text-message {
  white-space: pre-wrap;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #909399;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chart-message,
.table-message {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }
}
</style>