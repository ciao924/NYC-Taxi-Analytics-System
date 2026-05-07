<template>
  <div class="pie-chart">
    <div class="chart-header">
      <div class="chart-title">{{ title }}</div>
      <div class="chart-type-selector">
        <el-radio-group v-model="chartType" @change="handleChartTypeChange">
          <el-radio label="pie">饼图</el-radio>
          <el-radio label="doughnut">环形图</el-radio>
        </el-radio-group>
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
  data: Array<{
    name: string
    value: number
  }>
  height?: string
  showLegend?: boolean
}>(), {
  height: '300px',
  showLegend: true
})

const chartRef = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<ECharts | null>(null)
const chartType = ref<string>('doughnut')

const chartOption = computed(() => {
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: props.showLegend ? {
      orient: 'vertical',
      right: 10,
      top: 'center'
    } : {
      show: false
    },
    series: [
      {
        name: props.title,
        type: 'pie',
        radius: chartType.value === 'doughnut' ? ['40%', '70%'] : '70%',
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: props.data
      }
    ]
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

const handleChartTypeChange = () => {
  if (chartInstance.value) {
    chartInstance.value.setOption(chartOption.value)
  }
}

watch(() => props.data, () => {
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
.pie-chart {
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

.chart-type-selector {
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
}
</style>