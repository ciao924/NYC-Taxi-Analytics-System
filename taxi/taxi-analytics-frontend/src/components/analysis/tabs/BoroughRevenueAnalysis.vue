<template>
  <div class="borough-revenue-analysis">
    <div class="panel-header">
      <h3 class="panel-title">区域收入分析</h3>
      <p class="panel-desc">基于analysis_fee_by_borough表的各行政区收入贡献与订单分布分析</p>
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
            <div class="summary-label">覆盖区域数</div>
            <div class="summary-value">{{ data.length }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator trips"></div>
          <div class="card-body">
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ formatNumber(totalTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator revenue"></div>
          <div class="card-body">
            <div class="summary-label">总收入</div>
            <div class="summary-value">${{ formatNumber(totalRevenue, 2) }}</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator top"></div>
          <div class="card-body">
            <div class="summary-label">最高收入区域</div>
            <div class="summary-value">{{ topBorough }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="sankeyChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="bubbleChartRef" class="chart-area"></div>
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
  data: any[]
  loading: boolean
}>()

const sankeyChartRef = ref<HTMLElement | null>(null)
const bubbleChartRef = ref<HTMLElement | null>(null)
let sankeyChart: echarts.ECharts | null = null
let bubbleChart: echarts.ECharts | null = null

const processedData = computed(() => {
  const totalTrips = props.data.reduce((sum, d) => sum + (d.trip_count || 0), 0)
  const totalRevenue = props.data.reduce((sum, d) => sum + (d.total_revenue || 0), 0)

  return props.data.map(item => {
    const name = item.borough || 'Unknown'
    return {
      name,
      tripCount: item.trip_count || 0,
      tripRatio: totalTrips > 0 ? ((item.trip_count || 0) / totalTrips) * 100 : 0,
      revenue: item.total_revenue || 0,
      revenueRatio: totalRevenue > 0 ? ((item.total_revenue || 0) / totalRevenue) * 100 : 0,
      avgFare: item.avg_fare || 0,
      avgTip: item.avg_tip || 0
    }
  }).sort((a, b) => b.revenue - a.revenue)
})

const totalTrips = computed(() => processedData.value.reduce((sum, d) => sum + d.tripCount, 0))
const totalRevenue = computed(() => processedData.value.reduce((sum, d) => sum + d.revenue, 0))
const topBorough = computed(() => processedData.value.length > 0 ? processedData.value[0].name : '-')

const insights = computed(() => {
  const result: string[] = []
  if (processedData.value.length > 0) {
    const top = processedData.value[0]
    if (top.revenueRatio > 50) {
      result.push(`${top.name}区域收入占比达${top.revenueRatio.toFixed(1)}%，业务高度集中，建议关注区域风险分散`)
    } else if (top.revenueRatio < 20) {
      result.push(`各区域收入分布较为均衡，最高占比${top.name}仅为${top.revenueRatio.toFixed(1)}%，市场分散度良好`)
    }
  }

  const manhattan = processedData.value.find(d => d.name === 'Manhattan')
  if (manhattan && manhattan.avgFare > 30) {
    result.push(`${manhattan.name}区域平均订单金额$${manhattan.avgFare.toFixed(2)}，高于其他区域，长途商务需求集中`)
  }

  const highAvgFareAreas = processedData.value.filter(d => d.avgFare > 25)
  if (highAvgFareAreas.length > 0) {
    result.push(`${highAvgFareAreas.map(d => d.name).join('、')}等${highAvgFareAreas.length}个区域平均订单金额超过$25，商务出行需求显著`)
  }

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => {
    renderSankeyChart()
    renderBarChart()
  })
}

const renderSankeyChart = () => {
  if (!sankeyChartRef.value) return
  sankeyChart?.dispose()
  sankeyChart = echarts.init(sankeyChartRef.value)

  const sortedData = [...processedData.value].sort((a, b) => b.revenue - a.revenue)

  const nodes: any[] = [
    { name: '总订单', itemStyle: { color: '#3b82f6' } },
    { name: '总收入', itemStyle: { color: '#10b981' } }
  ]

  sortedData.forEach(d => {
    nodes.push({
      name: d.name,
      itemStyle: { color: getBoroughColor(d.name) }
    })
  })

  const links: any[] = []
  sortedData.forEach(d => {
    links.push({
      source: '总订单',
      target: d.name,
      value: d.tripCount,
      lineStyle: { color: getBoroughColor(d.name), opacity: 0.4 }
    })
    links.push({
      source: d.name,
      target: '总收入',
      value: d.revenue,
      lineStyle: { color: getBoroughColor(d.name), opacity: 0.6 }
    })
  })

  const option: echarts.EChartsOption = {
    title: {
      text: '区域收入流向桑基图',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' }
    },
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        if (params.dataType === 'edge') {
          const source = params.data.source
          const target = params.data.target
          const value = params.data.value
          const isRevenue = target === '总收入'
          return `<div style="padding:8px"><strong>${source} → ${target}</strong><br/>
            ${isRevenue ? '收入' : '订单'}: <strong>${isRevenue ? '$' + formatNumber(value, 2) : formatNumber(value)}</strong></div>`
        } else {
          const node = params.data
          const data = sortedData.find(d => d.name === node.name)
          if (data) {
            return `<div style="padding:8px"><strong>${node.name}</strong><br/>
              订单数: <strong>${formatNumber(data.tripCount)}</strong><br/>
              收入: <strong>$${formatNumber(data.revenue, 2)}</strong><br/>
              平均车费: <strong>$${data.avgFare.toFixed(2)}</strong></div>`
          }
          return `<div style="padding:8px"><strong>${node.name}</strong></div>`
        }
      }
    },
    series: [{
      type: 'sankey',
      emphasis: { focus: 'adjacency' },
      data: nodes,
      links: links,
      top: '15%',
      bottom: '10%',
      left: '10%',
      right: '10%',
      nodeWidth: 20,
      nodeGap: 8,
      label: {
        fontSize: 12,
        fontWeight: 500,
        color: '#4b5563'
      },
      lineStyle: {
        curveness: 0.5,
        opacity: 0.5
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    } as any]
  }

  sankeyChart.setOption(option)
}

const renderBarChart = () => {
  if (!bubbleChartRef.value) return
  bubbleChart?.dispose()
  bubbleChart = echarts.init(bubbleChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '区域订单与收入对比',
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
        const data = processedData.value.find(d => d.name === params[0].name)
        return `<div style="padding:8px"><strong>${params[0].name}</strong><br/>
          订单数: <strong>${formatNumber(params[0].value)}</strong><br/>
          收入: <strong>$${formatNumber(params[1].value, 2)}</strong><br/>
          平均车费: <strong>$${(data?.avgFare || 0).toFixed(2)}</strong></div>`
      }
    },
    legend: {
      data: ['订单数', '收入(千$)'],
      top: 35,
      textStyle: { fontSize: 12, color: '#6b7280' }
    },
    grid: { left: '8%', right: '8%', bottom: '8%', top: '20%', containLabel: true },
    xAxis: {
      type: 'category',
      data: processedData.value.map(d => d.name),
      axisLabel: { fontSize: 12, color: '#6b7280', rotate: processedData.value.length > 5 ? 30 : 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        nameTextStyle: { fontSize: 12, color: '#9ca3af' },
        axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : v.toString() },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '收入(千$)',
        nameTextStyle: { fontSize: 12, color: '#9ca3af' },
        axisLabel: { fontSize: 12, color: '#6b7280' },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        barWidth: '35%',
        data: processedData.value.map(d => d.tripCount),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3b82f6' },
            { offset: 1, color: '#1d4ed8' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      },
      {
        name: '收入(千$)',
        type: 'bar',
        barWidth: '35%',
        yAxisIndex: 1,
        data: processedData.value.map(d => Math.round(d.revenue / 1000)),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#059669' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }

  bubbleChart.setOption(option)
}

const getBoroughColor = (name: string): string => {
  const colors: Record<string, string> = {
    'Manhattan': '#3b82f6',
    'Brooklyn': '#8b5cf6',
    'Queens': '#f59e0b',
    'Bronx': '#10b981',
    'Staten Island': '#ef4444',
    'Ewr': '#06b6d4',
    'Jfk': '#ec4899',
    'Unknown': '#6b7280'
  }
  return colors[name] || '#6366f1'
}

const handleResize = () => {
  sankeyChart?.resize()
  bubbleChart?.resize()
}

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (!props.loading) nextTick(() => renderCharts())
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  sankeyChart?.dispose()
  bubbleChart?.dispose()
})
</script>

<style lang="scss" scoped>
.borough-revenue-analysis { width: 100%; }
.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; .loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; } }
@keyframes spin { to { transform: rotate(360deg); } }
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border: 1px solid #e0f2fe; border-radius: 12px; padding: 20px; text-align: center; position: relative; overflow: hidden; &.highlight { background: linear-gradient(135deg, #dbeafe 0%, #ffffff 100%); border-color: #93c5fd; .summary-value { color: #2563eb; } } }
.card-indicator { position: absolute; top: 0; left: 0; width: 4px; height: 100%; background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%); &.trips { background: linear-gradient(180deg, #8b5cf6 0%, #7c3aed 100%); } &.revenue { background: linear-gradient(180deg, #10b981 0%, #059669 100%); } &.top { background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%); } }
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
