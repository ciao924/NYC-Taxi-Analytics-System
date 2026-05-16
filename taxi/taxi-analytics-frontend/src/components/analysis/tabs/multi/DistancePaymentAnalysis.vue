<template>
  <div class="distance-payment-analysis">
    <div class="panel-header">
      <h3 class="panel-title">距离区间 × 支付方式 交叉分析</h3>
      <p class="panel-desc">分析不同距离区间内各支付方式的使用偏好与消费特征</p>
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
          <div class="summary-label">平均距离</div>
          <div class="summary-value">{{ formatNumber(avgDistance, 1) }} 英里</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">长途刷卡率</div>
          <div class="summary-value">{{ longDistanceCardRatio }}%</div>
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

      <div class="distance-summary">
        <div v-for="range in distanceRanges" :key="range.name" class="range-card">
          <div class="range-header">
            <span class="range-icon">{{ range.icon }}</span>
            <span class="range-name">{{ range.name }}</span>
          </div>
          <div class="range-stats">
            <div class="stat-item">
              <span class="stat-label">订单</span>
              <span class="stat-value">{{ formatNumber(range.tripCount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">刷卡率</span>
              <span class="stat-value" :class="range.cardRatio > 80 ? 'positive' : ''">{{ range.cardRatio }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">客单价</span>
              <span class="stat-value">${{ formatNumber(range.avgAmount, 1) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>距离区间</th>
              <th>支付方式</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总收入</th>
              <th>平均金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
              <td>
                <span class="distance-badge">{{ item.distanceRange || item.dimension1Name || 'N/A' }}</span>
              </td>
              <td>
                <span class="payment-badge" :class="getPaymentClass(item.paymentType || item.dimension2Name)">
                  {{ item.paymentType || item.dimension2Name || item.paymentDesc || 'N/A' }}
                </span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue || 0, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.avgAmount || 0, 2) }}</td>
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

const tableData = computed(() => props.data.slice(0, 20))

const totalCombinations = computed(() => props.data.length)

const totalTrips = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const avgDistance = computed(() => {
  let totalDistance = 0
  let validCount = 0
  props.data.forEach(item => {
    if (item.avgDistance && item.tripCount) {
      totalDistance += item.avgDistance * item.tripCount
      validCount += item.tripCount
    }
  })
  return validCount > 0 ? totalDistance / validCount : 0
})

const longDistanceCardRatio = computed(() => {
  const longDistanceData = props.data.filter(item => {
    const range = item.distanceRange || item.dimension1Name || ''
    return range.includes('10-20') || range.includes('20+') || range.includes('>10')
  })
  const total = longDistanceData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const cardData = longDistanceData.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
  )
  const cardTrips = cardData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  return total > 0 ? ((cardTrips / total) * 100).toFixed(1) : '0'
})

const distanceRanges = computed(() => {
  const ranges = [
    { name: '短途(<2英里)', pattern: ['<2', '0-2'], icon: '🚶' },
    { name: '中短途(2-5英里)', pattern: ['2-5'], icon: '🚲' },
    { name: '中长途(5-10英里)', pattern: ['5-10'], icon: '🚗' },
    { name: '长途(>10英里)', pattern: ['10-', '>10', '20+'], icon: '🚀' }
  ]

  return ranges.map(range => {
    const rangeData = props.data.filter(item => {
      const distance = item.distanceRange || item.dimension1Name || ''
      return range.pattern.some(p => distance.includes(p))
    })
    const tripCount = rangeData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cardTrips = rangeData.filter(item =>
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
    ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const totalAmount = rangeData.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
    return {
      ...range,
      tripCount,
      cardRatio: tripCount > 0 ? Math.round((cardTrips / tripCount) * 100) : 0,
      avgAmount: tripCount > 0 ? totalAmount / tripCount : 0
    }
  })
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const shortDistance = props.data.filter(item => {
    const range = item.distanceRange || item.dimension1Name || ''
    return range.includes('<2') || range.includes('0-2')
  })
  const shortCash = shortDistance.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
  ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const shortTotal = shortDistance.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (shortTotal > 0 && shortCash / shortTotal > 0.4) {
    result.push({
      icon: '🚶',
      type: 'info',
      title: '短途现金支付偏好',
      description: `短途出行(<2英里)现金支付占比 ${((shortCash / shortTotal) * 100).toFixed(1)}%，高于其他距离区间`
    })
  }

  const longDistance = props.data.filter(item => {
    const range = item.distanceRange || item.dimension1Name || ''
    return range.includes('10-') || range.includes('>10') || range.includes('20+')
  })
  const longAvgAmount = longDistance.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
  const longCount = longDistance.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const shortAvgAmount = shortDistance.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
  const shortCount = shortTotal

  if (longCount > 0 && shortCount > 0 && longAvgAmount / longCount > shortAvgAmount / shortCount * 2) {
    result.push({
      icon: '💰',
      type: 'highlight',
      title: '长途客单价更高',
      description: `长途出行平均金额 $${(longAvgAmount / longCount).toFixed(2)}，是短途的 ${((longAvgAmount / longCount) / (shortAvgAmount / shortCount)).toFixed(1)} 倍`
    })
  }

  return result
})

const getPaymentClass = (payment: string): string => {
  if (!payment) return 'default'
  const p = payment.toLowerCase()
  if (p.includes('card') || p.includes('credit')) return 'card'
  if (p.includes('cash')) return 'cash'
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

  const ranges = ['短途', '中短途', '中长途', '长途']
  const rangePatterns = ['<2|0-2', '2-5', '5-10', '10-|>10|20+']

  const cardData: number[] = []
  const cashData: number[] = []

  rangePatterns.forEach(pattern => {
    const cardTrips = props.data.filter(item => {
      const range = item.distanceRange || item.dimension1Name || ''
      const match = new RegExp(pattern).test(range)
      const isCard = (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
      return match && isCard
    }).reduce((sum, item) => sum + (item.tripCount || 0), 0)
    cardData.push(cardTrips)

    const cashTrips = props.data.filter(item => {
      const range = item.distanceRange || item.dimension1Name || ''
      const match = new RegExp(pattern).test(range)
      const isCash = (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
      return match && isCash
    }).reduce((sum, item) => sum + (item.tripCount || 0), 0)
    cashData.push(cashTrips)
  })

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const idx = params[0].dataIndex
        return `<strong>${ranges[idx]}</strong><br/>
                ${params[0].marker} 刷卡: ${formatNumber(params[0].value)}<br/>
                ${params[1].marker} 现金: ${formatNumber(params[1].value)}`
      }
    },
    legend: { data: ['刷卡', '现金'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: ranges },
    yAxis: { type: 'value', name: '订单数' },
    series: [
      { name: '刷卡', type: 'bar', data: cardData, itemStyle: { color: '#3b82f6' } },
      { name: '现金', type: 'bar', data: cashData, itemStyle: { color: '#10b981' } }
    ]
  }

  barChart.setOption(option)
}

const renderPieChart = () => {
  if (!pieChartRef.value || props.data.length === 0) return

  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const rangeGroups: Record<string, number> = {}
  props.data.forEach(item => {
    const range = item.distanceRange || item.dimension1Name || 'Unknown'
    rangeGroups[range] = (rangeGroups[range] || 0) + (item.tripCount || 0)
  })

  const sorted = Object.entries(rangeGroups).sort((a, b) => b[1] - a[1]).slice(0, 6)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `<strong>${params.name}</strong><br/>订单数: ${formatNumber(params.value)}<br/>占比: ${params.percent.toFixed(1)}%`
      }
    },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['40%', '50%'],
      label: { show: true, formatter: '{b}: {d}%' },
      data: sorted.map(([range, count], idx) => ({
        name: range,
        value: count,
        itemStyle: {
          color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'][idx]
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
.distance-payment-analysis {
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

  &.info {
    background: #eff6ff;
    border: 1px solid #3b82f6;

    .insight-icon { color: #3b82f6; }
  }

  &.highlight {
    background: #fffbeb;
    border: 1px solid #f59e0b;

    .insight-icon { color: #f59e0b; }
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
  }

  .chart-area {
    width: 100%;
    height: 280px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.distance-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .range-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .range-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .range-icon {
        font-size: 18px;
      }

      .range-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .range-stats {
      display: flex;
      gap: 12px;

      .stat-item {
        display: flex;
        flex-direction: column;

        .stat-label {
          font-size: 11px;
          color: #9ca3af;
        }

        .stat-value {
          font-size: 13px;
          font-weight: 600;
          color: #1f2937;

          &.positive {
            color: #10b981;
          }
        }
      }
    }
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

  .distance-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #fef3c7;
    color: #b45309;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

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

    &.default {
      background: #f3f4f6;
      color: #6b7280;
    }
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
  }
}
</style>
