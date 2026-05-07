<template>
  <div class="chat-window">
    <div class="chat-header">
      <div class="chat-title">AI 智能查询</div>
      <div class="chat-actions">
        <el-button size="small" @click="clearMessages">清空对话</el-button>
        <el-button size="small" type="primary" @click="newSession">新会话</el-button>
      </div>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <message-bubble
        v-for="(message, index) in messages"
        :key="index"
        :is-user="message.isUser"
        :content="message.content"
        :type="message.type"
        :timestamp="message.timestamp"
        :is-typing="message.isTyping"
      >
        <template v-if="message.type === 'chart'" #chart>
          <div class="chart-container">
            <!-- 图表内容 -->
            <slot name="chart" :message="message"></slot>
          </div>
        </template>
        <template v-if="message.type === 'table'" #table>
          <div class="table-container">
            <!-- 表格内容 -->
            <slot name="table" :message="message"></slot>
          </div>
        </template>
      </message-bubble>
    </div>
    
    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="输入您的问题，例如：近7天黄车收入趋势"
        @keyup.enter.ctrl="sendMessage"
      />
      <div class="input-actions">
        <div class="quick-questions">
          <el-button 
            v-for="question in quickQuestions" 
            :key="question"
            size="small"
            @click="useQuickQuestion(question)"
          >
            {{ question }}
          </el-button>
        </div>
        <div class="send-actions">
          <el-button type="primary" @click="sendMessage">发送 (Ctrl+Enter)</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import MessageBubble from './MessageBubble.vue'

const props = defineProps<{
  messages: Array<{
    isUser: boolean
    content: string
    type?: 'text' | 'chart' | 'table'
    timestamp: number
    isTyping?: boolean
    data?: any
  }>
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'send', message: string): void
  (e: 'clear'): void
  (e: 'newSession'): void
}>()

const inputMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

const quickQuestions = [
  '近7天订单趋势',
  '收入最高的区域',
  '各支付方式占比',
  '黄车 vs 绿车收入对比',
  '平均车费趋势'
]

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    emit('send', inputMessage.value.trim())
    inputMessage.value = ''
  }
}

const clearMessages = () => {
  emit('clear')
}

const newSession = () => {
  emit('newSession')
}

const useQuickQuestion = (question: string) => {
  inputMessage.value = question
  sendMessage()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
}

.chat-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #fafafa;
}

.chat-input {
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
  background-color: #fff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-questions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.send-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .input-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .quick-questions {
    justify-content: center;
  }
  
  .send-actions {
    justify-content: center;
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>