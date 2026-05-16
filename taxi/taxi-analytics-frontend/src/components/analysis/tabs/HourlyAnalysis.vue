<template>
  <div class="hourly-analysis">
    <div class="panel-header">
      <h3 class="panel-title">时段分布分析</h3>
      <p class="panel-desc">基于analysis_hourly_distribution表的24小时订单分布，识别高峰时段与低谷时段</p>
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
            <div class="summary-label">日均订单</div>
            <div class="summary-value">{{ formatNumber(dailyAvgTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator peak"></div>
          <div class="card-body">
            <div class="summary-label">高峰时段</div>
            <div class="summary-value peak-value">{{ peakHour }}:00</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator low"></div>
          <div class="card-body">
            <div class="summary-label">低谷时段</div>
            <div class="summary-value low-value">{{ lowHour }}:00</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator ratio"></div>
          <div class="card-body">
            <div class="summary-label">峰谷比</div>
            <div class="summary-value">{{ peakLowRatio }}:1</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container full">
          <div ref="areaChartRef" class="chart-area"></div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container full">
          <div ref="heatmapBarRef" class="chart-area"></div>
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

      <div class="time-periods">
        <div class="period-card morning">
          <div class="period-label">早高峰 6-9时</div>
          <div class="period-value">{{ formatNumber(morningTrips) }}</div>
          <div class="period-ratio">{{ morningRatio }}%</div>
        </div>
        <div class="period-card noon">
          <div class="period-label">午间 9-12时</div>
          <div class="period-value">{{ formatNumber(noonTrips) }}</div>
          <div class="period-ratio">{{ noonRatio }}%</div>
        </div>
        <div class="period-card afternoon">
          <div class="period-label">下午 12-17时</div>
          <div class="period-value">{{ formatNumber(afternoonTrips) }}</div>
          <div class="period-ratio">{{ afternoonRatio }}%</div>
        </div>
        <div class="period-card evening">
          <div class="period-label">晚高峰 17-20时</div>
          <div class="period-value">{{ formatNumber(eveningTrips) }}</div>
          <div class="period-ratio">{{ eveningRatio }}%</div>
        </div>
        <div class="period-card night">
          <div class="period-label">夜间 20-24时</div>
          <div class="period-value">{{ formatNumber(nightTrips) }}</div>
          <div class="period-ratio">{{ nightRatio }}%</div>
        </div>
        <div class="period-card latenight">
          <div class="period-label">深夜 0-6时</div>
          <div class="period-value">{{ formatNumber(lateNightTrips) }}</div>
          <div class="period-ratio">{{ lateNightRatio }}%</div>
        </div>
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

const areaChartRef = ref<HTMLElement | null>(null)
const heatmapBarRef = ref<HTMLElement | null>(null)
let areaChart: echarts.ECharts | null = null
let heatmapBar: echarts.ECharts | null = null

const hourlyData = computed(() => {
  const hours = Array.from({ length: 24 }, (_, i) => ({
    hour: i,
    tripCount: 0,
    avgFare: 0,
    totalRevenue: 0,
    avgTip: 0
  }))
  props.data.forEach(d => {
    const h = d.hour_of_day ?? d.hour ?? d.hourOfDay
    if (h !== undefined && h >= 0 && h < 24) {
      hours[h].tripCount += d.trip_count || 0
      hours[h].avgFare = d.avg_fare || d.avgFare || 0
      hours[h].totalRevenue += d.total_revenue || d.totalRevenue || 0
      hours[h].avgTip = d.avg_tip || d.avgTip || 0
    }
  })
  return hours
})

const totalTrips = computed(() => hourlyData.value.reduce((s, d) => s + d.tripCount, 0))
const dailyAvgTrips = computed(() => totalTrips.value > 0 ? Math.round(totalTrips.value / 24) : 0)
const peakHour = computed(() => {
  const max = hourlyData.value.reduce((a, b) => a.tripCount > b.tripCount ? a : b, hourlyData.value[0])
  return max.hour
})
const lowHour = computed(() => {
  const min = hourlyData.value.reduce((a, b) => a.tripCount < b.tripCount ? a : b, hourlyData.value[0])
  return min.hour
})
const peakLowRatio = computed(() => {
  const peak = hourlyData.value[peakHour.value]?.tripCount || 1
  const low = hourlyData.value[lowHour.value]?.tripCount || 1
  return low > 0 ? (peak / low).toFixed(1) : '-'
})

const getPeriodTrips = (start: number, end: number) => hourlyData.value.filter(d => d.hour >= start && d.hour < end).reduce((s, d) => s + d.tripCount, 0)
const morningTrips = computed(() => getPeriodTrips(6, 9))
const noonTrips = computed(() => getPeriodTrips(9, 12))
const afternoonTrips = computed(() => getPeriodTrips(12, 17))
const eveningTrips = computed(() => getPeriodTrips(17, 20))
const nightTrips = computed(() => getPeriodTrips(20, 24))
const lateNightTrips = computed(() => getPeriodTrips(0, 6))

const morningRatio = computed(() => totalTrips.value > 0 ? ((morningTrips.value / totalTrips.value) * 100).toFixed(1) : '0')
const noonRatio = computed(() => totalTrips.value > 0 ? ((noonTrips.value / totalTrips.value) * 100).toFixed(1) : '0')
const afternoonRatio = computed(() => totalTrips.value > 0 ? ((afternoonTrips.value / totalTrips.value) * 100).toFixed(1) : '0')
const eveningRatio = computed(() => totalTrips.value > 0 ? ((eveningTrips.value / totalTrips.value) * 100).toFixed(1) : '0')
const nightRatio = computed(() => totalTrips.value > 0 ? ((nightTrips.value / totalTrips.value) * 100).toFixed(1) : '0')
const lateNightRatio = computed(() => totalTrips.value > 0 ? ((lateNightTrips.value / totalTrips.value) * 100).toFixed(1) : '0')

const insights = computed(() => {
  const result: string[] = []
  if (parseFloat(peakLowRatio.value) > 5) result.push('峰谷比超过5:1，供需波动剧烈，建议动态调配运力')
  const eveningPeak = eveningTrips.value + nightTrips.value
  if (totalTrips.value > 0 && eveningPeak / totalTrips.value > 0.4) result.push('晚间出行需求旺盛，可考虑增加夜间运力投放')
  if (lateNightTrips.value > 0 && totalTrips.value > 0 && lateNightTrips.value / totalTrips.value < 0.05) result.push('深夜时段出行极少，可优化夜间定价策略')
  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => { renderAreaChart(); renderHeatmapBar() })
}

const renderAreaChart = () => {
  if (!areaChartRef.value) return
  areaChart?.dispose()
  areaChart = echarts.init(areaChartRef.value)

  const data = hourlyData.value

  const option: echarts.EChartsOption = {
    title: { text: '24小时订单趋势面积图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const d = data[p.dataIndex]
        return `<div style="padding:8px"><strong>${d.hour}:00 - ${d.hour}:59</strong><br/>
          订单数: <strong>${d.tripCount.toLocaleString()}</strong><br/>
          平均车费: <strong>$${d.avgFare.toFixed(2)}</strong><br/>
          总收入: <strong>$${formatNumber(d.totalRevenue, 2)}</strong></div>`
      }
    },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => `${d.hour}:00`),
      axisLabel: { fontSize: 11, color: '#6b7280', interval: 1 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        nameTextStyle: { fontSize: 12, color: '#9ca3af' },
        axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : String(v) },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '平均车费($)',
        nameTextStyle: { fontSize: 12, color: '#9ca3af' },
        axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => '$' + v.toFixed(0) },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 3, color: '#3b82f6' },
        itemStyle: { color: '#3b82f6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59,130,246,0.35)' },
            { offset: 0.5, color: 'rgba(59,130,246,0.12)' },
            { offset: 1, color: 'rgba(59,130,246,0.02)' }
          ])
        },
        markPoint: {
          data: [
            { type: 'max', name: '峰值', symbolSize: 50, label: { fontSize: 10 } },
            { type: 'min', name: '谷值', symbolSize: 50, label: { fontSize: 10 } }
          ],
          itemStyle: { color: '#3b82f6' }
        },
        markLine: {
          data: [{ type: 'average', name: '均值' }],
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { fontSize: 10, color: '#6b7280' }
        },
        data: data.map(d => d.tripCount),
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      },
      {
        name: '平均车费',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 3,
        lineStyle: { width: 2, color: '#f59e0b', type: 'dashed' },
        itemStyle: { color: '#f59e0b' },
        data: data.map(d => d.avgFare),
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }
  areaChart.setOption(option)
}

const renderHeatmapBar = () => {
  if (!heatmapBarRef.value) return
  heatmapBar?.dispose()
  heatmapBar = echarts.init(heatmapBarRef.value)

  const data = hourlyData.value
  const maxTrips = Math.max(...data.map(d => d.tripCount), 1)

  const option: echarts.EChartsOption = {
    title: { text: '时段热力条', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const d = data[p.dataIndex]
        return `<div style="padding:8px"><strong>${d.hour}:00</strong><br/>订单数: <strong>${d.tripCount.toLocaleString()}</strong><br/>热度: <strong>${((d.tripCount / maxTrips) * 100).toFixed(0)}%</strong></div>`
      }
    },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => `${d.hour}h`),
      axisLabel: { fontSize: 10, color: '#6b7280', interval: 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: { show: false },
    series: [{
      type: 'bar',
      barWidth: '85%',
      data: data.map(d => {
        const ratio = d.tripCount / maxTrips
        let color: string
        if (ratio > 0.8) color = '#dc2626'
        else if (ratio > 0.6) color = '#f59e0b'
        else if (ratio > 0.4) color = '#3b82f6'
        else if (ratio > 0.2) color = '#60a5fa'
        else color = '#93c5fd'
        return {
          value: d.tripCount,
          itemStyle: { color, borderRadius: [4, 4, 0, 0] }
        }
      }),
      label: { show: false },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  heatmapBar.setOption(option)
}

const handleResize = () => { areaChart?.resize(); heatmapBar?.resize() }

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); areaChart?.dispose(); heatmapBar?.dispose() })
</script>

<style lang="scss" scoped>
.hourly-analysis { width: 100%; }

.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }

.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; }
.loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { display: flex; align-items: center; gap: 14px; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 18px 20px; transition: box-shadow 0.2s; }
.summary-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.card-indicator { width: 6px; height: 42px; border-radius: 3px; background: #3b82f6; flex-shrink: 0; }
.card-indicator.peak { background: #ef4444; }
.card-indicator.low { background: #60a5fa; }
.card-indicator.ratio { background: #f59e0b; }
.summary-card.highlight { border-color: #fcd34d; background: linear-gradient(135deg, #fefce8 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #d97706; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
.summary-value { font-size: 22px; font-weight: 700; color: #1f2937; }
.peak-value { color: #dc2626 !important; }
.low-value { color: #3b82f6 !important; }

.chart-row { display: flex; gap: 24px; margin-bottom: 24px; }
.chart-container { flex: 1; background: #fafbfc; border-radius: 12px; padding: 16px; min-height: 360px; }
.chart-area { width: 100%; height: 340px; }

.insight-box { background: linear-gradient(135deg, #fefce8 0%, #fffbeb 100%); border: 1px solid #fde68a; border-radius: 12px; padding: 16px 20px; margin-bottom: 24px; }
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.insight-icon { font-size: 16px; }
.insight-title { font-size: 14px; font-weight: 600; color: #92400e; }
.insight-list { margin: 0; padding-left: 20px; }
.insight-list li { font-size: 13px; color: #78350f; line-height: 1.8; }

.time-periods { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.period-card { border-radius: 10px; padding: 14px 12px; text-align: center; border: 1px solid #e5e7eb; }
.period-card.morning { background: linear-gradient(135deg, #fef3c7, #fffbeb); border-color: #fde68a; }
.period-card.noon { background: linear-gradient(135deg, #fff7ed, #ffffff); border-color: #fed7aa; }
.period-card.afternoon { background: linear-gradient(135deg, #eff6ff, #ffffff); border-color: #bfdbfe; }
.period-card.evening { background: linear-gradient(135deg, #fef2f2, #ffffff); border-color: #fecaca; }
.period-card.night { background: linear-gradient(135deg, #f5f3ff, #ffffff); border-color: #c4b5fd; }
.period-card.latenight { background: linear-gradient(135deg, #1e1b4b, #312e81); border-color: #4338ca; }
.period-card.latenight .period-label { color: #c4b5fd; }
.period-card.latenight .period-value { color: #ffffff; }
.period-card.latenight .period-ratio { color: #a5b4fc; }
.period-label { font-size: 12px; color: #6b7280; margin-bottom: 6px; font-weight: 500; }
.period-value { font-size: 18px; font-weight: 700; color: #1f2937; margin-bottom: 4px; }
.period-ratio { font-size: 12px; color: #9ca3af; }
</style>
