<template>
  <div class="hotspots-analysis">
    <div class="panel-header">
      <h3 class="panel-title">热点分析</h3>
      <p class="panel-desc">基于analysis_pickup_hotspots和analysis_dropoff_hotspots表的上下车热点区域分析</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div class="summary-cards">
        <div class="summary-card">
          <div class="card-indicator"></div>
          <div class="card-body">
            <div class="summary-label">上车热点区域</div>
            <div class="summary-value">{{ pickupData.length }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator dropoff"></div>
          <div class="card-body">
            <div class="summary-label">下车热点区域</div>
            <div class="summary-value">{{ dropoffData.length }}</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator top-pickup"></div>
          <div class="card-body">
            <div class="summary-label">热门上车区域</div>
            <div class="summary-value">{{ topPickupZone }}</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator top-dropoff"></div>
          <div class="card-body">
            <div class="summary-label">热门下车区域</div>
            <div class="summary-value">{{ topDropoffZone }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="wordCloudChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="rankingChartRef" class="chart-area"></div>
        </div>
      </div>

      <div class="insight-box" v-if="insights.length > 0">
        <div class="insight-header">
          <span class="insight-icon">💡</span>
          <span class="insight-title">分析洞察</span>
        </div>
        <ul class="insight-list">
          <li v-for="(insight, idx) in insights" :key="idx">{{ insight }}</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  pickupData: any[]
  dropoffData: any[]
  loading: boolean
}>()

const wordCloudChartRef = ref<HTMLElement | null>(null)
const rankingChartRef = ref<HTMLElement | null>(null)
let wordCloudChart: echarts.ECharts | null = null
let rankingChart: echarts.ECharts | null = null

const processedPickupData = computed(() => {
  return props.pickupData.map(item => ({
    zoneName: item.zone_name || item.zoneName || '-',
    borough: item.borough || '-',
    serviceZone: item.service_zone || item.serviceZone || '-',
    tripCount: item.trip_count || 0,
    revenue: item.total_revenue || 0
  }))
})

const processedDropoffData = computed(() => {
  return props.dropoffData.map(item => ({
    zoneName: item.zone_name || item.zoneName || '-',
    borough: item.borough || '-',
    serviceZone: item.service_zone || item.serviceZone || '-',
    tripCount: item.trip_count || 0
  }))
})

const renderCharts = () => {
  if (props.loading) return
  nextTick(() => {
    renderWordCloudChart()
    renderRankingChart()
  })
}

const renderWordCloudChart = () => {
  if (!wordCloudChartRef.value || processedPickupData.value.length === 0) return
  wordCloudChart?.dispose()
  wordCloudChart = echarts.init(wordCloudChartRef.value)

  const data = processedPickupData.value.slice(0, 20).map((d, idx) => ({
    name: d.zoneName,
    value: d.tripCount,
    itemStyle: { color: getZoneColor(idx) }
  }))

  const maxTripCount = Math.max(...data.map(d => d.value), 1)

  const option: echarts.EChartsOption = {
    title: {
      text: '上车热点词云图',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const data = processedPickupData.value.find(d => d.zoneName === params.name)
        return `<div style="padding:8px"><strong>${params.name}</strong><br/>
          订单数: <strong>${formatNumber(params.value)}</strong><br/>
          区域: <strong>${data?.borough || '-'}</strong></div>`
      }
    },
    series: [{
      type: 'scatter',
      symbolSize: (data: any) => {
        const ratio = data[1] / maxTripCount
        return Math.max(20, Math.min(80, 20 + ratio * 60))
      },
      data: data.map(d => ({
        name: d.name,
        value: [Math.random() * 100, Math.random() * 100, d.value],
        itemStyle: d.itemStyle
      })),
      label: {
        show: true,
        formatter: '{b}',
        fontSize: 12,
        fontWeight: 500,
        color: '#4b5563',
        position: 'inside'
      },
      itemStyle: {
        opacity: 0.7,
        borderColor: '#ffffff',
        borderWidth: 2
      },
      emphasis: {
        focus: 'series',
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        }
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }],
    grid: { show: false },
    xAxis: { show: false },
    yAxis: { show: false }
  }

  wordCloudChart.setOption(option)
}

const renderRankingChart = () => {
  if (!rankingChartRef.value || processedPickupData.value.length === 0) return
  rankingChart?.dispose()
  rankingChart = echarts.init(rankingChartRef.value)

  const topPickup = processedPickupData.value.slice(0, 10)
  const topDropoff = processedDropoffData.value.slice(0, 10)

  const option: echarts.EChartsOption = {
    title: {
      text: '热点区域排行榜',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const pickup = params[0]
        const dropoff = params[1]
        return `<div style="padding:8px"><strong>${pickup.name}</strong><br/>
          上车: <strong>${formatNumber(pickup.value)}</strong>单<br/>
          下车: <strong>${formatNumber(dropoff.value)}</strong>单</div>`
      }
    },
    legend: {
      data: ['上车订单', '下车订单'],
      top: 35,
      textStyle: { fontSize: 12, color: '#6b7280' }
    },
    grid: { left: '28%', right: '12%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: {
      type: 'value',
      name: '订单数',
      nameTextStyle: { fontSize: 12, color: '#9ca3af' },
      axisLabel: {
        fontSize: 12,
        color: '#6b7280',
        formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : v.toString()
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: topPickup.map(d => d.zoneName.length > 15 ? d.zoneName.substring(0, 15) + '...' : d.zoneName),
      axisLabel: { fontSize: 12, color: '#6b7280' },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    series: [
      {
        name: '上车订单',
        type: 'bar',
        data: topPickup.map(d => d.tripCount),
        barWidth: '35%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#2563eb' }
          ]),
          borderRadius: [4, 0, 0, 4]
        },
        label: {
          show: true,
          position: 'insideRight',
          formatter: (params: any) => params.value.toLocaleString(),
          fontSize: 11,
          fontWeight: 500,
          color: '#ffffff'
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      },
      {
        name: '下车订单',
        type: 'bar',
        data: topDropoff.map(d => {
          const pickupZone = topPickup.find(p => p.zoneName === d.zoneName)
          return pickupZone ? d.tripCount : 0
        }),
        barWidth: '35%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#a78bfa' },
            { offset: 1, color: '#7c3aed' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'insideRight',
          formatter: (params: any) => params.value > 0 ? params.value.toLocaleString() : '',
          fontSize: 11,
          fontWeight: 500,
          color: '#ffffff'
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }

  rankingChart.setOption(option)
}

const handleResize = () => {
  wordCloudChart?.resize()
  rankingChart?.resize()
}

watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
watch(() => props.pickupData, () => renderCharts(), { deep: true })
watch(() => props.dropoffData, () => renderCharts(), { deep: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (!props.loading) nextTick(() => renderCharts())
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wordCloudChart?.dispose()
  rankingChart?.dispose()
})

const topPickupZone = computed(() => {
  if (processedPickupData.value.length === 0) return '-'
  return processedPickupData.value[0].zoneName
})

const topDropoffZone = computed(() => {
  if (processedDropoffData.value.length === 0) return '-'
  return processedDropoffData.value[0].zoneName
})

const insights = computed(() => {
  const result: string[] = []

  if (processedPickupData.value.length > 0) {
    const topPickup = processedPickupData.value[0]
    result.push(`${topPickup.zoneName}是上车订单最密集的区域，共${formatNumber(topPickup.tripCount)}单，可优先配置运力资源`)

    const manhattanZones = processedPickupData.value.filter(z => z.borough === 'Manhattan')
    if (manhattanZones.length > 0) {
      const manhattanShare = manhattanZones.reduce((sum, z) => sum + z.tripCount, 0) / processedPickupData.value.reduce((sum, z) => sum + z.tripCount, 0) * 100
      if (manhattanShare > 60) {
        result.push(`曼哈顿区域占据上车订单的${manhattanShare.toFixed(1)}%，是核心业务区域`)
      }
    }
  }

  if (processedPickupData.value.length > 0 && processedDropoffData.value.length > 0) {
    const topPickup = processedPickupData.value[0].zoneName
    const topDropoff = processedDropoffData.value[0].zoneName
    if (topPickup === topDropoff) {
      result.push(`${topPickup}既是热门上车点也是热门下车点，是核心交通枢纽`)
    }
  }

  const top5Pickup = processedPickupData.value.slice(0, 5)
  const top5Share = top5Pickup.reduce((sum, z) => sum + z.tripCount, 0) / processedPickupData.value.reduce((sum, z) => sum + z.tripCount, 0) * 100
  if (top5Share > 50) {
    result.push(`TOP5热点区域占总订单的${top5Share.toFixed(1)}%，业务集中度较高`)
  }

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const getZoneColor = (idx: number): string => {
  const colors = [
    '#3b82f6', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444',
    '#06b6d4', '#ec4899', '#84cc16', '#f97316', '#6366f1',
    '#14b8a6', '#a855f7', '#eab308', '#22c55e', '#f43f5e',
    '#0ea5e9', '#d946ef', '#65a30d', '#ea580c', '#4f46e5'
  ]
  return colors[idx % colors.length]
}
</script>

<style lang="scss" scoped>
.hotspots-analysis { width: 100%; }
.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; .loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; } }
@keyframes spin { to { transform: rotate(360deg); } }
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border: 1px solid #e0f2fe; border-radius: 12px; padding: 20px; text-align: center; position: relative; overflow: hidden; &.highlight { background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%); border-color: #fcd34d; .summary-value { color: #d97706; } } }
.card-indicator { position: absolute; top: 0; left: 0; width: 4px; height: 100%; background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%); &.dropoff { background: linear-gradient(180deg, #8b5cf6 0%, #7c3aed 100%); } &.top-pickup { background: linear-gradient(180deg, #10b981 0%, #059669 100%); } &.top-dropoff { background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%); } }
.card-body { padding-left: 12px; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.summary-value { font-size: 24px; font-weight: 600; color: #0369a1; }
.chart-row { display: flex; gap: 24px; margin-bottom: 24px; }
.chart-container { flex: 1; background: #fafbfc; border-radius: 12px; padding: 16px; min-height: 360px; }
.chart-area { width: 100%; height: 340px; }
.insight-box { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); border: 1px solid #fcd34d; border-radius: 12px; padding: 20px; margin-bottom: 24px; }
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.insight-icon { font-size: 18px; }
.insight-title { font-size: 14px; font-weight: 600; color: #92400e; }
.insight-list { margin: 0; padding-left: 20px; color: #78350f; font-size: 14px; line-height: 1.8; }
</style>
