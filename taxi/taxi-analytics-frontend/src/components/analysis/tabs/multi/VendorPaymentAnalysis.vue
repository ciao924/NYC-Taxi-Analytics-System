<template>
  <div class="vendor-payment-analysis">
    <div class="panel-header">
      <h3 class="panel-title">供应商 × 支付方式 交叉分析</h3>
      <p class="panel-desc">分析不同供应商在各支付方式上的订单分布与收入差异</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div class="summary-cards">
        <div class="summary-card highlight">
          <div class="summary-label">总组合数</div>
          <div class="summary-value">{{ formatNumber(totalCombinations) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">总订单数</div>
          <div class="summary-value">{{ formatNumber(totalTrips) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">总金额</div>
          <div class="summary-value">${{ formatNumber(totalAmount, 2) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">平均金额</div>
          <div class="summary-value">${{ formatNumber(avgAmount, 2) }}</div>
        </div>
      </div>

      <div class="insights-section" v-if="insights.length > 0">
        <div v-for="(insight, index) in insights" :key="index" class="insight-item" :class="insight.type">
          <div class="insight-icon">{{ insight.icon }}</div>
          <div class="insight-content">
            <div class="insight-title">{{ insight.title }}</div>
            <div class="insight-description">{{ insight.description }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container half">
          <div ref="barChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container half">
          <div ref="pieChartRef" class="chart-area"></div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>供应商</th>
              <th>支付方式</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总金额</th>
              <th>平均金额</th>
              <th>小费合计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
              <td class="vendor-cell">
                <span class="vendor-badge">{{ item.vendorName || item.dimension1Name || 'Unknown' }}</span>
              </td>
              <td class="payment-cell">
                <span class="payment-badge" :class="getPaymentClass(item.paymentType || item.dimension2Name)">
                  {{ item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || 'Unknown' }}
                </span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.avgAmount || item.avgFare, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.totalTip || 0, 2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  data: any[]
  loading: boolean
}>()

const barChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const tableData = computed(() => {
  return props.data.slice(0, 20)
})

const totalCombinations = computed(() => props.data.length)

const totalTrips = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const totalAmount = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
})

const avgAmount = computed(() => {
  if (totalTrips.value === 0) return 0
  return totalAmount.value / totalTrips.value
})

const insights = computed(() => {
  if (props.data.length === 0) return []

  const sortedByTrips = [...props.data].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  const sortedByAmount = [...props.data].sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0))

  const topTrip = sortedByTrips[0]
  const topAmount = sortedByAmount[0]

  const result = []

  if (topTrip) {
    result.push({
      icon: '📊',
      type: 'positive',
      title: '订单量最高组合',
      description: `${topTrip.vendorName || topTrip.dimension1Name || 'Unknown'} × ${topTrip.paymentType || topTrip.dimension2Name || topTrip.paymentDesc || topTrip.dimension2 || 'Unknown'}，订单数 ${formatNumber(topTrip.tripCount)} 单`
    })
  }

  if (topAmount && topAmount !== topTrip) {
    result.push({
      icon: '💰',
      type: 'highlight',
      title: '收入最高组合',
      description: `${topAmount.vendorName || topAmount.dimension1Name || 'Unknown'} × ${topAmount.paymentType || topAmount.dimension2Name || topAmount.paymentDesc || topAmount.dimension2 || 'Unknown'}，收入 $${formatNumber(topAmount.totalAmount || topAmount.totalRevenue, 2)}`
    })
  }

  const cashPayments = props.data.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
  )
  if (cashPayments.length > 0) {
    const cashTrips = cashPayments.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cashPercentage = (cashTrips / totalTrips.value * 100).toFixed(1)
    result.push({
      icon: '💵',
      type: 'info',
      title: '现金支付分析',
      description: `现金支付组合 ${cashPayments.length} 种，合计订单 ${formatNumber(cashTrips)} 单（占比 ${cashPercentage}%）`
    })
  }

  return result
})

const getPaymentClass = (payment: string): string => {
  if (!payment) return ''
  const p = payment.toLowerCase()
  if (p.includes('card') || p.includes('credit')) return 'card'
  if (p.includes('cash')) return 'cash'
  if (p.includes('app') || p.includes('mobile')) return 'app'
  return 'default'
}

const renderCharts = () => {
  renderBarChart()
  renderPieChart()
}

const renderBarChart = () => {
  if (!barChartRef.value || props.data.length === 0) return

  barChart?.dispose()
  barChart = echarts.init(barChartRef.value)

  const topData = props.data.slice(0, 10)
  const labels = topData.map(item =>
    `${(item.vendorName || item.dimension1Name || 'N/A').substring(0, 6)}×${(item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || 'N/A').substring(0, 4)}`
  )

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const idx = params[0].dataIndex
        const item = topData[idx]
        return `<strong>${item.vendorName || item.dimension1Name} × ${item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2}</strong><br/>
                订单数: ${formatNumber(item.tripCount)}<br/>
                总金额: $${formatNumber(item.totalAmount || item.totalRevenue, 2)}<br/>
                占比: ${formatNumber(item.percentage, 1)}%`
      }
    },
    legend: { data: ['订单数', '金额(千$)'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { rotate: 30, interval: 0 }
    },
    yAxis: [
      { type: 'value', name: '订单数', position: 'left' },
      { type: 'value', name: '金额(千$)', position: 'right' }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: topData.map(item => item.tripCount),
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '金额(千$)',
        type: 'bar',
        yAxisIndex: 1,
        data: topData.map(item => ((item.totalAmount || item.totalRevenue || 0) / 1000).toFixed(2)),
        itemStyle: { color: '#10b981' }
      }
    ]
  }

  barChart.setOption(option)
}

const renderPieChart = () => {
  if (!pieChartRef.value || props.data.length === 0) return

  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const topData = props.data.slice(0, 8)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `<strong>${params.name}</strong><br/>
                订单数: ${formatNumber(params.value)}<br/>
                占比: ${params.percent.toFixed(1)}%`
      }
    },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {d}%'
      },
      data: topData.map((item, idx) => ({
        name: `${item.vendorName || item.dimension1Name || 'N/A'}×${item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || 'N/A'}`,
        value: item.tripCount,
        itemStyle: {
          color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'][idx % 8]
        }
      }))
    }]
  }

  pieChart.setOption(option)
}

watch(() => props.data, async () => {
  await nextTick()
  renderCharts()
}, { deep: true })

watch(() => props.loading, async (isLoading) => {
  if (!isLoading) {
    await nextTick()
    renderCharts()
  }
})

onMounted(() => {
  if (props.data.length > 0) {
    renderCharts()
  }
})
</script>

<style lang="scss" scoped>
.vendor-payment-analysis {
  width: 100%;
}

.panel-header {
  margin-bottom: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.panel-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px;
  color: #6b7280;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;

  &.highlight {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .summary-label {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 4px;
  }

  .summary-value {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }
}

.insights-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;

  &.positive {
    background: #ecfdf5;
    border: 1px solid #10b981;

    .insight-icon { color: #10b981; }
  }

  &.highlight {
    background: #fffbeb;
    border: 1px solid #f59e0b;

    .insight-icon { color: #f59e0b; }
  }

  &.info {
    background: #eff6ff;
    border: 1px solid #3b82f6;

    .insight-icon { color: #3b82f6; }
  }

  .insight-icon {
    font-size: 16px;
    margin-top: 2px;
  }

  .insight-title {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 2px;
  }

  .insight-description {
    font-size: 12px;
    color: #6b7280;
  }
}

.chart-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;

  .chart-container.half {
    flex: 1;
    min-width: 0;
  }

  .chart-area {
    width: 100%;
    height: 280px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.data-table-wrapper {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th, td {
    padding: 10px 12px;
    text-align: left;
    border-bottom: 1px solid #e5e7eb;
  }

  th {
    background: #f9fafb;
    font-weight: 600;
    color: #374151;
    white-space: nowrap;
  }

  tbody tr:hover {
    background: #f9fafb;
  }

  tbody tr:last-child td {
    border-bottom: none;
  }

  .vendor-cell {
    .vendor-badge {
      display: inline-block;
      padding: 2px 8px;
      background: #dbeafe;
      color: #1d4ed8;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .payment-cell {
    .payment-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;

      &.card {
        background: #dbeafe;
        color: #1d4ed8;
      }

      &.cash {
        background: #dcfce7;
        color: #15803d;
      }

      &.app {
        background: #fef3c7;
        color: #b45309;
      }

      &.default {
        background: #f3f4f6;
        color: #6b7280;
      }
    }
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
    color: #1f2937;
  }
}
</style>
