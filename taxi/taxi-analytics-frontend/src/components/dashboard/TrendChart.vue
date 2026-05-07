<template>
  <div class="trend-chart">
    <div class="chart-header">
      <div class="chart-title">{{ title }}</div>
      <div class="metric-selector">
        <el-checkbox-group v-model="selectedMetrics" @change="handleMetricChange">
          <el-checkbox 
            v-for="metric in metrics" 
            :key="metric.key"
            :label="metric.key"
          >
            {{ metric.label }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>
    <div class="chart-content">
      <div class="chart-container" ref="chartRef" :style="{ height: height }"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, shallowRef, computed } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = withDefaults(defineProps<{
  title: string
  data: {
    dates: string[]
    metrics: Record<string, number[]>
  }
  metrics: Array<{
    key: string
    label: string
    color: string
  }>
  height?: string
}>(), {
  height: '400px'
})

const chartRef = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<ECharts | null>(null)
const selectedMetrics = ref<string[]>(props.metrics.map(m => m.key))

const chartOption = computed(() => {
  const series = selectedMetrics.value.map(key => {
    const metric = props.metrics.find(m => m.key === key)
    return {
      name: metric?.label || key,
      type: 'line',
      data: props.data.metrics[key] || [],
      itemStyle: {
        color: metric?.color || '#409EFF'
      },
      smooth: true,
      symbol: 'circle',
      symbolSize: 6
    }
  })

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: selectedMetrics.value.map(key => {
        const metric = props.metrics.find(m => m.key === key)
        return metric?.label || key
      }),
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.dates,
      axisLabel: {
        interval: props.data.dates.length > 30 ? 'auto' : 0,
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 1000000) {
            return (value / 1000000).toFixed(1) + 'M'
          } else if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'K'
          }
          return value
        }
      }
    },
    series
  } as EChartsOption
})

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance.value = echarts.init(chartRef.value)
  chartInstance.value.setOption(chartOption.value)
}

const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize()
  }
}

const handleMetricChange = () => {
  if (chartInstance.value) {
    chartInstance.value.setOption(chartOption.value)
  }
}

watch(() => props.data, () => {
  if (chartInstance.value) {
    chartInstance.value.setOption(chartOption.value)
  }
}, { deep: true })

watch(() => props.metrics, () => {
  // 当指标列表变化时，更新选中的指标
  selectedMetrics.value = props.metrics.map(m => m.key)
  if (chartInstance.value) {
    chartInstance.value.setOption(chartOption.value)
  }
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.metric-selector {
  display: flex;
  gap: 16px;
}

.chart-content {
  width: 100%;
}

.chart-container {
  width: 100%;
}

@media (max-width: 768px) {
  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .metric-selector {
    flex-wrap: wrap;
  }
}
</style>