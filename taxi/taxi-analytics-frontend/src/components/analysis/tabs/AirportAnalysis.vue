<template>
  <div class="airport-analysis">
    <div class="panel-header">
      <h3 class="panel-title">机场运营分析</h3>
      <p class="panel-desc">基于analysis_airport表的三大机场（肯尼迪JFK、拉瓜迪亚LGA、纽瓦克EWR）出行订单分析</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div class="summary-cards">
        <div class="summary-card highlight">
          <div class="card-indicator"></div>
          <div class="card-body">
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ formatNumber(totalTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator airport"></div>
          <div class="card-body">
            <div class="summary-label">机场出行订单</div>
            <div class="summary-value">{{ formatNumber(airportTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator non-airport"></div>
          <div class="card-body">
            <div class="summary-label">非机场出行订单</div>
            <div class="summary-value">{{ formatNumber(nonAirportTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator ratio"></div>
          <div class="card-body">
            <div class="summary-label">机场出行占比</div>
            <div class="summary-value">{{ airportRatio }}%</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="roseChartRef" class="chart-area"></div>
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

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>出行类型</th>
              <th>订单数</th>
              <th>占比</th>
              <th>平均金额</th>
              <th>平均距离(mi)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in processedData" :key="item.type">
              <td>
                <span class="type-badge" :class="item.type">{{ item.label }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ item.ratio }}%</td>
              <td>${{ formatNumber(item.avgAmount, 2) }}</td>
              <td>{{ formatNumber(item.avgDistance, 1) }}</td>
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
  data: any[]
  loading: boolean
}>()

const roseChartRef = ref<HTMLElement | null>(null)
const gaugeChartRef = ref<HTMLElement | null>(null)
let roseChart: echarts.ECharts | null = null
let gaugeChart: echarts.ECharts | null = null

const airportDetailData = computed(() => {
  const airportCodes = ['JFK', 'LaGuardia', 'EWR']
  return props.data
    .filter(d => airportCodes.includes(d.airport_code || ''))
    .map(item => ({
      name: item.airport_name || item.airport_code,
      tripCount: item.trip_count || 0,
      totalAmount: item.total_amount || 0,
      avgDistance: item.avg_distance || 0,
      percentage: item.percentage || 0
    }))
    .sort((a, b) => b.tripCount - a.tripCount)
})

const processedData = computed(() => {
  const airportData = props.data.find(d => d.airport_code === 'airport')
  const nonAirportData = props.data.find(d => d.airport_code === 'non_airport')
  const airportTripCount = airportData?.trip_count || 0
  const nonAirportTripCount = nonAirportData?.trip_count || 0
  const total = airportTripCount + nonAirportTripCount

  return [
    {
      type: 'airport',
      label: '机场出行',
      tripCount: airportTripCount,
      ratio: total > 0 ? ((airportTripCount / total) * 100).toFixed(1) : '0',
      avgAmount: airportData?.total_amount && airportTripCount > 0 ? airportData.total_amount / airportTripCount : 0,
      avgDistance: airportData?.avg_distance || 0
    },
    {
      type: 'non_airport',
      label: '非机场出行',
      tripCount: nonAirportTripCount,
      ratio: total > 0 ? ((nonAirportTripCount / total) * 100).toFixed(1) : '0',
      avgAmount: nonAirportData?.total_amount && nonAirportTripCount > 0 ? nonAirportData.total_amount / nonAirportTripCount : 0,
      avgDistance: nonAirportData?.avg_distance || 0
    }
  ]
})

const totalTrips = computed(() => props.data.reduce((sum, d) => sum + (d.trip_count || 0), 0))
const airportTrips = computed(() => {
  const airport = props.data.find(d => d.airport_code === 'airport')
  return airport?.trip_count || 0
})
const nonAirportTrips = computed(() => totalTrips.value - airportTrips.value)
const airportRatio = computed(() => {
  if (totalTrips.value === 0) return '0'
  return ((airportTrips.value / totalTrips.value) * 100).toFixed(1)
})

const insights = computed(() => {
  const result: string[] = []
  const ratio = parseFloat(airportRatio.value)
  if (ratio > 30) result.push('机场出行订单占比较高，商务出行需求旺盛')
  else if (ratio < 15) result.push('本地出行占主导，短途订单为主')

  const airport = processedData.value.find(p => p.type === 'airport')
  const nonAirport = processedData.value.find(p => p.type === 'non_airport')
  if (airport && nonAirport) {
    if (airport.avgAmount > nonAirport.avgAmount * 1.5) result.push('机场出行平均金额明显高于非机场出行，长途商务客群价值较高')
    if (airport.avgDistance > nonAirport.avgDistance * 2) result.push('机场出行平均距离较长，适合投放远途优惠活动')
  }

  if (airportDetailData.value.length > 0) {
    const top = airportDetailData.value[0]
    result.push(`${top.name}订单量最高，占机场出行${top.percentage.toFixed(1)}%`)
  }

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => { renderRoseChart(); renderGaugeChart() })
}

const renderRoseChart = () => {
  if (!roseChartRef.value) return
  roseChart?.dispose()
  roseChart = echarts.init(roseChartRef.value)

  const allData = [
    ...airportDetailData.value.map(d => ({ name: d.name, value: d.tripCount, category: 'airport' })),
    ...processedData.value.map(d => ({ name: d.label, value: d.tripCount, category: d.type }))
  ]

  const airportColors = ['#3b82f6', '#10b981', '#f59e0b']
  const categoryColors: Record<string, string> = { airport: '#6366f1', non_airport: '#94a3b8' }

  const option: echarts.EChartsOption = {
    title: { text: '机场订单南丁格尔玫瑰图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const detail = airportDetailData.value.find(d => d.name === params.name)
        const summary = processedData.value.find(d => d.label === params.name)
        let html = `<div style="padding:8px;font-weight:600;margin-bottom:6px">${params.name}</div>`
        html += `<div style="font-size:12px">订单数: <strong>${params.value.toLocaleString()}</strong> (${params.percent}%)</div>`
        if (detail) {
          html += `<div style="font-size:12px">总金额: <strong>$${detail.totalAmount.toLocaleString()}</strong></div>`
          html += `<div style="font-size:12px">平均距离: <strong>${detail.avgDistance.toFixed(1)} mi</strong></div>`
        }
        if (summary) {
          html += `<div style="font-size:12px">平均金额: <strong>$${summary.avgAmount.toFixed(2)}</strong></div>`
        }
        return html
      }
    },
    legend: { orient: 'horizontal', top: 35, textStyle: { fontSize: 12, color: '#6b7280' }, itemGap: 16 },
    series: [{
      name: '机场订单分布',
      type: 'pie',
      roseType: 'area',
      radius: ['20%', '70%'],
      center: ['50%', '58%'],
      itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 2, shadowBlur: 6, shadowColor: 'rgba(0,0,0,0.08)' },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, fontWeight: 500, color: '#4b5563', lineHeight: 16 },
      labelLine: { length: 12, length2: 8, smooth: true, lineStyle: { color: '#d1d5db' } },
      emphasis: { label: { fontSize: 13, fontWeight: 600 }, itemStyle: { shadowBlur: 15, shadowColor: 'rgba(0,0,0,0.15)' } },
      data: allData.map((d, idx) => ({
        value: d.value,
        name: d.name,
        itemStyle: {
          color: d.category === 'airport' && idx < airportColors.length
            ? new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: airportColors[idx].replace(')', ',0.6)').replace('rgb', 'rgba') },
                { offset: 1, color: airportColors[idx] }
              ])
            : categoryColors[d.category] || '#94a3b8'
        }
      })),
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  roseChart.setOption(option)
}

const renderGaugeChart = () => {
  if (!gaugeChartRef.value) return
  gaugeChart?.dispose()
  gaugeChart = echarts.init(gaugeChartRef.value)

  const ratio = parseFloat(airportRatio.value)

  const option: echarts.EChartsOption = {
    title: { text: '机场出行占比', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    series: [{
      type: 'gauge',
      center: ['50%', '60%'],
      radius: '80%',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 10,
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#60a5fa' }, { offset: 0.5, color: '#3b82f6' }, { offset: 1, color: '#1d4ed8' }]) },
      progress: { show: true, width: 20, roundCap: true },
      pointer: { show: true, length: '55%', width: 5, itemStyle: { color: '#3b82f6' } },
      axisLine: { lineStyle: { width: 20, color: [[1, '#e5e7eb']] }, roundCap: true },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { show: true, offsetCenter: [0, '70%'], fontSize: 13, color: '#6b7280' },
      detail: {
        valueAnimation: true,
        formatter: `{value}%`,
        fontSize: 28,
        fontWeight: 700,
        color: '#1d4ed8',
        offsetCenter: [0, '35%']
      },
      data: [{ value: ratio, name: '机场出行占比' }],
      animationDuration: 2000,
      animationEasing: 'cubicOut'
    }]
  }
  gaugeChart.setOption(option)
}

const handleResize = () => { roseChart?.resize(); gaugeChart?.resize() }

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); roseChart?.dispose(); gaugeChart?.dispose() })
</script>

<style lang="scss" scoped>
.airport-analysis { width: 100%; }

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
.card-indicator.airport { background: #6366f1; }
.card-indicator.non-airport { background: #10b981; }
.card-indicator.ratio { background: #f59e0b; }
.summary-card.highlight { border-color: #93c5fd; background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #2563eb; }
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

.data-table-wrapper { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { background: #f8fafc; padding: 12px 16px; text-align: left; font-weight: 600; color: #374151; border-bottom: 2px solid #e5e7eb; }
.data-table td { padding: 10px 16px; border-bottom: 1px solid #f3f4f6; color: #4b5563; }
.data-table tr:hover td { background: #f8fafc; }

.type-badge { display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; }
.type-badge.airport { background: #ede9fe; color: #6d28d9; }
.type-badge.non_airport { background: #d1fae5; color: #065f46; }
</style>
