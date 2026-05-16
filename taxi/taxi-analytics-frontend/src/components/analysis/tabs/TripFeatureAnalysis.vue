<template>
  <div class="trip-feature-analysis">
    <div class="panel-header">
      <h3 class="panel-title">行程特征分析</h3>
      <p class="panel-desc">基于analysis_distance_distribution/duration_distribution/passenger_distribution表的行程多维度分布分析</p>
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
            <div class="summary-label">平均行程距离</div>
            <div class="summary-value">{{ avgDistance }} mi</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator duration"></div>
          <div class="card-body">
            <div class="summary-label">平均行程时长</div>
            <div class="summary-value">{{ avgDuration }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator passenger"></div>
          <div class="card-body">
            <div class="summary-label">单人出行占比</div>
            <div class="summary-value">{{ singlePassengerRatio }}%</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator tip"></div>
          <div class="card-body">
            <div class="summary-label">小费订单占比</div>
            <div class="summary-value">{{ tipOrderRatio }}%</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="funnelChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="histogramChartRef" class="chart-area"></div>
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

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>小费比率区间</th>
              <th>订单数</th>
              <th>占比</th>
              <th>平均小费($)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in processedTipData" :key="item.range">
              <td><span class="range-badge">{{ item.range }}</span></td>
              <td>{{ formatNumber(item.count) }}</td>
              <td>{{ item.ratio.toFixed(1) }}%</td>
              <td>{{ item.avgTip.toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  passengerData: any[]
  tipData: any[]
  distanceData: any[]
  durationData: any[]
  loading: boolean
}>()

const funnelChartRef = ref<HTMLElement | null>(null)
const histogramChartRef = ref<HTMLElement | null>(null)
let funnelChart: echarts.ECharts | null = null
let histogramChart: echarts.ECharts | null = null

const processedDistanceData = computed(() => {
  return props.distanceData
    .map(d => ({
      range: d.distance_range || d.dim2_name || d.dim2_value || '-',
      count: d.trip_count || 0,
      avgDistance: d.avg_distance || 0,
      ratio: 0
    }))
    .sort((a, b) => a.avgDistance - b.avgDistance)
})

const processedPassengerData = computed(() => {
  return props.passengerData
    .map(d => ({
      range: d.passenger_range || d.dim2_name || `${d.passenger_count || d.dim2_value}人`,
      count: d.trip_count || 0,
      ratio: 0
    }))
    .sort((a, b) => {
      const numA = parseInt(a.range) || 0
      const numB = parseInt(b.range) || 0
      return numA - numB
    })
})



const processedTipData = computed(() => {
  const orderMap: Record<string, number> = {
    '无小费': 0,
    '0-5%': 1,
    '5-10%': 2,
    '10-15%': 3,
    '15-20%': 4,
    '20%以上': 5
  }
  
  const data = props.tipData.map(d => ({
    range: d.tip_range || d.dim2_name || d.dim2_value || '-',
    count: d.trip_count || 0,
    ratio: 0,
    avgTip: d.avg_tip || 0
  })).filter(d => d.range !== '-').sort((a, b) => {
    return (orderMap[a.range] || 99) - (orderMap[b.range] || 99)
  })
  
  const total = data.reduce((s, d) => s + d.count, 0)
  return data.map(d => ({ ...d, ratio: total > 0 ? (d.count / total) * 100 : 0 }))
})



const avgDistance = computed(() => {
  if (processedDistanceData.value.length === 0) return '0'
  const totalDist = processedDistanceData.value.reduce((s, d) => s + d.avgDistance * d.count, 0)
  const totalCount = processedDistanceData.value.reduce((s, d) => s + d.count, 0)
  return totalCount > 0 ? (totalDist / totalCount).toFixed(1) : '0'
})

const avgDuration = computed(() => {
  if (props.durationData.length === 0) return '-'
  const total = props.durationData.reduce((s, d) => s + (d.trip_count || 0), 0)
  return total > 0 ? `${(total / props.durationData.length).toFixed(0)}min` : '-'
})

const singlePassengerRatio = computed(() => {
  if (processedPassengerData.value.length === 0) return '0'
  const single = processedPassengerData.value.find(d => d.range.includes('1'))
  const total = processedPassengerData.value.reduce((s, d) => s + d.count, 0)
  if (!single || total === 0) return '0'
  return ((single.count / total) * 100).toFixed(1)
})

const tipOrderRatio = computed(() => {
  if (props.tipData.length === 0) return '-'
  const tipped = props.tipData.filter(d => d.tip_range !== '无小费' && d.tip_range !== '0' && d.tip_range !== '0.0').reduce((s, d) => s + (d.trip_count || 0), 0)
  const total = props.tipData.reduce((s, d) => s + (d.trip_count || 0), 0)
  return total > 0 ? ((tipped / total) * 100).toFixed(1) : '0'
})

const insights = computed(() => {
  const result: string[] = []
  if (parseFloat(singlePassengerRatio.value) > 60) result.push('单人出行占比较高，可考虑推出拼车优惠')
  
  if (processedTipData.value.length > 0) {
    const zeroTip = processedTipData.value.find(d => d.range === '无小费' || parseFloat(d.range) === 0)
    const zeroTipRatio = zeroTip ? zeroTip.ratio : 0
    if (zeroTipRatio > 30) result.push(`无小费订单占比${zeroTipRatio.toFixed(1)}%，建议优化服务提升小费转化率`)
    
    const highTip = processedTipData.value.filter(d => d.range === '15-20%' || d.range === '20%以上')
    const highTipCount = highTip.reduce((s, d) => s + d.count, 0)
    const totalTipCount = processedTipData.value.reduce((s, d) => s + d.count, 0)
    if (totalTipCount > 0 && highTipCount / totalTipCount > 0.15) result.push('高小费比率（≥15%）订单占比超过15%，服务质量认可度较高')
    
    const top = [...processedTipData.value].sort((a: any, b: any) => b.count - a.count)[0]
    result.push(`${top.range}小费区间订单量最高，占${top.ratio.toFixed(1)}%`)
  }
  
  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.distanceData.length === 0 && props.passengerData.length === 0 && props.durationData.length === 0 && props.tipData.length === 0) return
  nextTick(() => { renderTipChart(); renderHistogramChart() })
}

const renderTipChart = () => {
  if (!funnelChartRef.value) return
  funnelChart?.dispose()
  funnelChart = echarts.init(funnelChartRef.value)

  const data = processedTipData.value.length > 0 ? processedTipData.value : processedDistanceData.value
  const total = data.reduce((s: number, d: any) => s + d.count, 0)

  const option: echarts.EChartsOption = {
    title: { text: '小费比率分布', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const ratio = total > 0 ? ((p.value / total) * 100).toFixed(1) : '0'
        const item = processedTipData.value.find(d => d.range === p.name)
        return `<div style="padding:8px"><strong>${p.name}</strong><br/>订单数: <strong>${p.value.toLocaleString()}</strong><br/>占比: <strong>${ratio}%</strong><br/>平均小费: <strong>$${(item?.avgTip || 0).toFixed(2)}</strong></div>`
      }
    },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((d: any) => d.range),
      axisLabel: { fontSize: 11, color: '#6b7280', rotate: data.length > 6 ? 30 : 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '订单数',
      nameTextStyle: { fontSize: 12, color: '#9ca3af' },
      axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : String(v) },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      barWidth: '65%',
      data: data.map((d: any) => ({
        value: d.count,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#059669' }
          ]),
          borderRadius: [6, 6, 0, 0]
        }
      })),
      label: { show: true, position: 'top', fontSize: 10, fontWeight: 500, color: '#6b7280', formatter: (p: any) => p.value >= 1000 ? (p.value / 1000).toFixed(0) + 'k' : String(p.value) },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  funnelChart.setOption(option)
}

const renderHistogramChart = () => {
  if (!histogramChartRef.value) return
  histogramChart?.dispose()
  histogramChart = echarts.init(histogramChartRef.value)

  const data = processedPassengerData.value.length > 0 ? processedPassengerData.value : processedDistanceData.value
  const total = data.reduce((s: number, d: any) => s + d.count, 0)

  const option: echarts.EChartsOption = {
    title: { text: processedPassengerData.value.length > 0 ? '乘客数量分布直方图' : '距离分布直方图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const ratio = total > 0 ? ((p.value / total) * 100).toFixed(1) : '0'
        return `<div style="padding:8px"><strong>${p.name}</strong><br/>订单数: <strong>${p.value.toLocaleString()}</strong><br/>占比: <strong>${ratio}%</strong></div>`
      }
    },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((d: any) => d.range),
      axisLabel: { fontSize: 11, color: '#6b7280', rotate: data.length > 6 ? 30 : 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '订单数',
      nameTextStyle: { fontSize: 12, color: '#9ca3af' },
      axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : String(v) },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [{
      type: 'bar',
      barWidth: '65%',
      data: data.map((d: any) => ({
        value: d.count,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#818cf8' },
            { offset: 1, color: '#6366f1' }
          ]),
          borderRadius: [6, 6, 0, 0]
        }
      })),
      label: { show: true, position: 'top', fontSize: 10, fontWeight: 500, color: '#6b7280', formatter: (p: any) => p.value >= 1000 ? (p.value / 1000).toFixed(0) + 'k' : String(p.value) },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  histogramChart.setOption(option)
}

const handleResize = () => { funnelChart?.resize(); histogramChart?.resize() }

watch([() => props.distanceData, () => props.passengerData], () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); funnelChart?.dispose(); histogramChart?.dispose() })
</script>

<style lang="scss" scoped>
.trip-feature-analysis { width: 100%; }

.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }

.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; }
.loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { display: flex; align-items: center; gap: 14px; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 18px 20px; transition: box-shadow 0.2s; }
.summary-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.card-indicator { width: 6px; height: 42px; border-radius: 3px; background: #6366f1; flex-shrink: 0; }
.card-indicator.duration { background: #3b82f6; }
.card-indicator.passenger { background: #10b981; }
.card-indicator.tip { background: #f59e0b; }
.summary-card.highlight { border-color: #fcd34d; background: linear-gradient(135deg, #fefce8 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #d97706; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
.summary-value { font-size: 22px; font-weight: 700; color: #1f2937; }

.chart-row { display: flex; gap: 24px; margin-bottom: 24px; }
.chart-container { flex: 1; background: #fafbfc; border-radius: 12px; padding: 16px; min-height: 360px; }
.chart-area { width: 100%; height: 340px; }

.insight-box { background: linear-gradient(135deg, #fefce8 0%, #fffbeb 100%); border: 1px solid #fde68a; border-radius: 12px; padding: 16px 20px; margin-bottom: 24px; }
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.insight-icon { font-size: 16px; }
.insight-title { font-size: 14px; font-weight: 600; color: #92400e; }
.insight-list { margin: 0; padding-left: 20px; }
.insight-list li { font-size: 13px; color: #78350f; line-height: 1.8; }

.data-table-wrapper { overflow-x: auto; border-radius: 12px; border: 1px solid #e5e7eb; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { background: #f9fafb; padding: 12px 16px; text-align: left; font-size: 13px; font-weight: 600; color: #374151; border-bottom: 2px solid #e5e7eb; }
.data-table td { padding: 10px 16px; border-bottom: 1px solid #f3f4f6; color: #4b5563; font-size: 13px; }
.data-table tr:hover td { background: #f8fafc; }
.range-badge { display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; background: #ede9fe; color: #6d28d9; }
</style>
