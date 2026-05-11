<template>
  <div class="ai-chat-container">
    <div class="chat-header">
      <h2>AI 智能分析助手</h2>
      <el-select v-model="selectedDb" placeholder="选择数据库" size="default">
        <el-option label="出租车数据" value="taxi" />
      </el-select>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">U</span>
          <span v-else>AI</span>
        </div>
        <div class="message-content">
          <div class="message-text" v-if="msg.role === 'user'">{{ msg.content }}</div>
          <div class="message-text ai-response" v-else>
            <div v-if="msg.sql" class="sql-block">
              <div class="sql-label">Generated SQL:</div>
              <pre>{{ msg.sql }}</pre>
              <el-button size="small" type="primary" @click="executeQuery(msg.sql)">执行查询</el-button>
            </div>
            <div v-if="msg.explanation">{{ msg.explanation }}</div>
            <div v-if="msg.chartConfig" class="chart-preview">
              <el-tag>推荐图表: {{ msg.chartConfig.chartType }}</el-tag>
            </div>
          </div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>

      <div v-if="loading" class="message assistant">
        <div class="message-avatar">
          <span>AI</span>
        </div>
        <div class="message-content">
          <div class="message-text loading">
            <span class="loading-spinner"></span>
            AI 正在分析中...
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputQuery"
        type="textarea"
        :rows="2"
        placeholder="输入您的自然语言查询，例如：今天各机场的订单量排行"
        @keyup.enter.ctrl="sendQuery"
      />
      <div class="input-actions">
        <el-button type="primary" :disabled="!inputQuery || loading" @click="sendQuery">
          发送 (Ctrl+Enter)
        </el-button>
        <el-button @click="clearChat">清空对话</el-button>
      </div>
    </div>

    <el-dialog v-model="showResultDialog" title="查询结果" width="80%">
      <div v-if="queryResult">
        <el-tabs v-model="resultTab">
          <el-tab-pane label="数据" name="data">
            <el-table :data="queryResult.data" stripe border max-height="400">
              <el-table-column
                v-for="col in queryResult.columns"
                :key="col"
                :prop="col"
                :label="col"
              />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="图表" name="chart">
            <div ref="chartContainer" class="chart-container"></div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

const inputQuery = ref('')
const selectedDb = ref('taxi')
const messages = ref([])
const loading = ref(false)
const messagesContainer = ref(null)
const showResultDialog = ref(false)
const resultTab = ref('data')
const chartContainer = ref(null)
const queryResult = reactive({
  sql: '',
  data: [],
  columns: []
})
const currentSessionId = ref('')

const sendQuery = async () => {
  if (!inputQuery.value.trim()) return

  const userMessage = {
    role: 'user',
    content: inputQuery.value,
    timestamp: Date.now()
  }
  messages.value.push(userMessage)
  const queryText = inputQuery.value
  inputQuery.value = ''
  loading.value = true

  scrollToBottom()

  try {
    const response = await axios.post('/api/ai/chat', {
      query: queryText,
      sessionId: currentSessionId.value,
      database: selectedDb.value
    })

    const aiMessage = {
      role: 'assistant',
      content: response.data.data.explanation || '处理完成',
      sql: response.data.data.sql,
      chartConfig: response.data.data.chartConfig,
      timestamp: Date.now()
    }

    if (response.data.data.sessionId) {
      currentSessionId.value = response.data.data.sessionId
    }

    messages.value.push(aiMessage)
  } catch (error) {
    ElMessage.error('查询失败: ' + (error.message || '未知错误'))
    messages.value.push({
      role: 'assistant',
      content: '抱歉，处理您的请求时出现错误。',
      timestamp: Date.now()
    })
  } finally {
    loading.value = false
    nextTick(() => scrollToBottom())
  }
}

const executeQuery = async (sql) => {
  try {
    ElMessage.info('执行查询...')
    showResultDialog.value = true

    await nextTick()
  } catch (error) {
    ElMessage.error('执行失败')
  }
}

const clearChat = () => {
  messages.value = []
  currentSessionId.value = ''
  ElMessage.success('对话已清空')
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}

const renderChart = (chartConfig, data) => {
  if (!chartContainer.value) return

  const chart = echarts.init(chartContainer.value)

  const option = {
    title: {
      text: chartConfig.title || '数据可视化'
    },
    tooltip: {},
    xAxis: {
      type: 'category',
      data: data.map((item, index) => index)
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      type: chartConfig.chartType || 'bar',
      data: data.map(item => Object.values(item)[0])
    }]
  }

  chart.setOption(option)
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  color: #fff;
  flex-shrink: 0;
}

.message.assistant .message-avatar {
  background: #67c23a;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
}

.message.user .message-text {
  background: #409eff;
  color: #fff;
}

.message.assistant .message-text {
  background: #fff;
  color: #303133;
}

.message-text.loading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e5e7eb;
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.sql-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 12px;
}

.sql-label {
  color: #7aa874;
  font-size: 12px;
  margin-bottom: 8px;
}

.sql-block pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  white-space: pre-wrap;
}

.chart-preview {
  margin-top: 8px;
}

.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.message.user .message-time {
  text-align: right;
}

.chat-input {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.chart-container {
  width: 100%;
  height: 400px;
}
</style>