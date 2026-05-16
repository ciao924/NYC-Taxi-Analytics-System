<template>
  <div class="time-payment-analysis">
    <div class="panel-header">
      <h3 class="panel-title">时段 × 支付方式 交叉分析</h3>
      <p class="panel-desc">分析不同时段各支付方式的使用情况与消费特征</p>
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
          <div class="summary-label">刷卡高峰时段</div>
          <div class="summary-value">{{ cardPeakHour }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">现金高峰时段</div>
          <div class="summary-value">{{ cashPeakHour }}</div>
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

      <div class="time-summary">
        <div v-for="period in timePeriods" :key="period.name" class="period-card">
          <div class="period-header">
            <span class="period-icon">{{ period.icon }}</span>
            <span class="period-name">{{ period.name }}</span>
          </div>
          <div class="period-stats">
            <div class="stat-row">
              <span class="stat-label">总订单</span>
              <span class="stat-value">{{ formatNumber(period.tripCount) }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">刷卡占比</span>
              <span class="stat-value" :class="period.cardRatio > 70 ? 'positive' : ''">{{ period.cardRatio }}%</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">现金占比</span>
              <span class="stat-value" :class="period.cashRatio > 50 ? 'warning' : ''">{{ period.cashRatio }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>时段</th>
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
                <span class="time-badge">{{ formatHour(item.hour || item.dimension1Name || item.timeSlot) }}</span>
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

const formatHour = (hour: string | number): string => {
  if (!hour) return 'N/A'
  const h = typeof hour === 'string' ? parseInt(hour) : hour
  if (isNaN(h)) return hour.toString()
  return `${h}:00-${h + 1}:00`
}

const tableData = computed(() => props.data.slice(0, 20))

const totalCombinations = computed(() => props.data.length)

const totalTrips = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const cardPeakHour = computed(() => {
  const cardData = props.data.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
  )
  if (cardData.length === 0) return 'N/A'
  const sorted = [...cardData].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  return formatHour(sorted[0].hour || sorted[0].dimension1Name || sorted[0].timeSlot)
})

const cashPeakHour = computed(() => {
  const cashData = props.data.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
  )
  if (cashData.length === 0) return 'N/A'
  const sorted = [...cashData].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  return formatHour(sorted[0].hour || sorted[0].dimension1Name || sorted[0].timeSlot)
})

const timePeriods = computed(() => {
  const periods = [
    { name: '早高峰', hours: [7, 8, 9], icon: '🌅' },
    { name: '午间', hours: [12, 13, 14], icon: '☀️' },
    { name: '晚高峰', hours: [17, 18, 19], icon: '🌆' },
    { name: '夜间', hours: [22, 23, 0], icon: '🌙' }
  ]

  return periods.map(period => {
    const periodData = props.data.filter(item => {
      const hour = parseInt(item.hour || item.dimension1Name || item.timeSlot || '-1')
      return period.hours.includes(hour)
    })
    const tripCount = periodData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cardTrips = periodData.filter(item =>
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
    ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const cashTrips = periodData.filter(item =>
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
    ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
    return {
      ...period,
      tripCount,
      cardRatio: tripCount > 0 ? Math.round((cardTrips / tripCount) * 100) : 0,
      cashRatio: tripCount > 0 ? Math.round((cashTrips / tripCount) * 100) : 0
    }
  })
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const nightData = props.data.filter(item => {
    const hour = parseInt(item.hour || item.dimension1Name || item.timeSlot || '-1')
    return hour >= 22 || hour < 4
  })
  const nightCash = nightData.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
  ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const nightTotal = nightData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (nightTotal > 0 && nightCash / nightTotal > 0.3) {
    result.push({
      icon: '🌙',
      type: 'highlight',
      title: '夜间现金支付占比高',
      description: `夜间(22:00-04:00)现金支付占比 ${((nightCash / nightTotal) * 100).toFixed(1)}%，需关注夜间安全支付问题`
    })
  }

  const dayData = props.data.filter(item => {
    const hour = parseInt(item.hour || item.dimension1Name || item.timeSlot || '-1')
    return hour >= 9 && hour < 17
  })
  const dayCard = dayData.filter(item =>
    (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
  ).reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const dayTotal = dayData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (dayTotal > 0 && dayCard / dayTotal > 0.7) {
    result.push({
      icon: '💳',
      type: 'positive',
      title: '日间刷卡支付主导',
      description: `日间(09:00-17:00)刷卡支付占比 ${((dayCard / dayTotal) * 100).toFixed(1)}%，电子支付普及`
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

  const hours = Array.from({ length: 24 }, (_, i) => i)
  const cardData: number[] = []
  const cashData: number[] = []

  hours.forEach(hour => {
    const cardItem = props.data.find(item =>
      parseInt(item.hour || item.dimension1Name || item.timeSlot || '-1') === hour &&
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('card')
    )
    const cashItem = props.data.find(item =>
      parseInt(item.hour || item.dimension1Name || item.timeSlot || '-1') === hour &&
      (item.paymentType || item.dimension2Name || item.paymentDesc || item.dimension2 || '').toLowerCase().includes('cash')
    )
    cardData.push(cardItem?.tripCount || 0)
    cashData.push(cashItem?.tripCount || 0)
  })

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const hour = params[0].dataIndex
        return `<strong>${hour}:00-${hour + 1}:00</strong><br/>
                ${params[0].marker} 刷卡: ${formatNumber(params[0].value)}<br/>
                ${params[1].marker} 现金: ${formatNumber(params[1].value)}`
      }
    },
    legend: { data: ['刷卡', '现金'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}:00`),
      axisLabel: { interval: 2 }
    },
    yAxis: { type: 'value', name: '订单数' },
    series: [
      {
        name: '刷卡',
        type: 'line',
        data: cardData,
        smooth: true,
        lineStyle: { width: 3, color: '#3b82f6' },
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '现金',
        type: 'line',
        data: cashData,
        smooth: true,
        lineStyle: { width: 3, color: '#10b981' },
        itemStyle: { color: '#10b981' }
      }
    ]
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
.time-payment-analysis {
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

  &.positive {
    background: #ecfdf5;
    border: 1px solid #10b981;

    .insight-icon { color: #10b981; }
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

.time-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .period-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .period-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .period-icon {
        font-size: 18px;
      }

      .period-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .period-stats {
      .stat-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 4px;

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

          &.warning {
            color: #f59e0b;
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

  .time-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #f3f4f6;
    color: #4b5563;
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
