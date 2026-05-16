<template>
  <div class="vendor-analysis">
    <div class="panel-header">
      <h3 class="panel-title">供应商绩效分析</h3>
      <p class="panel-desc">基于analysis_vendor表的供应商市场份额、订单量及收入贡献多维度对比分析</p>
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
            <div class="summary-label">供应商总数</div>
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
          <div class="card-indicator cr4"></div>
          <div class="card-body">
            <div class="summary-label">CR4市场集中度</div>
            <div class="summary-value">{{ cr4Ratio }}%</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="funnelChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="radarChartRef" class="chart-area"></div>
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
              <th>供应商名称</th>
              <th>订单数</th>
              <th>市场份额</th>
              <th>总收入</th>
              <th>平均订单金额</th>
              <th>平均里程(mi)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in processedData" :key="item.vendorId">
              <td>
                <span class="vendor-name">{{ formatVendorName(item.vendorName) }}</span>
                <span class="vendor-id">{{ item.vendorId }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>
                <div class="share-bar">
                  <div class="share-fill" :style="{ width: item.marketShare + '%' }"></div>
                  <span class="share-text">{{ item.marketShare.toFixed(1) }}%</span>
                </div>
              </td>
              <td>${{ formatNumber(item.totalRevenue, 2) }}</td>
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

const funnelChartRef = ref<HTMLElement | null>(null)
const radarChartRef = ref<HTMLElement | null>(null)
let funnelChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null

const processedData = computed(() => {
  return props.data.map(item => ({
    vendorId: item.vendor_id || item.vendorId || '-',
    vendorName: item.vendor_name || item.vendorName || '-',
    tripCount: item.trip_count || 0,
    marketShare: item.market_share || item.revenue_ratio || 0,
    totalRevenue: item.total_revenue || item.total_amount || item.totalRevenue || 0,
    avgAmount: item.avg_fare || (item.trip_count > 0 ? (item.total_revenue || item.total_amount || 0) / item.trip_count : 0),
    avgDistance: item.avg_distance || item.avg_trip_distance || item.avgDistance || 0
  })).sort((a, b) => b.tripCount - a.tripCount)
})

const totalTrips = computed(() => processedData.value.reduce((sum, d) => sum + d.tripCount, 0))
const totalRevenue = computed(() => processedData.value.reduce((sum, d) => sum + d.totalRevenue, 0))

const cr4Ratio = computed(() => {
  const sorted = [...processedData.value].sort((a, b) => b.marketShare - a.marketShare)
  const top4 = sorted.slice(0, 4)
  return top4.reduce((s, d) => s + d.marketShare, 0).toFixed(1)
})

const insights = computed(() => {
  const result: string[] = []
  const sorted = [...processedData.value].sort((a, b) => b.marketShare - a.marketShare)
  if (sorted.length > 0) {
    const topVendor = sorted[0]
    if (topVendor.marketShare > 50) result.push(`市场高度集中，${formatVendorName(topVendor.vendorName)}占据${topVendor.marketShare.toFixed(1)}%市场份额，存在垄断风险`)
    else if (topVendor.marketShare < 20) result.push('市场竞争激烈，各供应商市场份额相对分散，无明显垄断者')
  }
  if (parseFloat(cr4Ratio.value) > 80) result.push('CR4指数超过80%，市场高度集中，建议关注反垄断合规')
  else if (parseFloat(cr4Ratio.value) < 40) result.push('CR4指数低于40%，市场分散度高，有利于新进入者')

  if (sorted.length >= 2) {
    const ratio = sorted[0].avgAmount / Math.max(sorted[sorted.length - 1].avgAmount, 0.01)
    if (ratio > 1.5) result.push(`头部供应商平均订单金额是末尾的${ratio.toFixed(1)}倍，定价策略差异明显`)
  }

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const formatVendorName = (name: string): string => {
  if (!name) return ''
  return name.replace(/^[\d\s]+/, '').trim() || name
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => { renderFunnelChart(); renderRadarChart() })
}

const renderFunnelChart = () => {
  if (!funnelChartRef.value) return
  funnelChart?.dispose()
  funnelChart = echarts.init(funnelChartRef.value)

  const sorted = [...processedData.value].sort((a, b) => b.tripCount - a.tripCount)
  const funnelColors = ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444']

  const option: echarts.EChartsOption = {
    title: { text: '供应商订单漏斗图', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const d = sorted[params.dataIndex]
        if (!d) return ''
        return `<div style="padding:8px;font-weight:600;margin-bottom:6px">${params.name}</div>
          <div style="font-size:12px">订单数: <strong>${d.tripCount.toLocaleString()}</strong></div>
          <div style="font-size:12px">市场份额: <strong>${d.marketShare.toFixed(1)}%</strong></div>
          <div style="font-size:12px">总收入: <strong>$${formatNumber(d.totalRevenue, 2)}</strong></div>
          <div style="font-size:12px">平均金额: <strong>$${d.avgAmount.toFixed(2)}</strong></div>`
      }
    },
    legend: { orient: 'horizontal', top: 35, textStyle: { fontSize: 12, color: '#6b7280' }, itemGap: 16 },
    series: [{
      name: '供应商订单',
      type: 'funnel',
      left: '10%',
      top: 70,
      bottom: 20,
      width: '80%',
      min: 0,
      max: sorted.length > 0 ? sorted[0].tripCount : 100,
      minSize: '20%',
      maxSize: '100%',
      sort: 'descending',
      gap: 4,
      label: {
        show: true,
        position: 'inside',
        formatter: (params: any) => {
          const d = sorted[params.dataIndex]
          return d ? `${params.name}\n${d.tripCount.toLocaleString()}单` : ''
        },
        fontSize: 12,
        fontWeight: 500,
        color: '#ffffff',
        lineHeight: 18
      },
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 2,
        shadowBlur: 6,
        shadowColor: 'rgba(0,0,0,0.08)'
      },
      emphasis: {
        label: { fontSize: 14, fontWeight: 600 },
        itemStyle: { shadowBlur: 15, shadowColor: 'rgba(0,0,0,0.15)' }
      },
      data: sorted.map((d, idx) => ({
        value: d.tripCount,
        name: formatVendorName(d.vendorName),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: funnelColors[idx % funnelColors.length] + 'cc' },
            { offset: 1, color: funnelColors[idx % funnelColors.length] }
          ])
        }
      })),
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  funnelChart.setOption(option)
}

const renderRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarChartRef.value)

  const data = processedData.value
  if (data.length === 0) return

  const maxTrips = Math.max(...data.map(d => d.tripCount), 1)
  const maxRevenue = Math.max(...data.map(d => d.totalRevenue), 1)
  const maxAvgFare = Math.max(...data.map(d => d.avgAmount), 1)
  const maxAvgDist = Math.max(...data.map(d => d.avgDistance), 1)
  const maxShare = Math.max(...data.map(d => d.marketShare), 1)

  const radarColors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#3b82f6']

  const option: echarts.EChartsOption = {
    title: { text: '供应商多维度雷达对比', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 }
    },
    legend: {
      orient: 'horizontal',
      top: 35,
      textStyle: { fontSize: 12, color: '#6b7280' },
      itemGap: 16,
      data: data.map(d => formatVendorName(d.vendorName))
    },
    radar: {
      center: ['50%', '58%'],
      radius: '65%',
      indicator: [
        { name: '订单量', max: 100 },
        { name: '总收入', max: 100 },
        { name: '平均金额', max: 100 },
        { name: '平均距离', max: 100 },
        { name: '市场份额', max: 100 }
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
      data: data.map((d, idx) => ({
        value: [
          (d.tripCount / maxTrips) * 100,
          (d.totalRevenue / maxRevenue) * 100,
          (d.avgAmount / maxAvgFare) * 100,
          (d.avgDistance / maxAvgDist) * 100,
          (d.marketShare / maxShare) * 100
        ],
        name: formatVendorName(d.vendorName),
        lineStyle: { color: radarColors[idx % radarColors.length], width: 2 },
        areaStyle: { color: radarColors[idx % radarColors.length] + '20' },
        itemStyle: { color: radarColors[idx % radarColors.length] }
      })),
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  radarChart.setOption(option)
}

const handleResize = () => { funnelChart?.resize(); radarChart?.resize() }

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); funnelChart?.dispose(); radarChart?.dispose() })
</script>

<style lang="scss" scoped>
.vendor-analysis { width: 100%; }

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
.card-indicator.trips { background: #3b82f6; }
.card-indicator.revenue { background: #10b981; }
.card-indicator.cr4 { background: #f59e0b; }
.summary-card.highlight { border-color: #c4b5fd; background: linear-gradient(135deg, #ede9fe 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #7c3aed; }
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
.data-table th { background: #f9fafb; padding: 14px 16px; text-align: left; font-size: 13px; font-weight: 600; color: #374151; border-bottom: 2px solid #e5e7eb; }
.data-table td { padding: 12px 16px; font-size: 13px; color: #4b5563; border-bottom: 1px solid #f3f4f6; }
.data-table tbody tr:hover { background: #f9fafb; }
.vendor-name { font-weight: 500; color: #1f2937; }
.vendor-id { display: block; font-size: 12px; color: #9ca3af; margin-top: 2px; }
.share-bar { position: relative; height: 20px; background: #e5e7eb; border-radius: 10px; overflow: hidden; min-width: 100px; }
.share-fill { position: absolute; left: 0; top: 0; height: 100%; background: linear-gradient(90deg, #8b5cf6, #6366f1); border-radius: 10px; }
.share-text { position: relative; z-index: 1; padding-left: 8px; line-height: 20px; font-size: 12px; font-weight: 500; color: #4b5563; }
</style>
