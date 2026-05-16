<template>
  <div class="taxi-type-analysis">
    <div class="panel-header">
      <h3 class="panel-title">车型费用分析</h3>
      <p class="panel-desc">基于analysis_fee_by_taxi_type表的黄色、绿色出租车费用及小费数据对比分析</p>
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
            <div class="summary-label">车型种类</div>
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
          <div class="card-indicator tip"></div>
          <div class="card-body">
            <div class="summary-label">平均小费率</div>
            <div class="summary-value">{{ avgTipRate }}%</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="comparisonChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="gaugeChartRef" class="chart-area"></div>
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

const comparisonChartRef = ref<HTMLElement | null>(null)
const gaugeChartRef = ref<HTMLElement | null>(null)
let comparisonChart: echarts.ECharts | null = null
let gaugeChart: echarts.ECharts | null = null

const taxiTypeInfo: Record<string, { name: string; color: string }> = {
  'yellow': { name: '黄色出租车', color: '#f59e0b' },
  'green': { name: '绿色出租车', color: '#10b981' },
  'fhv': { name: '预约车(FHV)', color: '#3b82f6' },
  'shared': { name: '拼车', color: '#8b5cf6' }
}

const processedData = computed(() => {
  const totalTrips = props.data.reduce((sum, d) => sum + (d.trip_count || 0), 0)

  return props.data.map(item => {
    const taxiType = item.taxi_type || item.taxiType || ''
    const info = taxiTypeInfo[taxiType] || { name: taxiType, color: '#6b7280' }
    const tripCount = item.trip_count || 0
    const revenue = item.total_revenue || 0
    return {
      taxiType,
      taxiTypeName: info.name,
      taxiTypeColor: info.color,
      tripCount,
      ratio: totalTrips > 0 ? (tripCount / totalTrips) * 100 : 0,
      revenue,
      avgFare: tripCount > 0 ? revenue / tripCount : 0,
      avgTip: item.avg_tip || 0,
      tipRate: item.tip_rate || 0
    }
  }).sort((a, b) => b.revenue - a.revenue)
})

const totalTrips = computed(() => processedData.value.reduce((sum, d) => sum + d.tripCount, 0))
const totalRevenue = computed(() => processedData.value.reduce((sum, d) => sum + d.revenue, 0))
const avgTipRate = computed(() => {
  const weighted = processedData.value.reduce((sum, d) => sum + d.tipRate * d.tripCount, 0)
  return totalTrips.value > 0 ? (weighted / totalTrips.value).toFixed(1) : '0.0'
})

const insights = computed(() => {
  const result: string[] = []

  if (processedData.value.length > 1) {
    const [first, second] = processedData.value
    if (first.avgFare > second.avgFare * 1.2) {
      result.push(`${first.taxiTypeName}平均订单金额比${second.taxiTypeName}高${((first.avgFare / second.avgFare - 1) * 100).toFixed(1)}%，适合定位高端市场`)
    }
    if (first.tipRate > second.tipRate * 1.3) {
      result.push(`${first.taxiTypeName}小费比率明显高于${second.taxiTypeName}，服务质量差异显著`)
    }
  }

  const yellowTaxi = processedData.value.find(d => d.taxiType === 'yellow')
  if (yellowTaxi && yellowTaxi.revenue > totalRevenue.value * 0.7) {
    result.push('黄色出租车仍是主要收入来源，建议持续优化其服务质量和运力配置')
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
    renderComparisonChart()
    renderTipChart()
  })
}

const renderComparisonChart = () => {
  if (!comparisonChartRef.value) return
  comparisonChart?.dispose()
  comparisonChart = echarts.init(comparisonChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '车型订单占比',
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
        const data = processedData.value.find(d => d.taxiTypeName === params.name)
        return `<div style="padding:8px"><strong>${params.name}</strong><br/>
          订单数: <strong>${formatNumber(params.value)}</strong><br/>
          占比: <strong>${params.percent.toFixed(1)}%</strong><br/>
          平均车费: <strong>$${(data?.avgFare || 0).toFixed(2)}</strong></div>`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: { fontSize: 12, color: '#6b7280' }
    },
    series: [{
      name: '车型订单',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['55%', '55%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#ffffff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {d}%',
        fontSize: 11,
        fontWeight: 500,
        color: '#4b5563'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 13,
          fontWeight: 600
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        }
      },
      data: processedData.value.map(d => ({
        value: d.tripCount,
        name: d.taxiTypeName,
        itemStyle: { color: d.taxiTypeColor }
      })),
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }

  comparisonChart.setOption(option)
}

const renderTipChart = () => {
  if (!gaugeChartRef.value) return
  gaugeChart?.dispose()
  gaugeChart = echarts.init(gaugeChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '车型平均小费',
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
        const data = processedData.value.find(d => d.taxiTypeName === params[0].name)
        return `<div style="padding:8px"><strong>${params[0].name}</strong><br/>
          小费比率: <strong>${(data?.tipRate || 0).toFixed(1)}%</strong><br/>
          平均小费: <strong>$${params[0].value.toFixed(2)}</strong><br/>
          订单数: <strong>${formatNumber(data?.tripCount || 0)}</strong></div>`
      }
    },
    grid: { left: '10%', right: '10%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: processedData.value.map(d => d.taxiTypeName),
      axisLabel: { fontSize: 12, color: '#6b7280', rotate: processedData.value.length > 4 ? 30 : 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '平均小费($)',
      nameTextStyle: { fontSize: 12, color: '#9ca3af' },
      axisLabel: { fontSize: 12, color: '#6b7280', formatter: '$' + '{value}' },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      barWidth: '50%',
      data: processedData.value.map(d => ({
        value: d.avgTip,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: d.taxiTypeColor },
            { offset: 1, color: adjustColor(d.taxiTypeColor, -20) }
          ]),
          borderRadius: [6, 6, 0, 0]
        }
      })),
      label: { 
        show: true, 
        position: 'top', 
        fontSize: 11, 
        fontWeight: 500, 
        color: '#6b7280', 
        formatter: (p: any) => '$' + p.value.toFixed(2) 
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }

  gaugeChart.setOption(option)
}

const adjustColor = (color: string, amount: number): string => {
  const hex = color.replace('#', '')
  const r = Math.max(0, Math.min(255, parseInt(hex.substring(0, 2), 16) + amount))
  const g = Math.max(0, Math.min(255, parseInt(hex.substring(2, 4), 16) + amount))
  const b = Math.max(0, Math.min(255, parseInt(hex.substring(4, 6), 16) + amount))
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
}

const handleResize = () => {
  comparisonChart?.resize()
  gaugeChart?.resize()
}

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (!props.loading) nextTick(() => renderCharts())
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  comparisonChart?.dispose()
  gaugeChart?.dispose()
})
</script>

<style lang="scss" scoped>
.taxi-type-analysis { width: 100%; }
.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; .loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; } }
@keyframes spin { to { transform: rotate(360deg); } }
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border: 1px solid #e0f2fe; border-radius: 12px; padding: 20px; text-align: center; position: relative; overflow: hidden; &.highlight { background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%); border-color: #fcd34d; .summary-value { color: #d97706; } } }
.card-indicator { position: absolute; top: 0; left: 0; width: 4px; height: 100%; background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%); &.trips { background: linear-gradient(180deg, #10b981 0%, #059669 100%); } &.revenue { background: linear-gradient(180deg, #8b5cf6 0%, #7c3aed 100%); } &.tip { background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%); } }
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
