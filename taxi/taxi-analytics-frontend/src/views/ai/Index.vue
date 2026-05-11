<template>
  <div class="ai-page">
    <div class="ai-layout">
      <!-- 侧边栏 -->
      <aside class="ai-sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">会话管理</h3>
          <el-button size="small" type="primary" @click="createNewSession">+ 新建会话</el-button>
        </div>

        <!-- 会话列表 -->
        <div class="session-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: currentSessionId === session.sessionId }"
            @click="selectSession(session.sessionId)"
          >
            <div class="session-info">
              <span class="session-name">{{ session.sessionName }}</span>
            </div>
            <div class="session-actions">
              <el-button size="mini" @click.stop="showRenameDialog(session)" title="重命名">编辑</el-button>
              <el-button size="mini" type="danger" @click.stop="deleteSession(session.sessionId)" title="删除">删除</el-button>
            </div>
          </div>
          <div v-if="sessions.length === 0" class="empty-state">
            <p>暂无会话记录</p>
            <el-button size="small" type="primary" @click="createNewSession">
              新建第一个会话
            </el-button>
          </div>
        </div>

        <!-- 我的收藏 -->
        <div class="sidebar-section">
          <div class="section-header">
            <h4 class="section-title">我的收藏</h4>
          </div>
          <div class="favorite-list">
            <div
              v-for="fav in favorites"
              :key="fav.id"
              class="favorite-item"
              @click="applyFavorite(fav.queryText)"
            >
              <span class="favorite-text">{{ fav.queryText }}</span>
              <el-button size="mini" type="link" @click.stop="removeFavorite(fav.id)" title="取消收藏">×</el-button>
            </div>
            <div v-if="favorites.length === 0" class="empty-state small">
              <p>暂无收藏</p>
            </div>
          </div>
        </div>

        <!-- 定时任务 -->
        <div class="sidebar-section">
          <div class="section-header">
            <h4 class="section-title">定时任务</h4>
            <el-button size="mini" type="primary" @click="showScheduleDialog = true" title="创建任务">+</el-button>
          </div>
          <div class="schedule-list">
            <div
              v-for="task in scheduledTasks"
              :key="task.taskId"
              class="schedule-item"
            >
              <div class="schedule-info">
                <span class="schedule-query">{{ task.queryText }}</span>
                <span class="schedule-time">{{ task.scheduleTime }}</span>
              </div>
              <el-button size="mini" type="danger" @click="deleteScheduledTask(task.taskId)" title="删除任务">×</el-button>
            </div>
            <div v-if="scheduledTasks.length === 0" class="empty-state small">
              <p>暂无定时任务</p>
            </div>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="ai-main">
        <!-- 聊天头部 -->
        <header class="chat-header">
          <div class="chat-title">
            <h3>{{ currentSessionName || '智能数据查询助手' }}</h3>
          </div>
          <div class="chat-actions">
            <el-button size="small" @click="toggleSuggestions">查询建议</el-button>
            <el-button size="small" @click="toggleHistory">查询历史</el-button>
          </div>
        </header>

        <!-- 查询建议面板 -->
        <div class="suggestions-panel" v-if="showSuggestions">
          <div class="panel-header">
            <h4>查询建议</h4>
            <el-button size="mini" type="link" @click="showSuggestions = false">×</el-button>
          </div>
          <div class="suggestions-list">
            <div
              v-for="suggestion in suggestions"
              :key="suggestion.id"
              class="suggestion-item"
              @click="applySuggestion(suggestion.text)"
            >
              <span>{{ suggestion.text }}</span>
            </div>
            <div v-if="suggestions.length === 0" class="empty-state small">
              <p>暂无查询建议</p>
            </div>
          </div>
        </div>

        <!-- 查询历史面板 -->
        <div class="history-panel" v-if="showHistory">
          <div class="panel-header">
            <h4>查询历史</h4>
            <el-button size="mini" type="link" @click="showHistory = false">×</el-button>
          </div>
          <div class="history-list">
            <div
              v-for="item in history"
              :key="item.messageId"
              class="history-item"
              @click="replayQuery(item.question)"
            >
              <div class="history-question">{{ item.question }}</div>
              <div class="history-time">{{ item.createTime }}</div>
            </div>
            <div v-if="history.length === 0" class="empty-state small">
              <p>暂无查询历史</p>
            </div>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">AI</div>
            <h2>欢迎使用智能数据查询助手</h2>
            <p>您可以通过自然语言查询出租车数据，例如：</p>
            <div class="welcome-examples">
              <el-button size="small" @click="applyExample('查询2025年1月的订单总量')">
                查询2025年1月的订单总量
              </el-button>
              <el-button size="small" @click="applyExample('统计总收入最高的10个区域')">
                统计总收入最高的10个区域
              </el-button>
              <el-button size="small" @click="applyExample('分析周末与工作日的出行差异')">
                分析周末与工作日的出行差异
              </el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div
            v-for="msg in messages"
            :key="msg.messageId"
            class="message-item"
            :class="msg.role"
          >
            <div class="message-avatar">
              {{ msg.role === 'user' ? 'U' : 'AI' }}
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
                <div v-else class="message-text">
                  <div v-if="msg.chartType && msg.chartData" class="chart-container">
                    <v-chart :option="getChartOption(msg)" autoresize />
                  </div>
                  <div v-else>{{ msg.content }}</div>
                </div>
              </div>
              <div class="message-actions" v-if="msg.role === 'assistant'">
                <el-button size="mini" @click="copyMessage(msg.content)" title="复制">复制</el-button>
                <el-button size="mini" @click="addToFavorites(msg.content)" title="收藏">收藏</el-button>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="loading" class="message-item assistant">
            <div class="message-avatar">AI</div>
            <div class="message-content">
              <div class="message-bubble loading">
                <div class="loading-spinner"></div>
                <span>正在分析您的问题...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <div class="input-wrapper">
            <textarea
              v-model="queryText"
              placeholder="请输入您的问题，按 Enter 发送，Shift + Enter 换行"
              rows="2"
              @keydown.enter.exact.prevent="sendQuery"
              @keydown.enter.shift.exact="(e) => { if (e.target) (e.target as HTMLTextAreaElement).value += '\n' }"
            ></textarea>
          </div>
          <div class="input-actions">
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!queryText.trim()"
              @click="sendQuery"
            >
              发送
            </el-button>
          </div>
        </div>
      </main>
    </div>

    <!-- 重命名会话对话框 -->
    <el-dialog title="重命名会话" v-model="renameDialog.visible" width="400px">
      <el-input
        v-model="renameDialog.newName"
        placeholder="请输入新名称"
        @keyup.enter="confirmRename"
      />
      <template #footer>
        <el-button @click="renameDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建定时任务对话框 -->
    <el-dialog title="创建定时任务" v-model="showScheduleDialog" width="400px">
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="查询内容">
          <el-textarea
            v-model="scheduleForm.queryText"
            placeholder="请输入查询内容"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="执行时间">
          <el-input v-model="scheduleForm.scheduleTime" type="datetime-local" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showScheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSchedule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// 状态定义
const queryText = ref('')
const loading = ref(false)
const messages = ref<any[]>([])
const sessions = ref<any[]>([])
const currentSessionId = ref<string>('')
const currentSessionName = ref('')
const suggestions = ref<any[]>([])
const history = ref<any[]>([])
const favorites = ref<any[]>([])
const scheduledTasks = ref<any[]>([])
const showSuggestions = ref(false)
const showHistory = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话框状态
const renameDialog = ref({
  visible: false,
  sessionId: '',
  newName: ''
})
const showScheduleDialog = ref(false)
const scheduleForm = ref({
  queryText: '',
  scheduleTime: ''
})

// 模拟数据
const mockSessions = [
  { sessionId: '1', sessionName: '会话 1' },
  { sessionId: '2', sessionName: '会话 2' }
]

const mockSuggestions = [
  { id: '1', text: '查询2025年1月的订单总量' },
  { id: '2', text: '统计总收入最高的10个区域' },
  { id: '3', text: '分析周末与工作日的出行差异' },
  { id: '4', text: '查看最近7天的订单趋势' }
]

const mockHistory = [
  { messageId: '1', question: '查询2025年1月的订单总量', createTime: '2025-03-31 10:00:00' },
  { messageId: '2', question: '统计总收入最高的10个区域', createTime: '2025-03-30 15:30:00' }
]

const mockFavorites = [
  { id: '1', queryText: '查询2025年1月的订单总量' },
  { id: '2', queryText: '统计总收入最高的10个区域' }
]

const mockScheduledTasks = [
  { taskId: '1', queryText: '每日订单统计', scheduleTime: '2025-04-01 08:00:00' }
]

// 图表配置
const getChartOption = (msg: any) => {
  const chartType = msg.chartType || 'line'
  const chartData = msg.chartData || []
  
  if (chartType === 'pie') {
    return {
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', data: chartData.map((item: any) => ({ name: item.name, value: item.value })) }]
    }
  }
  
  if (chartType === 'bar') {
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: chartData.map((item: any) => item.name) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: chartData.map((item: any) => item.value) }]
    }
  }
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: chartData.map((item: any) => item.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: chartData.map((item: any) => item.value) }]
  }
}

// 方法
const loadSessions = async () => {
  setTimeout(() => {
    sessions.value = mockSessions
    if (sessions.value.length > 0) {
      selectSession(sessions.value[0].sessionId)
    }
  }, 300)
}

const loadSuggestions = async () => {
  setTimeout(() => {
    suggestions.value = mockSuggestions
  }, 300)
}

const loadHistory = async () => {
  setTimeout(() => {
    history.value = mockHistory
  }, 300)
}

const loadFavorites = async () => {
  setTimeout(() => {
    favorites.value = mockFavorites
  }, 300)
}

const loadScheduledTasks = async () => {
  setTimeout(() => {
    scheduledTasks.value = mockScheduledTasks
  }, 300)
}

const createNewSession = async () => {
  try {
    const newSession = {
      sessionId: Date.now().toString(),
      sessionName: '新会话 ' + new Date().toLocaleString()
    }
    sessions.value.unshift(newSession)
    selectSession(newSession.sessionId)
    ElMessage.success('会话创建成功')
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const selectSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  const session = sessions.value.find(s => s.sessionId === sessionId)
  if (session) {
    currentSessionName.value = session.sessionName
  }
  messages.value = []
}

const showRenameDialog = (session: any) => {
  renameDialog.value = {
    visible: true,
    sessionId: session.sessionId,
    newName: session.sessionName
  }
}

const confirmRename = async () => {
  try {
    const session = sessions.value.find(s => s.sessionId === renameDialog.value.sessionId)
    if (session) {
      session.sessionName = renameDialog.value.newName
      if (session.sessionId === currentSessionId.value) {
        currentSessionName.value = renameDialog.value.newName
      }
      ElMessage.success('重命名成功')
    }
    renameDialog.value.visible = false
  } catch (error) {
    ElMessage.error('重命名失败')
  }
}

const deleteSession = async (sessionId: string) => {
  try {
    const index = sessions.value.findIndex(s => s.sessionId === sessionId)
    if (index > -1) {
      sessions.value.splice(index, 1)
      if (sessionId === currentSessionId.value) {
        currentSessionId.value = ''
        currentSessionName.value = ''
        messages.value = []
      }
      ElMessage.success('删除成功')
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const sendQuery = async () => {
  if (!queryText.value.trim() || loading.value) return

  const question = queryText.value.trim()
  queryText.value = ''

  // 添加用户消息
  messages.value.push({
    messageId: 'temp_' + Date.now(),
    role: 'user',
    content: question
  })
  scrollToBottom()

  loading.value = true
  try {
    // 调用后端的AI接口
    const response = await fetch('http://localhost:8080/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        query: question,
        sessionId: currentSessionId.value || 'default',
        database: 'default'
      })
    })

    if (!response.ok) {
      throw new Error(`API调用失败: ${response.status} ${response.statusText}`)
    }

    const data = await response.json()
    
    // 处理API响应
    const aiResponse = {
      content: data.data.explanation || '抱歉，未能获取到有效的响应',
      chartType: data.data.chartConfig?.chartType || null,
      chartData: data.data.chartConfig?.data || null
    }

    messages.value.push({
      messageId: 'temp_' + (Date.now() + 1),
      role: 'assistant',
      content: aiResponse.content,
      chartType: aiResponse.chartType,
      chartData: aiResponse.chartData
    })

    scrollToBottom()
  } catch (error: any) {
    messages.value.push({
      messageId: 'temp_' + (Date.now() + 1),
      role: 'assistant',
      content: '抱歉，查询失败：' + (error.message || '未知错误')
    })
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

const applySuggestion = (text: string) => {
  queryText.value = text
  showSuggestions.value = false
}

const applyFavorite = (text: string) => {
  queryText.value = text
}

const applyExample = (text: string) => {
  queryText.value = text
}

const replayQuery = (question: string) => {
  queryText.value = question
  showHistory.value = false
}

const copyMessage = (content: string) => {
  navigator.clipboard.writeText(content)
  ElMessage.success('已复制到剪贴板')
}

const addToFavorites = async (content: string) => {
  try {
    const newFavorite = {
      id: Date.now().toString(),
      queryText: content
    }
    favorites.value.push(newFavorite)
    ElMessage.success('已添加到收藏')
  } catch (error) {
    ElMessage.error('添加收藏失败')
  }
}

const removeFavorite = async (id: string) => {
  try {
    const index = favorites.value.findIndex(f => f.id === id)
    if (index > -1) {
      favorites.value.splice(index, 1)
      ElMessage.success('已取消收藏')
    }
  } catch (error) {
    ElMessage.error('取消收藏失败')
  }
}

const confirmSchedule = async () => {
  try {
    const newTask = {
      taskId: Date.now().toString(),
      queryText: scheduleForm.value.queryText,
      scheduleTime: scheduleForm.value.scheduleTime
    }
    scheduledTasks.value.push(newTask)
    showScheduleDialog.value = false
    scheduleForm.value = {
      queryText: '',
      scheduleTime: ''
    }
    ElMessage.success('定时任务创建成功')
  } catch (error) {
    ElMessage.error('创建定时任务失败')
  }
}

const deleteScheduledTask = async (taskId: string) => {
  try {
    const index = scheduledTasks.value.findIndex(t => t.taskId === taskId)
    if (index > -1) {
      scheduledTasks.value.splice(index, 1)
      ElMessage.success('定时任务已删除')
    }
  } catch (error) {
    ElMessage.error('删除定时任务失败')
  }
}

const toggleSuggestions = () => {
  showSuggestions.value = !showSuggestions.value
  if (showSuggestions.value) {
    showHistory.value = false
  }
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  if (showHistory.value) {
    showSuggestions.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 初始化
onMounted(async () => {
  await Promise.all([
    loadSessions(),
    loadSuggestions(),
    loadHistory(),
    loadFavorites(),
    loadScheduledTasks()
  ])
})
</script>

<style lang="scss" scoped>
.ai-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  position: relative;
}

.ai-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(16, 185, 129, 0.05) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.ai-layout {
  display: flex;
  height: 100vh;
  position: relative;
  z-index: 1;
}

/* 侧边栏 */
.ai-sidebar {
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
}

.session-item:hover {
  background: #f3f4f6;
  transform: translateX(4px);
}

.session-item.active {
  background: #dbeafe;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(59, 130, 246, 0.2);
}

.session-info {
  flex: 1;
}

.session-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.sidebar-section {
  border-top: 1px solid #e5e7eb;
  padding: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  margin: 0;
  flex: 1;
}

.favorite-list,
.schedule-list {
  max-height: 180px;
  overflow-y: auto;
}

.favorite-item,
.schedule-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 6px;
}

.favorite-item:hover,
.schedule-item:hover {
  background: #f3f4f6;
}

.favorite-text,
.schedule-query {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #374151;
}

.schedule-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

/* 主内容区 */
.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
}

.chat-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.chat-actions {
  display: flex;
  gap: 10px;
}

.suggestions-panel,
.history-panel {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  max-height: 220px;
  overflow-y: auto;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.panel-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h4 {
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  margin: 0;
}

.suggestions-list,
.history-list {
  padding: 12px 24px;
}

.suggestion-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 6px;
}

.suggestion-item:hover {
  background: #f3f4f6;
  transform: translateX(4px);
}

.history-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
  background: #f9fafb;
}

.history-item:hover {
  background: #f3f4f6;
  transform: translateX(4px);
}

.history-question {
  font-size: 14px;
  color: #374151;
  margin-bottom: 4px;
}

.history-time {
  font-size: 12px;
  color: #9ca3af;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #f9fafb 0%, #ffffff 100%);
}

.welcome-message {
  text-align: center;
  padding: 80px 40px;
  color: #6b7280;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  color: white;
  font-size: 40px;
}

.welcome-message h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px;
}

.welcome-message p {
  font-size: 16px;
  margin: 0 0 32px;
}

.welcome-examples {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 400px;
  margin: 0 auto;
}

.message-item {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-size: 18px;
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #10b981, #34d399);
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-bubble {
  padding: 16px 20px;
  border-radius: 16px;
  line-height: 1.6;
  position: relative;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-bubble {
  background: white;
  color: #374151;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.message-bubble.loading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
  align-self: flex-start;
}

.message-content:hover .message-actions {
  opacity: 1;
}

.chart-container {
  margin-top: 16px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 300px;
}

.chat-input {
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.05);
}

.input-wrapper {
  margin-bottom: 12px;
}

.input-wrapper textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  resize: none;
  transition: all 0.2s ease;
  background: white;
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

.empty-state.small {
  padding: 20px 10px;
}

.empty-state p {
  margin: 0 0 16px;
  font-size: 14px;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-sidebar {
    width: 260px;
  }
  
  .chat-messages {
    padding: 16px;
  }
  
  .welcome-message {
    padding: 40px 20px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .chat-input {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .ai-layout {
    flex-direction: column;
  }
  
  .ai-sidebar {
    width: 100%;
    height: 200px;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
  }
  
  .chat-header {
    padding: 16px;
  }
  
  .chat-actions {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
