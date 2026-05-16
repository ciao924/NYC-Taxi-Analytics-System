<template>
  <div class="borough-payment-analysis">
    <div class="panel-header">
      <h3 class="panel-title">区域 × 支付方式 交叉分析</h3>
      <p class="panel-desc">分析纽约各区（Manhattan、Brooklyn、Queens等）在不同支付方式上的表现</p>
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
          <div class="summary-label">总收入</div>
          <div class="summary-value">${{ formatNumber(totalRevenue, 2) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">现金支付占比</div>
          <div class="summary-value">{{ cashPercentage }}%</div>
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
        <div class="chart-container">
          <div ref="chartRef" class="chart-area"></div>
        </div>
      </div>

      <div class="borough-grid">
        <div v-for="borough in boroughSummary" :key="borough.name" class="borough-card">
          <div class="borough-header">
            <span class="borough-name">{{ borough.name }}</span>
            <span class="borough-trips">{{ formatNumber(borough.tripCount) }} 单</span>
          </div>
          <div class="payment-breakdown">
            <div class="payment-bar">
              <div class="card-portion" :style="{ width: borough.cardRatio + '%' }"></div>
              <div class="cash-portion" :style="{ width: borough.cashRatio + '%' }"></div>
            </div>
            <div class="payment-labels">
              <span class="card-label">💳 {{ borough.cardRatio }}%</span>
              <span class="cash-label">💵 {{ borough.cashRatio }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>区域</th>
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
                <span class="borough-badge">{{ item.borough || item.dimension1Name || 'N/A' }}</span>
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

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

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

const totalRevenue = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
})

const cashPercentage = computed(() => {
  const cashTrips = props.data
    .filter(item => (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash'))
    .reduce((sum, item) => sum + (item.tripCount || 0), 0)
  return totalTrips.value > 0 ? ((cashTrips / totalTrips.value) * 100).toFixed(1) : '0'
})

const boroughSummary = computed(() => {
  const boroughs = ['Manhattan', 'Brooklyn', 'Queens', 'Bronx', 'Staten Island', 'Unknown']
  return boroughs.map(name => {
    const data = props.data.filter(item =>
      (item.borough || item.dimension1Name || '').toLowerCase().includes(name.toLowerCase())
    )
    const tripCount = data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cardData = data.filter(item =>
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
    )
    const cashData = data.filter(item =>
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
    )
    const cardTrips = cardData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cashTrips = cashData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    return {
      name,
      tripCount,
      cardRatio: tripCount > 0 ? Math.round((cardTrips / tripCount) * 100) : 0,
      cashRatio: tripCount > 0 ? Math.round((cashTrips / tripCount) * 100) : 0
    }
  }).filter(b => b.tripCount > 0)
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const manhattanData = props.data.filter(item =>
    (item.borough || item.dimension1Name || '').toLowerCase().includes('manhattan')
  )
  const manhattanTrips = manhattanData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (manhattanTrips > 0) {
    const manhattanCard = manhattanData
      .filter(item => (item.paymentType || item.dimension2Name || '').toLowerCase().includes('card'))
      .reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cardRatio = ((manhattanCard / manhattanTrips) * 100).toFixed(1)

    result.push({
      icon: '🏙️',
      type: 'highlight',
      title: '曼哈顿区域主导',
      description: `Manhattan订单量 ${formatNumber(manhattanTrips)} 单，其中信用卡支付占比 ${cardRatio}%`
    })
  }

  const nonManhattan = props.data.filter(item =>
    !(item.borough || item.dimension1Name || '').toLowerCase().includes('manhattan')
  )
  const nonManhattanCash = nonManhattan
    .filter(item => (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash'))
    .reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const nonManhattanTotal = nonManhattan.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (nonManhattanTotal > 0 && nonManhattanCash / nonManhattanTotal > 0.3) {
    result.push({
      icon: '💵',
      type: 'info',
      title: '外围区域现金偏好',
      description: `Manhattan以外区域现金支付比例较高，合计 ${formatNumber(nonManhattanCash)} 单（占比 ${((nonManhattanCash / nonManhattanTotal) * 100).toFixed(1)}%）`
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

const renderChart = () => {
  if (!chartRef.value || props.data.length === 0) return

  chart?.dispose()
  chart = echarts.init(chartRef.value)

  const topData = props.data.slice(0, 15)
  const boroughs = [...new Set(topData.map(item => item.borough || item.dimension1Name || 'N/A'))]

  const series: echarts.SeriesOption[] = []
  const payments = ['card', 'cash']
  const colors = { card: '#3b82f6', cash: '#10b981' }

  payments.forEach(payment => {
    series.push({
      name: payment === 'card' ? '信用卡' : '现金',
      type: 'bar',
      stack: payment,
      data: boroughs.map(borough => {
        const item = topData.find(d =>
          (d.borough || d.dimension1Name) === borough &&
          (d.paymentType || d.dimension2Name || d.paymentDesc || d.dimension2 || '').toLowerCase().includes(payment)
        )
        return item?.tripCount || 0
      }),
      itemStyle: { color: colors[payment as keyof typeof colors] }
    })
  })

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        let result = `<strong>${params[0].axisValue}</strong><br/>`
        params.forEach((p: any) => {
          if (p.value > 0) {
            result += `${p.marker} ${p.seriesName}: ${formatNumber(p.value)}<br/>`
          }
        })
        return result
      }
    },
    legend: { data: ['信用卡', '现金'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: boroughs,
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value', name: '订单数' },
    series
  }

  chart.setOption(option)
}

watch(() => props.data, async () => {
  await nextTick()
  renderChart()
}, { deep: true })

watch(() => props.loading, async (isLoading) => {
  if (!isLoading) {
    await nextTick()
    renderChart()
  }
})

onMounted(() => {
  if (props.data.length > 0) {
    renderChart()
  }
})
</script>

<style lang="scss" scoped>
.borough-payment-analysis {
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
  margin-bottom: 16px;

  .chart-area {
    width: 100%;
    height: 280px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.borough-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .borough-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .borough-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .borough-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }

      .borough-trips {
        font-size: 12px;
        color: #6b7280;
      }
    }

    .payment-breakdown {
      .payment-bar {
        display: flex;
        height: 8px;
        border-radius: 4px;
        overflow: hidden;
        background: #e5e7eb;

        .card-portion {
          background: #3b82f6;
        }

        .cash-portion {
          background: #10b981;
        }
      }

      .payment-labels {
        display: flex;
        justify-content: space-between;
        margin-top: 4px;
        font-size: 11px;
        color: #6b7280;
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

  .borough-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #ede9fe;
    color: #6d28d9;
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
