<template>
  <el-card class="kpi-card" shadow="hover" :body-style="{ padding: '20px' }">
    <div class="kpi-header">
      <div class="kpi-title">{{ title }}</div>
      <el-tooltip v-if="tooltip" :content="tooltip" placement="top">
        <span class="kpi-info-icon">?</span>
      </el-tooltip>
    </div>
    
    <div class="kpi-value-container">
      <span v-if="prefix" class="kpi-prefix">{{ prefix }}</span>
      <span class="kpi-value">{{ formattedValue }}</span>
      <span v-if="suffix" class="kpi-suffix">{{ suffix }}</span>
    </div>
    
    <div class="kpi-trends" v-if="showTrend && (trendValue !== undefined || weeklyValue !== undefined)">
      <div class="kpi-trend-item" v-if="trendValue !== undefined">
        <span class="trend-label">日环比</span>
        <span class="trend-value" :class="trendValue >= 0 ? 'up' : 'down'">
          {{ trendValue >= 0 ? '↑' : '↓' }}{{ Math.abs(trendValue) }}%
        </span>
      </div>
      <div class="kpi-trend-item" v-if="weeklyValue !== undefined">
        <span class="trend-label">周同比</span>
        <span class="trend-value" :class="weeklyValue >= 0 ? 'up' : 'down'">
          {{ weeklyValue >= 0 ? '↑' : '↓' }}{{ Math.abs(weeklyValue) }}%
        </span>
      </div>
    </div>
    
    <div class="kpi-mini-chart" v-if="trendData && trendData.length > 0">
      <div class="chart-title">趋势</div>
      <div class="mini-chart-container">
        <canvas ref="chartCanvas" :width="120" :height="30"></canvas>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'

const props = defineProps<{
  title: string
  value: number | string
  prefix?: string
  suffix?: string
  tooltip?: string
  decimals?: number
  showTrend?: boolean
  trendValue?: number
  weeklyValue?: number
  trendData?: number[]
}>()

const chartCanvas = ref<HTMLCanvasElement | null>(null)

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    // 格式化数字，添加千位分隔符，处理小数
    return new Intl.NumberFormat('zh-CN', {
      minimumFractionDigits: props.decimals || 0,
      maximumFractionDigits: props.decimals || 0
    }).format(props.value)
  }
  return props.value || '0'
})

const drawMiniChart = () => {
  if (!chartCanvas.value || !props.trendData || props.trendData.length === 0) {
    return
  }
  
  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  const data = props.trendData
  const min = Math.min(...data)
  const max = Math.max(...data)
  const range = max - min || 1
  const stepX = canvas.width / (data.length - 1)
  
  // 绘制趋势线
  ctx.beginPath()
  ctx.strokeStyle = '#409EFF'
  ctx.lineWidth = 2
  
  data.forEach((value, index) => {
    const x = index * stepX
    const y = canvas.height - ((value - min) / range) * canvas.height
    
    if (index === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }
  })
  
  ctx.stroke()
  
  // 绘制数据点
  data.forEach((value, index) => {
    const x = index * stepX
    const y = canvas.height - ((value - min) / range) * canvas.height
    
    ctx.beginPath()
    ctx.fillStyle = '#409EFF'
    ctx.arc(x, y, 2, 0, 2 * Math.PI)
    ctx.fill()
  })
}

onMounted(() => {
  drawMiniChart()
})

watch(() => props.trendData, () => {
  drawMiniChart()
}, { deep: true })
</script>

<style scoped>
.kpi-card {
  height: 100%;
  border-radius: 8px;
  transition: all 0.3s;
}

.kpi-card:hover {
  transform: translateY(-2px);
}

.kpi-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.kpi-title {
  font-size: 14px;
  color: #606266;
}

.kpi-info-icon {
  color: #909399;
  cursor: pointer;
}

.kpi-value-container {
  display: flex;
  align-items: baseline;
  margin-bottom: 16px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.kpi-prefix {
  font-size: 16px;
  margin-right: 4px;
  color: #303133;
}

.kpi-suffix {
  font-size: 14px;
  margin-left: 4px;
  color: #606266;
}

.kpi-trends {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  margin-bottom: 12px;
}

.kpi-trend-item {
  display: flex;
  align-items: center;
}

.trend-label {
  color: #909399;
  margin-right: 8px;
}

.trend-value {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.trend-value.up {
  color: #f56c6c;
}

.trend-value.down {
  color: #67c23a;
}

.kpi-mini-chart {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.chart-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.mini-chart-container {
  display: flex;
  justify-content: flex-end;
}

.mini-chart-container canvas {
  vertical-align: middle;
}
</style>