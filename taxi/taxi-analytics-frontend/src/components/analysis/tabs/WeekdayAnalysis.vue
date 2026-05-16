<template>
  <div class="weekday-analysis">
    <div class="panel-header">
      <h3 class="panel-title">星期分析</h3>
      <p class="panel-desc">基于analysis_weekday_analysis表的周一至周日订单分布，识别工作日与周末差异</p>
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
            <div class="summary-label">周订单总数</div>
            <div class="summary-value">{{ formatNumber(weekTotalTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator daily"></div>
          <div class="card-body">
            <div class="summary-label">日均订单</div>
            <div class="summary-value">{{ formatNumber(dailyAvgTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator weekend"></div>
          <div class="card-body">
            <div class="summary-label">周末增幅</div>
            <div class="summary-value" :class="weekendGrowth >= 0 ? 'positive' : 'negative'">
              {{ weekendGrowth >= 0 ? '+' : '' }}{{ weekendGrowth.toFixed(1) }}%
            </div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator top"></div>
          <div class="card-body">
            <div class="summary-label">最高单日</div>
            <div class="summary-value">{{ topDay }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="radarChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="groupedBarRef" class="chart-area"></div>
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

const radarChartRef = ref<HTMLElement | null>(null)
const groupedBarRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null
let groupedBar: echarts.ECharts | null = null

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const processedData = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => ({
    dayOfWeek: i,
    dayName: weekDays[i],
    totalTrips: 0,
    totalRevenue: 0,
    avgFare: 0,
    avgDistance: 0
  }))
  props.data.forEach(d => {
    const dow = d.day_of_week ?? d.dayOfWeek ?? d.day
    if (dow !== undefined && dow >= 1 && dow <= 7) {
      const dayIndex = dow - 1
      days[dayIndex].totalTrips += d.total_trips || d.trip_count || 0
      days[dayIndex].totalRevenue += d.total_revenue || 0
      days[dayIndex].avgFare = d.avg_fare || d.avgFare || 0
      days[dayIndex].avgDistance = d.avg_distance || d.avgDistance || 0
    }
  })
  return days
})

const weekTotalTrips = computed(() => processedData.value.reduce((s, d) => s + d.totalTrips, 0))
const dailyAvgTrips = computed(() => weekTotalTrips.value > 0 ? Math.round(weekTotalTrips.value / 7) : 0)

const weekdayTrips = computed(() => processedData.value.slice(0, 5).reduce((s, d) => s + d.totalTrips, 0))
const weekendTrips = computed(() => processedData.value.slice(5, 7).reduce((s, d) => s + d.totalTrips, 0))
const weekdayAvg = computed(() => weekdayTrips.value / 5)
const weekendAvg = computed(() => weekendTrips.value / 2)
const weekendGrowth = computed(() => weekdayAvg.value > 0 ? ((weekendAvg.value - weekdayAvg.value) / weekdayAvg.value) * 100 : 0)

const topDay = computed(() => {
  const max = processedData.value.reduce((a, b) => a.totalTrips > b.totalTrips ? a : b, processedData.value[0])
  return max.dayName
})

const insights = computed(() => {
  const result: string[] = []
  if (weekendGrowth.value > 20) result.push('周末出行需求显著增加，可增加周末运力投放')
  else if (weekendGrowth.value < -10) result.push('周末出行需求下降，可优化周末定价策略')
  
  const friday = processedData.value[4]
  const saturday = processedData.value[5]
  if (friday.totalTrips > saturday.totalTrips * 1.3) result.push('周五订单量远超周六，可能是通勤需求主导')
  
  const max = processedData.value.reduce((a, b) => a.totalTrips > b.totalTrips ? a : b, processedData.value[0])
  result.push(`${max.dayName}订单量最高，占周总量${((max.totalTrips / weekTotalTrips.value) * 100).toFixed(1)}%`)
  
  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => { renderRadarChart(); renderGroupedBar() })
}

const renderRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarChartRef.value)

  const data = processedData.value
  const maxTrips = Math.max(...data.map(d => d.totalTrips), 1)
  const maxRevenue = Math.max(...data.map(d => d.totalRevenue), 1)
  const maxFare = Math.max(...data.map(d => d.avgFare), 1)
  const maxDist = Math.max(...data.map(d => d.avgDistance), 1)

  const option: echarts.EChartsOption = {
    title: { text: '星期多维度雷达图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: { backgroundColor: 'rgba(255,255,255,0.96)', borderColor: '#e5e7eb', borderWidth: 1, textStyle: { color: '#374151', fontSize: 12 } },
    legend: { orient: 'horizontal', top: 35, textStyle: { fontSize: 12, color: '#6b7280' }, data: ['工作日', '周末'] },
    radar: {
      center: ['50%', '58%'],
      radius: '65%',
      indicator: [
        { name: '订单量', max: 100 },
        { name: '总收入', max: 100 },
        { name: '平均车费', max: 100 },
        { name: '平均距离', max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#4b5563', fontSize: 12, fontWeight: 500 },
      splitArea: { areaStyle: { color: ['rgba(99,102,241,0.02)', 'rgba(99,102,241,0.04)', 'rgba(99,102,241,0.06)', 'rgba(99,102,241,0.08)'] } },
      splitLine: { lineStyle: { color: '#e5e7eb' } },
      axisLine: { lineStyle: { color: '#d1d5db' } }
    },
    series: [{
      type: 'radar',
      symbolSize: 5,
      data: [
        {
          value: [
            (weekdayAvg.value / maxTrips) * 100,
            (processedData.value.slice(0, 5).reduce((s, d) => s + d.totalRevenue, 0) / 5 / maxRevenue) * 100,
            (processedData.value.slice(0, 5).reduce((s, d) => s + d.avgFare, 0) / 5 / maxFare) * 100,
            (processedData.value.slice(0, 5).reduce((s, d) => s + d.avgDistance, 0) / 5 / maxDist) * 100
          ],
          name: '工作日',
          lineStyle: { color: '#6366f1', width: 2 },
          areaStyle: { color: 'rgba(99,102,241,0.2)' },
          itemStyle: { color: '#6366f1' }
        },
        {
          value: [
            (weekendAvg.value / maxTrips) * 100,
            (processedData.value.slice(5, 7).reduce((s, d) => s + d.totalRevenue, 0) / 2 / maxRevenue) * 100,
            (processedData.value.slice(5, 7).reduce((s, d) => s + d.avgFare, 0) / 2 / maxFare) * 100,
            (processedData.value.slice(5, 7).reduce((s, d) => s + d.avgDistance, 0) / 2 / maxDist) * 100
          ],
          name: '周末',
          lineStyle: { color: '#10b981', width: 2 },
          areaStyle: { color: 'rgba(16,185,129,0.2)' },
          itemStyle: { color: '#10b981' }
        }
      ],
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  radarChart.setOption(option)
}

const renderGroupedBar = () => {
  if (!groupedBarRef.value) return
  groupedBar?.dispose()
  groupedBar = echarts.init(groupedBarRef.value)

  const data = processedData.value

  const option: echarts.EChartsOption = {
    title: { text: '星期分组柱状图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const d = data[params[0].dataIndex]
        return `<div style="padding:8px"><strong>${d.dayName}</strong><br/>
          订单数: <strong>${d.totalTrips.toLocaleString()}</strong><br/>
          总收入: <strong>$${formatNumber(d.totalRevenue, 2)}</strong><br/>
          平均车费: <strong>$${d.avgFare.toFixed(2)}</strong></div>`
      }
    },
    legend: { orient: 'horizontal', top: 35, textStyle: { fontSize: 12, color: '#6b7280' }, data: ['订单数', '总收入'] },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '20%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.dayName),
      axisLabel: { fontSize: 12, color: '#6b7280' },
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
        name: '收入($)',
        nameTextStyle: { fontSize: 12, color: '#9ca3af' },
        axisLabel: { fontSize: 12, color: '#6b7280', formatter: (v: number) => v >= 1000 ? '$' + (v / 1000).toFixed(0) + 'k' : '$' + v.toFixed(0) },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 0,
        barWidth: '35%',
        data: data.map((d, idx) => ({
          value: d.totalTrips,
          itemStyle: { color: idx >= 5 ? '#10b981' : '#6366f1', borderRadius: [4, 4, 0, 0] }
        })),
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      },
      {
        name: '总收入',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: '35%',
        data: data.map((d, idx) => ({
          value: d.totalRevenue,
          itemStyle: { color: idx >= 5 ? '#059669' : '#4f46e5', borderRadius: [4, 4, 0, 0] }
        })),
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }
  groupedBar.setOption(option)
}

const handleResize = () => { radarChart?.resize(); groupedBar?.resize() }

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); radarChart?.dispose(); groupedBar?.dispose() })
</script>

<style lang="scss" scoped>
.weekday-analysis { width: 100%; }
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
.card-indicator.daily { background: #3b82f6; }
.card-indicator.weekend { background: #10b981; }
.card-indicator.top { background: #f59e0b; }
.summary-card.highlight { border-color: #fcd34d; background: linear-gradient(135deg, #fefce8 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #d97706; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 6px; }
.summary-value { font-size: 22px; font-weight: 700; color: #1f2937; }
.summary-value.positive { color: #059669 !important; }
.summary-value.negative { color: #dc2626 !important; }
.chart-row { display: flex; gap: 24px; margin-bottom: 24px; }
.chart-container { flex: 1; background: #fafbfc; border-radius: 12px; padding: 16px; min-height: 360px; }
.chart-area { width: 100%; height: 340px; }
.insight-box { background: linear-gradient(135deg, #fefce8 0%, #fffbeb 100%); border: 1px solid #fde68a; border-radius: 12px; padding: 16px 20px; margin-bottom: 24px; }
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.insight-icon { font-size: 16px; }
.insight-title { font-size: 14px; font-weight: 600; color: #92400e; }
.insight-list { margin: 0; padding-left: 20px; }
.insight-list li { font-size: 13px; color: #78350f; line-height: 1.8; }
</style>
