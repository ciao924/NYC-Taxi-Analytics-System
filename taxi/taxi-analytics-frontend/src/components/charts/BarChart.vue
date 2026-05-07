<template>
  <div ref="chartRef" :style="{ width, height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

interface ChartData {
  name: string
  value: number
}

const props = defineProps<{
  data: ChartData[]
  height?: string
  width?: string
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.data.map(item => item.name),
      axisLabel: { color: '#666', rotate: 30 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } },
      axisLabel: { color: '#666' }
    },
    series: [{
      type: 'bar',
      data: props.data.map((item, index) => ({
        name: item.name,
        value: item.value,
        itemStyle: {
          color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'][index % 5]
        }
      })),
      barWidth: '50%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }

  chart.setOption(option)
}

onMounted(() => {
  initChart()
})

watch(() => props.data, () => {
  if (chart) {
    chart.setOption({
      xAxis: { data: props.data.map(item => item.name) },
      series: [{
        data: props.data.map((item, index) => ({
          name: item.name,
          value: item.value,
          itemStyle: {
            color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'][index % 5]
          }
        }))
      }]
    })
  }
}, { deep: true })
</script>
