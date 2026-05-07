<template>
  <div class="chart-container" ref="chartRef" :style="{ width: width, height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  width?: string
  height?: string
  theme?: string | object
}>(), {
  width: '100%',
  height: '300px',
  theme: 'default'
})

const emit = defineEmits<{
  (e: 'click', params: any): void
}>()

const chartRef = ref<HTMLElement | null>(null)
// 使用 shallowRef 避免 Vue 将 echart 实例转化为响应式对象导致性能问题
const chartInstance = shallowRef<ECharts | null>(null)

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance.value = echarts.init(chartRef.value, props.theme)
  chartInstance.value.setOption(props.option)
  
  chartInstance.value.on('click', (params) => {
    emit('click', params)
  })
}

const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize()
  }
}

watch(() => props.option, (newOption) => {
  if (chartInstance.value && newOption) {
    chartInstance.value.setOption(newOption, { notMerge: false })
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
.chart-container {
  overflow: hidden;
}
</style>