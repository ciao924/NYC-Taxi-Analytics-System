<template>
  <div class="ai-page">
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="page-title">智能数据查询助手</h2>
        <p class="page-subtitle">AI驱动的业务数据分析平台</p>
      </div>
      <div class="header-right">
        <el-button size="small" type="primary" @click="createNewSession">
          新建会话
        </el-button>
      </div>
    </div>

    <div class="ai-layout">
      <!-- 侧边栏 -->
      <aside class="ai-sidebar">
        <!-- 会话列表 -->
        <div class="sidebar-section">
          <div class="section-header">
            <span class="section-indicator"></span>
            <span class="section-title">会话记录</span>
          </div>
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
                <span class="session-time">{{ session.createTime }}</span>
              </div>
              <div class="session-actions">
                <button class="action-btn edit-btn" @click.stop="showRenameDialog(session)" title="重命名">编辑</button>
                <button class="action-btn delete-btn" @click.stop="deleteSession(session.sessionId)" title="删除">删除</button>
              </div>
            </div>
            <div v-if="sessions.length === 0" class="empty-state">
              <div class="empty-icon"></div>
              <p>暂无会话记录</p>
              <el-button size="small" type="primary" @click="createNewSession">
                开始新对话
              </el-button>
            </div>
          </div>
        </div>

        <!-- 快捷查询 -->
        <div class="sidebar-section">
          <div class="section-header">
            <span class="section-indicator"></span>
            <span class="section-title">快捷查询</span>
          </div>
          <div class="quick-actions">
            <button
              v-for="(example, index) in quickExamples"
              :key="index"
              class="quick-btn"
              @click="applyExample(example.text)"
            >
              <span class="quick-icon">{{ example.icon }}</span>
              <span class="quick-text">{{ example.text }}</span>
            </button>
          </div>
        </div>

        <!-- 我的收藏 -->
        <div class="sidebar-section">
          <div class="section-header">
            <span class="section-indicator"></span>
            <span class="section-title">我的收藏</span>
          </div>
          <div class="favorite-list">
            <div
              v-for="fav in favorites"
              :key="fav.id"
              class="favorite-item"
              @click="applyFavorite(fav.queryText)"
            >
              <span class="favorite-text">{{ fav.queryText }}</span>
              <button class="remove-fav" @click.stop="removeFavorite(fav.id)" title="取消收藏">×</button>
            </div>
            <div v-if="favorites.length === 0" class="empty-state small">
              <p>暂无收藏</p>
            </div>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="ai-main">
        <!-- 聊天头部 -->
        <header class="chat-header">
          <div class="chat-title">
            <h2>{{ currentSessionName || '智能数据查询助手' }}</h2>
          </div>
          <div class="chat-actions">
            <button class="action-toggle" @click="toggleSuggestions" :class="{ active: showSuggestions }">
              查询建议
            </button>
            <button class="action-toggle" @click="toggleHistory" :class="{ active: showHistory }">
              查询历史
            </button>
          </div>
        </header>

        <!-- 查询建议面板 -->
        <div class="suggestions-panel" v-if="showSuggestions">
          <div class="panel-header">
            <h4>查询建议</h4>
            <button class="close-btn" @click="showSuggestions = false">×</button>
          </div>
          <div class="suggestions-list">
            <div
              v-for="suggestion in suggestions"
              :key="suggestion.id"
              class="suggestion-item"
              @click="applySuggestion(suggestion.text)"
            >
              <span class="suggestion-icon">{{ suggestion.icon }}</span>
              <span class="suggestion-text">{{ suggestion.text }}</span>
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
            <button class="close-btn" @click="showHistory = false">×</button>
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
            <div class="welcome-card">
              <h2>欢迎使用智能数据查询助手</h2>
              <p>您可以通过自然语言查询出租车运营数据</p>
              <div class="welcome-examples">
                <button v-for="(example, index) in welcomeExamples" :key="index" class="example-btn" @click="applyExample(example)">
                  {{ example }}
                </button>
              </div>
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
              <div v-if="msg.role === 'user'" class="avatar user-avatar">用户</div>
              <div v-else class="avatar ai-avatar">AI</div>
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
                <div v-else>
                  <!-- 图表区域 -->
                  <div v-if="canShowChart(msg)" class="chart-wrapper">
                    <div class="chart-header">
                      <span class="chart-title">{{ msg.chartConfig?.title || '数据分析结果' }}</span>
                      <span class="chart-type">{{ getChartTypeName(msg.chartConfig?.chart_type || 'bar') }}</span>
                    </div>
                    <div class="chart-container" :style="{ height: msg.chartHeight + 'px' }">
                      <v-chart :option="getChartOption(msg)" autoresize class="chart" />
                    </div>
                  </div>
                  <!-- 解释文本 -->
                    <div v-if="msg.explanation" class="explanation">
                      <div class="explanation-icon">说明</div>
                      <span>{{ msg.explanation }}</span>
                    </div>
                  <!-- SQL代码 -->
                  <div v-if="msg.sql" class="sql-block">
                    <div class="sql-header">
                      <span class="sql-label">生成的SQL</span>
                      <button class="copy-btn" @click="copySql(msg.sql)">
                        {{ copiedSql === msg.sql ? '已复制' : '复制' }}
                      </button>
                    </div>
                    <pre class="sql-content">{{ msg.sql }}</pre>
                  </div>
                </div>
              </div>
              <div class="message-actions" v-if="msg.role === 'assistant'">
                <button class="msg-action-btn" @click="copyMessage(msg.explanation || msg.content)" title="复制">复制</button>
                <button class="msg-action-btn" @click="addToFavorites(msg.explanation || msg.content)" title="收藏">收藏</button>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="loading" class="message-item assistant">
            <div class="message-avatar">
              <div class="avatar ai-avatar">AI</div>
            </div>
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
              @input="handleInput"
            ></textarea>
            <div class="input-hints">
              <span class="hint">提示: 尝试输入 "统计总收入最高的10个区域"</span>
            </div>
          </div>
          <div class="input-actions">
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!queryText.trim()"
              @click="sendQuery"
              class="send-btn"
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
          <el-input
            type="textarea"
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
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, ScatterChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent, ToolboxComponent } from 'echarts/components'
import { aiApi } from '@/api/ai'

use([CanvasRenderer, LineChart, BarChart, PieChart, ScatterChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, ToolboxComponent])

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
const chartContainerWidth = ref(600)
const copiedSql = ref('')

// 快捷示例
const quickExamples = [
  { icon: '趋势', text: '近7天订单趋势' },
  { icon: '收入', text: '各区域收入对比' },
  { icon: '对比', text: '黄车与绿车对比' },
  { icon: '分布', text: '支付方式分布' },
]

const welcomeExamples = [
  '查询2025年1月的订单总量',
  '统计总收入最高的10个区域',
  '分析周末与工作日的出行差异',
]

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

// 图表颜色（参考数据看板配色方案）
const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']

const getColor = (index: number) => chartColors[index % chartColors.length]

/**
 * 颜色变亮工具函数
 */
const lightenColor = (hex: string, percent: number): string => {
  const num = parseInt(hex.replace('#', ''), 16)
  const amt = Math.round(2.55 * percent)
  const R = Math.min(255, (num >> 16) + amt)
  const G = Math.min(255, ((num >> 8) & 0x00FF) + amt)
  const B = Math.min(255, (num & 0x0000FF) + amt)
  return '#' + (0x1000000 + R * 0x10000 + G * 0x100 + B).toString(16).slice(1)
}

const calculateChartHeight = (width: number) => {
  const aspectRatio = 16 / 10
  const calculatedHeight = Math.floor(width / aspectRatio)
  return Math.max(300, Math.min(500, calculatedHeight))
}

// 判断是否可以显示图表
// 严格遵循规范：必须包含完整的chartConfig配置
const canShowChart = (msg: any): boolean => {
  // 必须有chartConfig
  if (!msg.chartConfig) return false
  
  // 严格校验：chartConfig必须包含必要字段
  const chartConfig = msg.chartConfig
  const hasData = chartConfig.data && chartConfig.data.length > 0
  const hasChartType = chartConfig.chart_type && chartConfig.chart_type.trim() !== ''
  const hasXField = chartConfig.x_field && chartConfig.x_field.trim() !== ''
  const hasYField = chartConfig.y_field && chartConfig.y_field.toString().trim() !== ''
  
  // 严格遵循规范：所有必要字段都必须存在
  return hasData && hasChartType && hasXField && hasYField
}

const getChartOption = (msg: any) => {
  if (!msg.chartConfig || !msg.chartConfig.data) return {}

  // 严格遵循规范：完全使用后端返回的chartConfig
  const chartConfig = msg.chartConfig
  const chartType = chartConfig.chart_type || 'bar'
  const title = chartConfig.title || ''
  const chartData = chartConfig.data || []
  const xField = chartConfig.x_field
  const yField = chartConfig.y_field
  const showLegend = chartConfig.legend !== false
  const showPercentage = chartConfig.percentage === true
  
  // 严格校验：x_field和y_field必须存在
  if (!xField || !yField) {
    console.error('chartConfig缺少必要的字段配置', chartConfig)
    return {}
  }
  
  // 从data数组中根据x_field和y_field提取数据
  const xData = chartData.map((item: any) => item[xField])
  
  // 处理y_field（可能是单个字段名或字段名数组）
  const yFields = Array.isArray(yField) ? yField : [yField]
  
  // 根据x_field和y_field提取数据
  const getSeriesData = (field: string) => {
    return chartData.map((item: any) => {
      const value = item[field]
      return typeof value === 'number' ? value : (parseFloat(value) || 0)
    })
  }

  // 饼图配置
  if (chartType === 'pie') {
    return {
      tooltip: {
        trigger: 'item',
        formatter: showPercentage ? '{b}: {c} ({d}%)' : '{b}: {c}'
      },
      legend: {
        show: showLegend,
        orient: 'horizontal',
        bottom: 10,
        data: xData
      },
      series: [{
        name: title || '数据',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: showPercentage,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        // 严格使用x_field和y_field从数据中提取
        data: chartData.map((item: any, index: number) => ({
          value: typeof item[yFields[0]] === 'number' ? item[yFields[0]] : (parseFloat(item[yFields[0]]) || 0),
          name: item[xField],
          itemStyle: { color: getColor(index) }
        }))
      }]
    }
  }

  // 散点图配置
  if (chartType === 'scatter') {
    return {
      title: {
        text: title,
        left: 'left',
        textStyle: { fontSize: 14, fontWeight: 'bold', color: '#374151' }
      },
      tooltip: {
        trigger: 'item',
        formatter: `${xField}: {b}<br/>${yFields[0]}: {c}`
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '8%',
        top: '15%',
        containLabel: true
      },
      xAxis: { 
        type: 'category', 
        data: xData, 
        axisLabel: { fontSize: 12, color: '#6b7280' } 
      },
      yAxis: { type: 'value', axisLabel: { fontSize: 12, color: '#6b7280' } },
      series: [{
        type: 'scatter',
        data: getSeriesData(yFields[0]).map((value: number, index: number) => ({
          value: [index, value],
          symbolSize: Math.max(12, value / 150)
        })),
        itemStyle: { color: '#10b981' },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(16, 185, 129, 0.5)'
          }
        }
      }]
    }
  }

  // 柱状图配置（参考数据看板样式）
  if (chartType === 'bar') {
    const yData = getSeriesData(yFields[0])
    const barColor = getColor(0)
    return {
      title: {
        text: title,
        left: 'left',
        textStyle: { fontSize: 14, fontWeight: 'bold', color: '#374151' }
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        padding: [10, 14],
        textStyle: { color: '#374151', fontSize: 11 },
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          const value = params[0].value
          const formattedValue = value >= 1000000 ? '$' + (value / 1000000).toFixed(1) + 'M' 
            : value >= 1000 ? '$' + (value / 1000).toFixed(1) + 'K' : value.toString()
          return `<div style="font-weight: 600; margin-bottom: 8px; color: #1f2937; font-size: 11px;">${params[0].axisValue}</div>
            <div style="display: flex; justify-content: space-between; gap: 30px; font-size: 10px;">
              <span><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${params[0].color}; margin-right: 6px;"></span>${params[0].seriesName}</span>
              <span style="font-weight: 600;">${formattedValue}</span>
            </div>`
        }
      },
      grid: {
        left: '10%',
        right: '8%',
        bottom: '12%',
        top: '18%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: {
          color: '#6b7280',
          fontSize: 10,
          margin: 12,
          rotate: xData.length > 6 ? 45 : 0
        },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: '#6b7280',
          fontSize: 9,
          formatter: (value: number) => {
            if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
            else if (value >= 1000) return (value / 1000).toFixed(1) + 'K'
            return value.toString()
          }
        },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        data: yData,
        barWidth: Math.min(40, Math.max(20, 600 / xData.length)),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: barColor },
              { offset: 1, color: lightenColor(barColor, 20) }
            ]
          }
        },
        emphasis: {
          scale: true,
          itemStyle: {
            shadowBlur: 12,
            shadowColor: barColor + '50',
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: barColor },
                { offset: 1, color: lightenColor(barColor, 10) }
              ]
            }
          }
        },
        animationDuration: 1200,
        animationEasing: 'elasticOut'
      }]
    }
  }

  // 默认折线图配置（参考数据看板样式）
  const yData = getSeriesData(yFields[0])
  const lineColor = getColor(0)
  return {
    title: {
      text: title,
      left: 'left',
      textStyle: { fontSize: 14, fontWeight: 'bold', color: '#374151' }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      axisPointer: { type: 'cross', crossStyle: { color: '#9ca3af' } },
      formatter: (params: any) => {
        const value = params[0].value
        const formattedValue = value >= 1000000 ? '$' + (value / 1000000).toFixed(1) + 'M' 
          : value >= 1000 ? '$' + (value / 1000).toFixed(1) + 'K' : value.toString()
        return `<div style="font-weight: 600; margin-bottom: 8px; color: #1f2937; font-size: 11px;">${params[0].axisValue}</div>
          <div style="display: flex; justify-content: space-between; gap: 30px; font-size: 10px;">
            <span><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${params[0].color}; margin-right: 6px;"></span>${params[0].seriesName}</span>
            <span style="font-weight: 600;">${formattedValue}</span>
          </div>`
      }
    },
    grid: {
      left: '10%',
      right: '8%',
      bottom: '12%',
      top: '18%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      axisLabel: {
        color: '#6b7280',
        fontSize: 10,
        margin: 12,
        rotate: xData.length > 8 ? 45 : 0
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#6b7280',
        fontSize: 9,
        formatter: (value: number) => {
          if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
          else if (value >= 1000) return (value / 1000).toFixed(1) + 'K'
          return value.toString()
        }
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      type: 'line',
      smooth: true,
      data: yData,
      lineStyle: { color: lineColor, width: 3 },
      itemStyle: {
        color: lineColor,
        borderWidth: 2,
        borderColor: '#fff'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: lineColor + '30' },
            { offset: 1, color: lineColor + '05' }
          ]
        }
      },
      emphasis: {
        scale: true,
        itemStyle: {
          shadowBlur: 10,
          shadowColor: lineColor + '50'
        }
      },
      animationDuration: 1500,
      animationEasing: 'cubicInOut'
    }]
  }
}

const getChartTypeName = (chartType: string) => {
  const typeMap: Record<string, string> = {
    bar: '柱状图',
    line: '折线图',
    pie: '饼图',
    scatter: '散点图',
    histogram: '直方图'
  }
  return typeMap[chartType] || chartType
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
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

    const result = await response.json()
    
    // 严格遵循规范：后端返回的数据结构是 {code, message, data: {sql, chartConfig, executionTime, explanation, count}}
    const responseData = result.data
    
    // 检查响应码
    if (result.code !== 200 || !responseData) {
      throw new Error(result.message || '查询失败')
    }
    
    // 严格遵循规范：完全使用后端返回的chartConfig
    const chartHeight = canShowChart({ chartConfig: responseData.chartConfig }) 
      ? calculateChartHeight(chartContainerWidth.value) 
      : 0

    // 创建AI响应消息
    const aiResponse = {
      messageId: 'temp_' + (Date.now() + 1),
      role: 'assistant',
      content: responseData.explanation || '查询完成',
      explanation: responseData.explanation || '查询完成',
      sql: responseData.sql || null,
      chartConfig: responseData.chartConfig || null,
      chartHeight: chartHeight
    }

    messages.value.push(aiResponse)
    scrollToBottom()

    // 添加到历史记录
    history.value.unshift({
      messageId: 'history_' + Date.now(),
      question: question,
      createTime: new Date().toLocaleString('zh-CN')
    })
    if (history.value.length > 20) history.value.pop()

  } catch (error: any) {
    messages.value.push({
      messageId: 'temp_' + (Date.now() + 1),
      role: 'assistant',
      content: '',
      explanation: '抱歉，查询失败：' + (error.message || '未知错误'),
      sql: null,
      hasChart: false,
      chartConfig: null,
      chartHeight: 0
    })
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

const handleInput = () => {
  // 输入时可以添加一些动态效果
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

const copyMessage = async (content: string) => {
  if (!content) return
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('复制成功')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copySql = async (sql: string) => {
  if (!sql) return
  try {
    await navigator.clipboard.writeText(sql)
    copiedSql.value = sql
    setTimeout(() => { copiedSql.value = '' }, 2000)
    ElMessage.success('SQL已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const addToFavorites = (content: string) => {
  if (!content) return
  const exists = favorites.value.some(f => f.queryText === content)
  if (exists) {
    ElMessage.warning('已存在于收藏中')
    return
  }
  favorites.value.unshift({
    id: 'fav_' + Date.now(),
    queryText: content,
    createTime: new Date().toLocaleString('zh-CN')
  })
  if (favorites.value.length > 10) favorites.value.pop()
  ElMessage.success('已添加到收藏')
}

const removeFavorite = (id: string) => {
  favorites.value = favorites.value.filter(f => f.id !== id)
  ElMessage.success('已取消收藏')
}

const createNewSession = () => {
  const newSessionId = 'session_' + Date.now()
  const newSession = {
    sessionId: newSessionId,
    sessionName: '新会话 ' + new Date().toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
    createTime: new Date().toLocaleString('zh-CN')
  }
  sessions.value.unshift(newSession)
  currentSessionId.value = newSessionId
  currentSessionName.value = newSession.sessionName
  messages.value = []
}

const selectSession = async (sessionId: string) => {
  currentSessionId.value = sessionId
  const session = sessions.value.find(s => s.sessionId === sessionId)
  if (session) {
    currentSessionName.value = session.sessionName
  }
  
  // 从后端加载会话历史消息
  try {
    const chatMessages = await aiApi.getSessionMessages(sessionId)
    if (chatMessages && chatMessages.length > 0) {
      messages.value = chatMessages.map((msg: any) => {
        let chartConfig = null
        if (msg.chartConfig) {
          chartConfig = typeof msg.chartConfig === 'string'
            ? JSON.parse(msg.chartConfig)
            : msg.chartConfig
        }

        let explanation = msg.explanation
        let content = msg.content

        // 对于assistant消息，从content JSON中解析explanation和chartConfig
        if (msg.role === 'assistant' && content && typeof content === 'string') {
          try {
            const parsedContent = JSON.parse(content)
            if (parsedContent.explanation) {
              explanation = parsedContent.explanation
            }
            // 如果chartConfig为空，尝试从content中获取
            if (!chartConfig && parsedContent.chartConfig) {
              chartConfig = parsedContent.chartConfig
            }
          } catch (e) {
            // content不是JSON，保持原样
          }
        }

        // 计算图表高度
        const chartHeight = canShowChart({ chartConfig })
          ? calculateChartHeight(chartContainerWidth.value)
          : 0

        return {
          messageId: msg.messageId || msg.message_id,
          role: msg.role,
          content: content,
          explanation: explanation,
          sql: msg.sql || msg.sql_text,
          chartConfig: chartConfig,
          chartHeight: chartHeight
        }
      })
    } else {
      messages.value = []
    }
  } catch (error) {
    console.error('加载会话消息失败:', error)
    messages.value = []
  }
}

const showRenameDialog = (session: any) => {
  renameDialog.value = {
    visible: true,
    sessionId: session.sessionId,
    newName: session.sessionName
  }
}

const confirmRename = () => {
  const session = sessions.value.find(s => s.sessionId === renameDialog.value.sessionId)
  if (session && renameDialog.value.newName.trim()) {
    session.sessionName = renameDialog.value.newName.trim()
    if (currentSessionId.value === session.sessionId) {
      currentSessionName.value = session.sessionName
    }
    ElMessage.success('重命名成功')
  }
  renameDialog.value.visible = false
}

const deleteSession = (sessionId: string) => {
  sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
  if (currentSessionId.value === sessionId) {
    if (sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].sessionId
      currentSessionName.value = sessions.value[0].sessionName
    } else {
      currentSessionId.value = ''
      currentSessionName.value = ''
    }
    messages.value = []
  }
  ElMessage.success('删除成功')
}

const confirmSchedule = () => {
  if (!scheduleForm.value.queryText.trim() || !scheduleForm.value.scheduleTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  scheduledTasks.value.unshift({
    taskId: 'task_' + Date.now(),
    queryText: scheduleForm.value.queryText,
    scheduleTime: scheduleForm.value.scheduleTime
  })
  scheduleForm.value = { queryText: '', scheduleTime: '' }
  showScheduleDialog.value = false
  ElMessage.success('定时任务已创建')
}

const toggleSuggestions = () => {
  showSuggestions.value = !showSuggestions.value
  showHistory.value = false
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  showSuggestions.value = false
}

const handleResize = () => {
  if (messagesContainer.value) {
    chartContainerWidth.value = messagesContainer.value.offsetWidth - 120
  }
}

onMounted(async () => {
  // 从后端加载历史会话列表
  try {
    const sessionList = await aiApi.getSessions()
    if (sessionList && sessionList.length > 0) {
      sessions.value = sessionList.map((s: any) => ({
        sessionId: s.session_id,
        sessionName: s.session_name || '新对话',
        createTime: s.create_time,
        messageCount: s.message_count
      }))
      // 默认选中第一个会话
      if (sessions.value.length > 0) {
        selectSession(sessions.value[0].sessionId)
      }
    } else {
      createNewSession()
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    createNewSession()
  }
  
  // 加载查询历史
  try {
    const historyData = await aiApi.getQueryHistory(20)
    if (historyData) {
      history.value = historyData.map((h: any) => ({
        messageId: h.message_id,
        question: h.content,
        createTime: h.create_time
      }))
    }
  } catch (error) {
    console.error('加载查询历史失败:', error)
  }
  
  // 初始化示例数据
  suggestions.value = [
    { id: 1, icon: '趋势', text: '查询近7天的订单趋势' },
    { id: 2, icon: '收入', text: '统计各区域收入对比' },
    { id: 3, icon: '对比', text: '分析黄车与绿车运营差异' },
    { id: 4, icon: '支付', text: '查看支付方式占比分布' },
    { id: 5, icon: '高峰', text: '分析早晚高峰时段特征' },
    { id: 6, icon: '热点', text: '查询热门上车点排行' },
  ]

  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 2px 0 0 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 布局 */
.ai-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.ai-sidebar {
  width: 260px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.sidebar-section {
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-section:last-child {
  border-bottom: none;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #475569;
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-indicator {
  width: 4px;
  height: 14px;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 2px;
}

.section-title {
  font-size: 12px;
}

/* 会话列表 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.session-item:hover {
  background: #f8fafc;
}

.session-item.active {
  background: #f0f9ff;
  border-color: #e0f2fe;
}

.session-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.session-item.active .session-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-name {
  display: block;
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.session-time {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  background: transparent;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 12px;
  color: #94a3b8;
}

.action-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

.edit-btn:hover {
  color: #3b82f6;
}

.delete-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* 快捷查询 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.quick-btn:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
  transform: translateX(2px);
}

.quick-icon {
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  background: #f0f9ff;
  color: #0ea5e9;
  border-radius: 4px;
  letter-spacing: 0.3px;
}

.quick-text {
  font-size: 13px;
  color: #475569;
}

/* 收藏列表 */
.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.favorite-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  background: #fffdf7;
}

.favorite-item:hover {
  background: #fef3c7;
}

.favorite-text {
  flex: 1;
  font-size: 12px;
  color: #92400e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-fav {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #fbbf24;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  opacity: 0;
}

.favorite-item:hover .remove-fav {
  opacity: 1;
}

.remove-fav:hover {
  background: #f59e0b;
  color: #ffffff;
}

/* 空状态 */
.empty-state {
  padding: 20px 12px;
  text-align: center;
}

.empty-state.small {
  padding: 12px;
}

.empty-icon {
  width: 48px;
  height: 48px;
  background: #f8fafc;
  border-radius: 12px;
  margin: 0 auto 12px;
}

.empty-state p {
  color: #94a3b8;
  font-size: 13px;
  margin: 0 0 12px 0;
}

/* 主内容区 */
.ai-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fa;
}

/* 聊天头部 */
.chat-header {
  background: #ffffff;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title h2 {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.chat-actions {
  display: flex;
  gap: 6px;
}

.action-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #64748b;
  transition: all 0.15s ease;
}

.action-toggle:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.action-toggle.active {
  background: #f0f9ff;
  border-color: #38bdf8;
  color: #0284c7;
}

/* 面板 */
.suggestions-panel, .history-panel {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-header h4 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.close-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #94a3b8;
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

/* 建议列表 */
.suggestions-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 8px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.suggestion-item:hover {
  background: #f0f9ff;
  border-color: #e0f2fe;
}

.suggestion-icon {
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  background: #f0f9ff;
  color: #0284c7;
  border-radius: 4px;
  letter-spacing: 0.3px;
}

.suggestion-text {
  font-size: 13px;
  color: #475569;
}

/* 历史列表 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.history-item:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.history-question {
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

.history-time {
  font-size: 11px;
  color: #94a3b8;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 欢迎消息 */
.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.welcome-card {
  text-align: center;
  padding: 40px 48px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
}

.welcome-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 14px;
  margin: 0 auto 16px;
}

.welcome-card h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.welcome-card p {
  color: #64748b;
  font-size: 13px;
  margin: 0 0 20px 0;
}

.welcome-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.example-btn {
  padding: 8px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  color: #475569;
  transition: all 0.15s ease;
}

.example-btn:hover {
  background: #f0f9ff;
  border-color: #38bdf8;
  color: #0284c7;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-radius: 16px 16px 4px 16px;
}

.message-item.user .message-text {
  color: white;
}

.message-item.user .explanation {
  background: rgba(255, 255, 255, 0.15);
}

.message-item.user .explanation-icon {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.message-item.user .explanation span {
  color: rgba(255, 255, 255, 0.9);
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.message-content {
  max-width: 72%;
}

.message-bubble {
  background: #ffffff;
  border-radius: 16px 16px 16px 4px;
  padding: 14px 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.message-text {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

/* 图表区域 */
.chart-wrapper {
  margin-bottom: 14px;
  background: #f8fafc;
  border-radius: 10px;
  padding: 14px;
  border: 1px solid #f1f5f9;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chart-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.chart-type {
  font-size: 11px;
  color: #0284c7;
  background: #f0f9ff;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.chart-container {
  width: 100%;
}

.chart {
  width: 100%;
  height: 100%;
}

/* 解释文本 */
.explanation {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #fef3c7;
  border-radius: 6px;
}

.explanation-icon {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  background: #f59e0b;
  color: #ffffff;
  border-radius: 3px;
  flex-shrink: 0;
}

/* SQL代码块 */
.sql-block {
  background: #1e293b;
  border-radius: 8px;
  overflow: hidden;
}

.sql-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #334155;
}

.sql-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  color: #cbd5e1;
  transition: all 0.15s ease;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.sql-content {
  padding: 12px;
  margin: 0;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #e2e8f0;
  overflow-x: auto;
  line-height: 1.5;
}

/* 消息操作 */
.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-left: 2px;
}

.msg-action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  color: #64748b;
  transition: all 0.15s ease;
}

.msg-action-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #475569;
}

/* 加载状态 */
.message-bubble.loading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 输入区域 */
.chat-input {
  background: #ffffff;
  border-top: 1px solid #e5e7eb;
  padding: 14px 24px;
}

.input-wrapper {
  flex: 1;
}

.input-wrapper textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: all 0.15s ease;
  background: #f8fafc;
  color: #334155;
}

.input-wrapper textarea:focus {
  border-color: #3b82f6;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-wrapper textarea::placeholder {
  color: #94a3b8;
}

.input-hints {
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.send-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.35);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.ai-sidebar::-webkit-scrollbar {
  width: 4px;
}

.ai-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.ai-sidebar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .ai-sidebar {
    width: 220px;
  }
  
  .message-content {
    max-width: 80%;
  }
}

@media (max-width: 768px) {
  .ai-sidebar {
    width: 180px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .welcome-card {
    padding: 32px 24px;
  }
  
  .suggestions-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .ai-sidebar {
    display: none;
  }
  
  .chat-header {
    padding: 10px 16px;
  }
  
  .chat-messages {
    padding: 16px;
  }
  
  .chat-input {
    padding: 12px 16px;
  }
}
</style>
