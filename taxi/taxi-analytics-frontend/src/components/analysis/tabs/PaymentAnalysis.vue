<template>
  <div class="payment-analysis">
    <div class="panel-header">
      <h3 class="panel-title">支付方式分析</h3>
      <p class="panel-desc">基于analysis_payment_analysis表的支付渠道订单分布及金额瀑布流分析</p>
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
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ formatNumber(totalTrips) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator amount"></div>
          <div class="card-body">
            <div class="summary-label">总金额</div>
            <div class="summary-value">${{ formatNumber(totalAmount, 2) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator tip"></div>
          <div class="card-body">
            <div class="summary-label">平均小费金额</div>
            <div class="summary-value">${{ averageTip }}</div>
          </div>
        </div>
        <div class="summary-card highlight">
          <div class="card-indicator dominant"></div>
          <div class="card-body">
            <div class="summary-label">主流支付方式</div>
            <div class="summary-value">{{ dominantPayment }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="ringChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="waterfallChartRef" class="chart-area"></div>
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
              <th>支付方式</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总金额</th>
              <th>平均金额</th>
              <th>平均小费</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in processedData" :key="item.paymentType">
              <td>
                <span class="payment-badge" :class="item.paymentType">{{ item.paymentDesc }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ item.ratio.toFixed(1) }}%</td>
              <td>${{ formatNumber(item.totalAmount, 2) }}</td>
              <td>${{ formatNumber(item.avgAmount, 2) }}</td>
              <td>${{ formatNumber(item.avgTip, 2) }}</td>
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

const ringChartRef = ref<HTMLElement | null>(null)
const waterfallChartRef = ref<HTMLElement | null>(null)
let ringChart: echarts.ECharts | null = null
let waterfallChart: echarts.ECharts | null = null

const paymentTypeMap: Record<string, string> = {
  'credit_card': '信用卡', 'cash': '现金', 'no_charge': '免费',
  'dispute': '争议', 'unknown': '未知'
}

const rawData = computed(() => {
  return props.data.map((item: any) => ({
    paymentType: item.payment_type || item.payment_name || 'unknown',
    paymentDesc: item.payment_desc || paymentTypeMap[item.payment_type || item.payment_name] || item.payment_name || '未知',
    tripCount: item.trip_count || 0,
    totalAmount: item.total_amount || 0,
    avgAmount: item.avg_amount || (item.trip_count > 0 ? (item.total_amount || 0) / item.trip_count : 0),
    avgTip: item.avg_tip || 0,
    isCashless: item.is_cashless || 0
  })).sort((a: any, b: any) => b.tripCount - a.tripCount)
})

const totalTrips = computed(() => rawData.value.reduce((sum: number, d: any) => sum + d.tripCount, 0))

const processedData = computed(() => {
  const total = totalTrips.value
  return rawData.value.map((item: any) => ({ ...item, ratio: total > 0 ? (item.tripCount / total) * 100 : 0 }))
})

const totalAmount = computed(() => processedData.value.reduce((sum: number, d: any) => sum + d.totalAmount, 0))

const averageTip = computed(() => {
  let totalTip = 0, totalCount = 0
  processedData.value.forEach(d => {
    if (d.tripCount > 0 && d.avgTip > 0) { totalTip += d.avgTip * d.tripCount; totalCount += d.tripCount }
  })
  return totalCount > 0 ? (totalTip / totalCount).toFixed(2) : '0.00'
})

const dominantPayment = computed(() => {
  if (processedData.value.length === 0) return '-'
  return processedData.value[0].paymentDesc
})

const insights = computed(() => {
  const result: string[] = []
  const creditRatio = processedData.value.find((d: any) => d.paymentType === 'credit_card')?.ratio || 0
  if (creditRatio > 70) result.push('信用卡支付已为主流方式，用户支付习惯良好，适合推广无现金服务')
  else if (creditRatio < 50) result.push('现金支付仍占较大比例，可考虑提升电子支付场景覆盖率')

  const cashRatio = processedData.value.find((d: any) => d.paymentType === 'cash')?.ratio || 0
  if (cashRatio > 40) result.push('现金支付比例较高，需关注现金管理成本和找零效率')

  const unknownRatio = processedData.value.find((d: any) => d.paymentType === 'unknown')?.ratio || 0
  if (unknownRatio > 5) result.push('存在未知支付方式，建议完善数据采集流程，提高数据质量')

  const cashlessData = processedData.value.filter((d: any) => d.isCashless === 1)
  if (cashlessData.length > 0) {
    const cashlessRatio = cashlessData.reduce((s: number, d: any) => s + d.ratio, 0)
    result.push(`无现金支付占比${cashlessRatio.toFixed(1)}%，数字化支付趋势${cashlessRatio > 60 ? '明显' : '待提升'}`)
  }

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => { renderRingChart(); renderWaterfallChart() })
}

const renderRingChart = () => {
  if (!ringChartRef.value) return
  ringChart?.dispose()
  ringChart = echarts.init(ringChartRef.value)

  const ringColors = ['#3b82f6', '#10b981', '#6b7280', '#ef4444', '#9ca3af']

  const option: echarts.EChartsOption = {
    title: { text: '支付方式环形分布', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const d = processedData.value[params.dataIndex]
        return `<div style="padding:8px;font-weight:600;margin-bottom:6px">${params.name}</div>
          <div style="font-size:12px">订单数: <strong>${d.tripCount.toLocaleString()}</strong> (${params.percent}%)</div>
          <div style="font-size:12px">总金额: <strong>$${formatNumber(d.totalAmount, 2)}</strong></div>
          <div style="font-size:12px">平均金额: <strong>$${d.avgAmount.toFixed(2)}</strong></div>
          <div style="font-size:12px">平均小费: <strong>$${d.avgTip.toFixed(2)}</strong></div>`
      }
    },
    legend: { orient: 'horizontal', top: 35, textStyle: { fontSize: 12, color: '#6b7280' }, itemGap: 16 },
    series: [{
      name: '支付方式',
      type: 'pie',
      radius: ['40%', '72%'],
      center: ['50%', '56%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 10, borderColor: '#ffffff', borderWidth: 3, shadowBlur: 6, shadowColor: 'rgba(0,0,0,0.06)' },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, fontWeight: 500, color: '#4b5563', lineHeight: 16 },
      labelLine: { length: 12, length2: 8, smooth: true, lineStyle: { color: '#d1d5db' } },
      emphasis: { label: { fontSize: 13, fontWeight: 600 }, itemStyle: { shadowBlur: 15, shadowColor: 'rgba(0,0,0,0.15)' } },
      data: processedData.value.map((d: any, idx: number) => ({
        value: d.tripCount,
        name: d.paymentDesc,
        itemStyle: { color: ringColors[idx % ringColors.length] }
      })),
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }
  ringChart.setOption(option)
}

const renderWaterfallChart = () => {
  if (!waterfallChartRef.value) return
  waterfallChart?.dispose()
  waterfallChart = echarts.init(waterfallChartRef.value)

  const data = processedData.value
  const amounts = data.map((d: any) => d.totalAmount)
  const total = amounts.reduce((s: number, v: number) => s + v, 0)

  const helperData = [0]
  for (let i = 1; i < amounts.length; i++) {
    helperData.push(helperData[i - 1] + amounts[i - 1])
  }

  const waterfallColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#6b7280']

  const option: echarts.EChartsOption = {
    title: { text: '支付金额瀑布流', left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const idx = params[0].dataIndex
        const d = data[idx]
        if (!d) return ''
        return `<div style="padding:8px;font-weight:600;margin-bottom:6px">${d.paymentDesc}</div>
          <div style="font-size:12px">金额: <strong>$${formatNumber(d.totalAmount, 2)}</strong></div>
          <div style="font-size:12px">占比: <strong>${d.ratio.toFixed(1)}%</strong></div>
          <div style="font-size:12px">订单数: <strong>${d.tripCount.toLocaleString()}</strong></div>`
      }
    },
    grid: { left: '5%', right: '5%', bottom: '8%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: [...data.map((d: any) => d.paymentDesc), '合计'],
      axisLabel: { fontSize: 12, color: '#6b7280', rotate: data.length > 4 ? 20 : 0 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '金额($)',
      nameTextStyle: { fontSize: 12, color: '#9ca3af' },
      axisLabel: {
        fontSize: 12, color: '#6b7280',
        formatter: (v: number) => {
          if (v >= 1000000) return '$' + (v / 1000000).toFixed(1) + 'M'
          if (v >= 1000) return '$' + (v / 1000).toFixed(0) + 'k'
          return '$' + v.toFixed(0)
        }
      },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
    },
    series: [
      {
        name: '辅助',
        type: 'bar',
        stack: 'total',
        barWidth: '50%',
        itemStyle: { color: 'transparent' },
        data: [...helperData, 0],
        emphasis: { itemStyle: { color: 'transparent' } }
      },
      {
        name: '金额',
        type: 'bar',
        stack: 'total',
        barWidth: '50%',
        itemStyle: { borderRadius: [6, 6, 0, 0] },
        data: [
          ...data.map((d: any, idx: number) => ({
            value: d.totalAmount,
            itemStyle: { color: waterfallColors[idx % waterfallColors.length] }
          })),
          {
            value: total,
            itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#6366f1' }, { offset: 1, color: '#4f46e5' }
            ]) }
          }
        ],
        label: {
          show: true,
          position: 'top',
          fontSize: 11,
          fontWeight: 500,
          color: '#4b5563',
          formatter: (params: any) => {
            const v = params.value
            if (v >= 1000000) return '$' + (v / 1000000).toFixed(1) + 'M'
            if (v >= 1000) return '$' + (v / 1000).toFixed(0) + 'k'
            return '$' + v.toFixed(0)
          }
        },
        animationDuration: 1500,
        animationEasing: 'cubicOut'
      }
    ]
  }
  waterfallChart.setOption(option)
}

const handleResize = () => { ringChart?.resize(); waterfallChart?.resize() }

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => { window.addEventListener('resize', handleResize); if (!props.loading) nextTick(() => renderCharts()) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); ringChart?.dispose(); waterfallChart?.dispose() })
</script>

<style lang="scss" scoped>
.payment-analysis { width: 100%; }

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
.card-indicator.amount { background: #10b981; }
.card-indicator.tip { background: #f59e0b; }
.card-indicator.dominant { background: #6366f1; }
.summary-card.highlight { border-color: #a5b4fc; background: linear-gradient(135deg, #eef2ff 0%, #ffffff 100%); }
.summary-card.highlight .card-indicator { background: #4f46e5; }
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

.payment-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; }
.payment-badge.credit_card { background: #dbeafe; color: #2563eb; }
.payment-badge.cash { background: #d1fae5; color: #059669; }
.payment-badge.no_charge { background: #f3f4f6; color: #6b7280; }
.payment-badge.dispute { background: #fee2e2; color: #dc2626; }
.payment-badge.unknown { background: #f3f4f6; color: #9ca3af; }
</style>
