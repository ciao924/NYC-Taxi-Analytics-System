<template>
  <div class="ai-query-container">
    <el-card class="query-card">
      <template #header>
        <div class="card-header">
          <span>智能查询</span>
          <el-tag type="success" size="small">AI 驱动</el-tag>
        </div>
      </template>

      <div class="query-form">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题，例如：近7天各供应商收入占比"
          @keyup.ctrl.enter="handleQuery"
        />
        <div class="query-actions">
          <el-button
            type="primary"
            :loading="loading"
            @click="handleQuery"
          >
            查询 (Ctrl + Enter)
          </el-button>
          <el-button @click="handleClear">
            清空
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- SQL显示区域 -->
    <el-card v-if="queryResult && queryResult.data && queryResult.data.sql" class="sql-card">
      <template #header>
        <div class="card-header">
          <span>执行的SQL</span>
          <el-button size="small" @click="copySql" type="text">复制</el-button>
        </div>
      </template>
      <pre class="sql-content">{{ queryResult.data.sql }}</pre>
    </el-card>

    <!-- 图表结果区域 -->
    <el-card 
      v-if="hasChart" 
      class="result-card"
      :class="{ 'no-data': isEmptyData }"
    >
      <template #header>
        <div class="card-header">
          <span>{{ chartTitle }}</span>
          <el-tag :type="getChartTypeTag(chartType)" size="small">
            {{ getChartTypeName(chartType) }}
          </el-tag>
        </div>
      </template>

      <!-- 暂无数据提示 -->
      <div v-if="isEmptyData" class="empty-data">
        <el-empty description="暂无数据" />
      </div>

      <!-- 图表容器 -->
      <div v-else class="chart-container" ref="chartContainer">
        <v-chart
          :option="chartOption"
          :style="{ height: chartHeight + 'px', width: '100%' }"
          autoresize
        />
      </div>
    </el-card>

    <!-- 错误提示区域 -->
    <el-card v-if="errorMessage" class="error-card">
      <template #header>
        <div class="card-header">
          <span>查询失败</span>
          <el-tag type="danger" size="small">错误</el-tag>
        </div>
      </template>
      <el-alert
        :title="errorMessage"
        type="error"
        :closable="false"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { aiApi, type AiQueryResponse, type ChartConfig } from '@/api/ai'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  ScatterChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const question = ref('')
const loading = ref(false)
const queryResult = ref<AiQueryResponse | null>(null)
const errorMessage = ref('')
const chartContainer = ref<HTMLElement | null>(null)
const chartHeight = ref(400)

// 计算图表相关属性
const chartConfig = computed<ChartConfig | null>(() => {
  if (!queryResult.value || !queryResult.value.data) {
    return null
  }
  return queryResult.value.data.chartConfig
})

const chartType = computed(() => {
  return chartConfig.value?.chart_type || 'bar'
})

const chartTitle = computed(() => {
  return chartConfig.value?.title || '数据分析结果'
})

const hasChart = computed(() => {
  return chartConfig.value !== null
})

const isEmptyData = computed(() => {
  return chartConfig.value?.data && chartConfig.value.data.length === 0
})

// 计算图表高度
const calculateChartHeight = () => {
  if (chartContainer.value) {
    const containerWidth = chartContainer.value.offsetWidth
    const aspectRatio = 16 / 10
    const calculatedHeight = Math.floor(containerWidth / aspectRatio)
    chartHeight.value = Math.max(300, Math.min(600, calculatedHeight))
  }
}

const handleResize = () => {
  calculateChartHeight()
}

onMounted(() => {
  calculateChartHeight()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

watch(queryResult, () => {
  setTimeout(() => {
    calculateChartHeight()
  }, 100)
})

/**
 * 渲染图表配置
 * 严格遵循API契约：前端只负责渲染后端返回的图表配置
 * 不得自行猜测或重新生成图表类型、X/Y轴字段
 */
const chartOption = computed(() => {
  if (!chartConfig.value) {
    return {}
  }

  const config = chartConfig.value
  const chartType = config.chart_type
  const xField = config.x_field
  const yField = config.y_field
  const data = config.data || []
  const title = config.title
  const showLegend = config.legend !== false

  // 校验必要字段
  if (!chartType || !xField || !yField || !data || data.length === 0) {
    console.error('图表配置不完整', config)
    return {}
  }

  // 从data数组中提取x轴和y轴数据
  const xAxisData = data.map(item => item[xField])
  
  // 处理y_field（可能是单个字段名或字段名数组）
  const yFields = Array.isArray(yField) ? yField : [yField]
  
  // 根据x_field和y_field提取数据
  const getSeriesData = (field: string) => {
    return data.map(item => {
      const value = item[field]
      return typeof value === 'number' ? value : (parseFloat(value) || 0)
    })
  }

  // 饼图配置
  if (chartType === 'pie') {
    return {
      title: {
        text: title,
        left: 'left',
        textStyle: { fontSize: 14, fontWeight: 'bold', color: '#374151' }
      },
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        padding: [10, 14],
        textStyle: { color: '#374151', fontSize: 11 },
        formatter: (params: any) => {
          const total = data.reduce((sum: number, item: any) => sum + (item[yFields[0]] || 0), 0)
          const percentage = total > 0 ? ((params.value / total) * 100).toFixed(1) : '0'
          return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${params.name}</div>
            <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
              <span>数值:</span><span style="font-weight: 600;">${params.value}</span>
            </div>
            <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
              <span>占比:</span><span style="font-weight: 600; color: ${params.color};">${percentage}%</span>
            </div>`
        }
      },
      legend: {
        show: showLegend,
        orient: 'horizontal',
        bottom: '0%',
        left: 'center',
        textStyle: { color: '#6b7280', fontSize: 10 },
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 14,
        padding: [0, 0, 8, 0]
      },
      series: [
        {
          name: title || '数据',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 10,
            color: '#374151',
            alignTo: 'edge',
            edgeDistance: 5
          },
          labelLine: {
            length: 8,
            length2: 6,
            smooth: true
          },
          data: data.map((item: any, index: number) => ({
            name: item[xField],
            value: item[yFields[0]],
            itemStyle: { color: getSeriesColor(index) }
          })),
          emphasis: {
            scale: true,
            scaleSize: 6,
            itemStyle: {
              shadowBlur: 12,
              shadowColor: 'rgba(0, 0, 0, 0.2)'
            }
          },
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDelay: (idx: number) => idx * 60
        }
      ]
    }
  }

  // 堆叠柱状图配置
  if (chartType === 'stacked_bar') {
    const series = yFields.map(field => ({
      name: field,
      type: 'bar',
      stack: 'total',
      data: getSeriesData(field),
      emphasis: {
        focus: 'series'
      }
    }))

    return {
      title: {
        text: title,
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        show: showLegend,
        bottom: 10,
        data: yFields
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xAxisData,
        axisLabel: {
          interval: 0,
          rotate: xAxisData.length > 6 ? 30 : 0
        }
      },
      yAxis: {
        type: 'value'
      },
      series
    }
  }

  // 水平柱状图配置
  if (chartType === 'horizontal_bar') {
    return {
      title: {
        text: title,
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        show: showLegend,
        bottom: 10,
        data: yFields
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'value'
      },
      yAxis: {
        type: 'category',
        data: xAxisData,
        axisLabel: {
          interval: 0
        }
      },
      series: [
        {
          name: yFields[0],
          type: 'bar',
          data: getSeriesData(yFields[0]),
          itemStyle: {
            color: '#409EFF'
          }
        }
      ]
    }
  }

  // 散点图配置
  if (chartType === 'scatter') {
    return {
      title: {
        text: title,
        left: 'center'
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
        data: xAxisData,
        axisLabel: {
          interval: 0,
          rotate: xAxisData.length > 6 ? 30 : 0
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: title || '数据',
          type: 'scatter',
          data: getSeriesData(yFields[0]).map((value: number, index: number) => ({
            value: [index, value],
            symbolSize: Math.max(10, value / 200)
          })),
          itemStyle: { color: '#10b981' }
        }
      ]
    }
  }

  // 表格类型（使用柱状图展示）
  if (chartType === 'table') {
    return {
      title: {
        text: title,
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        show: showLegend,
        bottom: 10,
        data: yFields
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
        data: xAxisData,
        axisLabel: {
          interval: 0,
          rotate: xAxisData.length > 6 ? 30 : 0
        }
      },
      yAxis: {
        type: 'value'
      },
      series: yFields.map(field => ({
        name: field,
        type: 'bar',
        data: getSeriesData(field),
        itemStyle: {
          color: getSeriesColor(yFields.indexOf(field))
        }
      }))
    }
  }

  // 默认：柱状图或折线图配置
  const baseOption: any = {
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
      textStyle: { color: '#374151', fontSize: 11 },
      axisPointer: {
        type: chartType === 'line' ? 'cross' : 'shadow',
        crossStyle: { color: '#9ca3af' }
      }
    },
    legend: {
      show: showLegend,
      bottom: 0,
      left: 'center',
      textStyle: { color: '#6b7280', fontSize: 10 },
      itemWidth: 12,
      itemHeight: 8,
      itemGap: 20
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
      data: xAxisData,
      axisLabel: {
        color: '#6b7280',
        fontSize: 10,
        margin: 12,
        rotate: xAxisData.length > 6 ? 30 : 0
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
    series: yFields.map((field, index) => ({
      name: field,
      type: chartType,
      data: getSeriesData(field),
      lineStyle: chartType === 'line' ? { color: getSeriesColor(index), width: 3 } : undefined,
      itemStyle: {
        color: chartType === 'bar'
          ? {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: getSeriesColor(index) },
              { offset: 1, color: lightenColor(getSeriesColor(index), 20) }
            ]
          }
          : { color: getSeriesColor(index), borderWidth: 2, borderColor: '#fff' }
      },
      areaStyle: chartType === 'line' ? {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: getSeriesColor(index) + '30' },
            { offset: 1, color: getSeriesColor(index) + '05' }
          ]
        }
      } : undefined,
      smooth: chartType === 'line',
      barWidth: chartType === 'bar' ? Math.min(40, Math.max(20, 600 / xAxisData.length)) : undefined,
      emphasis: {
        scale: true,
        itemStyle: {
          shadowBlur: 10,
          shadowColor: getSeriesColor(index) + '50'
        }
      },
      animationDuration: 1500,
      animationEasing: 'cubicInOut'
    }))
  }

  return baseOption
})

/**
 * 获取系列颜色（参考数据看板配色方案）
 */
const getSeriesColor = (index: number): string => {
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']
  return colors[index % colors.length]
}

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

/**
 * 处理查询请求
 */
const handleQuery = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入您的问题')
    return
  }

  loading.value = true
  errorMessage.value = ''
  queryResult.value = null

  try {
    const response = await aiApi.query({ query: question.value.trim() })
    
    // 检查响应码
    if (response.code !== 200) {
      throw new Error(response.message)
    }
    
    queryResult.value = response
    ElMessage.success('查询成功')
  } catch (error: any) {
    errorMessage.value = error.message || '查询失败，请稍后重试'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

/**
 * 清空查询
 */
const handleClear = () => {
  question.value = ''
  queryResult.value = null
  errorMessage.value = ''
}

/**
 * 复制SQL
 */
const copySql = async () => {
  if (queryResult.value && queryResult.value.data && queryResult.value.data.sql) {
    try {
      await navigator.clipboard.writeText(queryResult.value.data.sql)
      ElMessage.success('SQL已复制到剪贴板')
    } catch (e) {
      ElMessage.error('复制失败')
    }
  }
}

/**
 * 获取图表类型名称
 */
const getChartTypeName = (chartType: string): string => {
  const typeMap: Record<string, string> = {
    bar: '柱状图',
    line: '折线图',
    pie: '饼图',
    scatter: '散点图',
    stacked_bar: '堆叠柱状图',
    horizontal_bar: '水平柱状图',
    table: '表格'
  }
  return typeMap[chartType] || chartType
}

/**
 * 获取图表类型标签样式
 */
const getChartTypeTag = (chartType: string): string => {
  const typeMap: Record<string, string> = {
    bar: 'primary',
    line: 'success',
    pie: 'warning',
    scatter: 'info',
    stacked_bar: 'primary',
    horizontal_bar: 'primary',
    table: 'default'
  }
  return typeMap[chartType] || 'default'
}
</script>

<style scoped>
.ai-query-container {
  padding: 24px;
}

.query-card,
.result-card,
.error-card,
.sql-card {
  margin-bottom: 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: none;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}

.card-header span:first-child {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.query-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.query-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.sql-content {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Monaco', 'Menlo', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-data {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.result-card.no-data {
  min-height: 200px;
}

/* 图表区域样式 */
.result-card .el-card__body {
  padding: 20px;
}

.chart-wrapper {
  width: 100%;
  height: 100%;
}
</style>