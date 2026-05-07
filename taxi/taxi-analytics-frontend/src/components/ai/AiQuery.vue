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
          placeholder="请输入您的问题，例如：近7天黄车收入趋势"
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

    <el-card v-if="queryResult" class="result-card">
      <template #header>
        <div class="card-header">
          <span>查询结果</span>
          <el-tag :type="queryResult.chart === 'bar' ? 'primary' : 'success'" size="small">
            {{ getChartTypeName(queryResult.chart) }}
          </el-tag>
        </div>
      </template>

      <div class="chart-container">
        <v-chart
          :option="chartOption"
          :style="{ height: '400px' }"
        />
      </div>
    </el-card>

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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { aiApi, type AiQueryResponse } from '@/api/ai'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const question = ref('')
const loading = ref(false)
const queryResult = ref<AiQueryResponse | null>(null)
const errorMessage = ref('')

const chartOption = computed(() => {
  if (!queryResult.value) {
    return {}
  }

  const { chart, data } = queryResult.value

  const baseOption: any = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['数值']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.x,
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '数值',
        type: chart,
        data: data.y,
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }

  if (chart === 'pie') {
    baseOption.series = [
      {
        name: '数值',
        type: 'pie',
        radius: '50%',
        data: data.x.map((label, index) => ({
          name: label,
          value: data.y[index]
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  return baseOption
})

const handleQuery = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入您的问题')
    return
  }

  loading.value = true
  errorMessage.value = ''
  queryResult.value = null

  try {
    const response = await aiApi.query({ question: question.value.trim() })
    queryResult.value = response
    ElMessage.success('查询成功')
  } catch (error: any) {
    errorMessage.value = error.message || '查询失败，请稍后重试'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  question.value = ''
  queryResult.value = null
  errorMessage.value = ''
}

const getChartTypeName = (chartType: string) => {
  const typeMap: Record<string, string> = {
    bar: '柱状图',
    line: '折线图',
    pie: '饼图',
    scatter: '散点图'
  }
  return typeMap[chartType] || chartType
}
</script>

<style scoped>
.ai-query-container {
  padding: 20px;
}

.query-card,
.result-card,
.error-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.query-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.chart-container {
  width: 100%;
}
</style>