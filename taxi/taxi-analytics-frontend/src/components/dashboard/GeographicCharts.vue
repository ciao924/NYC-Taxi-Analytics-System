<template>
  <div class="geographic-charts">
    <div class="chart-header">
      <div class="chart-title">{{ title }}</div>
      <div class="chart-controls">
        <el-select v-model="selectedMetric" @change="handleMetricChange" placeholder="选择指标">
          <el-option 
            v-for="metric in metrics" 
            :key="metric.key"
            :label="metric.label"
            :value="metric.key"
          />
        </el-select>
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
  metrics: Array<{
    key: string
    label: string
  }>
  height?: string
}>(), {
  height: '400px'
})

const chartRef = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<ECharts | null>(null)
const selectedMetric = ref<string>(props.metrics[0]?.key || '')

// 模拟纽约行政区数据
const nycBoroughs = [
  { name: 'Manhattan', value: 0 },
  { name: 'Brooklyn', value: 0 },
  { name: 'Queens', value: 0 },
  { name: 'The Bronx', value: 0 },
  { name: 'Staten Island', value: 0 }
]

// 合并实际数据
const mergedData = computed(() => {
  const dataMap = new Map(props.data.map(item => [item.name, item.value]))
  return nycBoroughs.map(borough => ({
    name: borough.name,
    value: dataMap.get(borough.name) || 0
  }))
})

const chartOption = computed(() => {
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}'
    },
    visualMap: {
      min: 0,
      max: Math.max(...mergedData.value.map(item => item.value)) || 100,
      left: 'left',
      top: 'bottom',
      text: ['高', '低'],
      calculable: true,
      inRange: {
        color: ['#e0f7fa', '#00acc1']
      }
    },
    series: [
      {
        name: props.title,
        type: 'map',
        map: '纽约',
        roam: true,
        label: {
          show: true,
          fontSize: 12
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            areaColor: '#ffcc80'
          }
        },
        data: mergedData.value
      }
    ]
  } as EChartsOption
})

const initChart = () => {
  if (!chartRef.value) return
  
  // 注册纽约地图（这里使用模拟数据，实际项目中需要加载真实的地图数据）
  registerNYCMap()
  
  chartInstance.value = echarts.init(chartRef.value)
  chartInstance.value.setOption(chartOption.value)
}

const registerNYCMap = () => {
  // 模拟纽约地图数据
  const nycMapData = {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: { name: 'Manhattan' },
        geometry: {
          type: 'Polygon',
          coordinates: [[
            [-74.0479, 40.6843],
            [-73.9066, 40.6843],
            [-73.9066, 40.8792],
            [-74.0479, 40.8792],
            [-74.0479, 40.6843]
          ]]
        }
      },
      {
        type: 'Feature',
        properties: { name: 'Brooklyn' },
        geometry: {
          type: 'Polygon',
          coordinates: [[
            [-74.0479, 40.5795],
            [-73.8373, 40.5795],
            [-73.8373, 40.6843],
            [-74.0479, 40.6843],
            [-74.0479, 40.5795]
          ]]
        }
      },
      {
        type: 'Feature',
        properties: { name: 'Queens' },
        geometry: {
          type: 'Polygon',
          coordinates: [[
            [-73.8373, 40.5795],
            [-73.7004, 40.5795],
            [-73.7004, 40.7812],
            [-73.8373, 40.7812],
            [-73.8373, 40.5795]
          ]]
        }
      },
      {
        type: 'Feature',
        properties: { name: 'The Bronx' },
        geometry: {
          type: 'Polygon',
          coordinates: [[
            [-73.9066, 40.8792],
            [-73.7898, 40.8792],
            [-73.7898, 40.9136],
            [-73.9066, 40.9136],
            [-73.9066, 40.8792]
          ]]
        }
      },
      {
        type: 'Feature',
        properties: { name: 'Staten Island' },
        geometry: {
          type: 'Polygon',
          coordinates: [[
            [-74.2591, 40.4961],
            [-74.0479, 40.4961],
            [-74.0479, 40.6461],
            [-74.2591, 40.6461],
            [-74.2591, 40.4961]
          ]]
        }
      }
    ]
  }
  
  echarts.registerMap('纽约', nycMapData as any)
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
.geographic-charts {
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

.chart-controls {
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