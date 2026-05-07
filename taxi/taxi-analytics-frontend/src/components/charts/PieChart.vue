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
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {d}%'
      },
      data: props.data.map((item, index) => ({
        name: item.name,
        value: item.value,
        itemStyle: {
          color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'][index % 5]
        }
      }))
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
