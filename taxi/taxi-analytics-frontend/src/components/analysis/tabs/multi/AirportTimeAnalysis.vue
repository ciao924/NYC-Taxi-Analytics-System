<template>
  <div class="airport-time-analysis">
    <div class="panel-header">
      <h3 class="panel-title">机场 × 时段 交叉分析</h3>
      <p class="panel-desc">分析三大机场（JFK、LGA、EWR）在各时段的订单分布与运营效率</p>
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
          <div class="summary-label">峰值时段</div>
          <div class="summary-value highlight-text">{{ peakHour }}</div>
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
        <div class="chart-container">
          <div ref="heatmapRef" class="chart-area"></div>
        </div>
      </div>

      <div class="airport-summary">
        <div v-for="airport in airportSummary" :key="airport.code" class="airport-card" :class="airport.code.toLowerCase()">
          <div class="airport-header">
            <span class="airport-code">{{ airport.code }}</span>
            <span class="airport-name">{{ airport.name }}</span>
          </div>
          <div class="airport-stats">
            <div class="stat-item">
              <span class="stat-label">订单数</span>
              <span class="stat-value">{{ formatNumber(airport.tripCount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">占比</span>
              <span class="stat-value">{{ airport.percentage }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">收入</span>
              <span class="stat-value">${{ formatNumber(airport.revenue, 2) }}</span>
            </div>
          </div>
          <div class="peak-hours">
            <span class="peak-label">高峰时段:</span>
            <span class="peak-value">{{ airport.peakHour }}</span>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>机场</th>
              <th>时段</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总收入</th>
              <th>平均金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
              <td>
                <span class="airport-badge" :class="getAirportClass(item.airportCode || item.dimension1Name)">
                  {{ item.airportCode || item.dimension1Name || 'N/A' }}
                </span>
              </td>
              <td>
                <span class="time-badge">{{ item.timeSlot || item.timePeriod || item.dimension2Name || item.hour ? formatHour(item.hour || item.dimension2Name) : 'N/A' }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue || 0, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.avgAmount || item.avgFare || 0, 2) }}</td>
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

const heatmapRef = ref<HTMLElement | null>(null)
let heatmapChart: echarts.ECharts | null = null

const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const formatHour = (hour: string | number): string => {
  const h = typeof hour === 'string' ? parseInt(hour) : hour
  if (isNaN(h)) return hour?.toString() || 'N/A'
  if (h >= 0 && h < 6) return `凌晨${h}:00`
  if (h >= 6 && h < 9) return `早高峰${h}:00`
  if (h >= 9 && h < 12) return `上午${h}:00`
  if (h >= 12 && h < 14) return `午间${h}:00`
  if (h >= 14 && h < 17) return `下午${h}:00`
  if (h >= 17 && h < 20) return `晚高峰${h}:00`
  return `夜间${h}:00`
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

const peakHour = computed(() => {
  if (props.data.length === 0) return 'N/A'
  const sorted = [...props.data].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  const peak = sorted[0]
  if (!peak) return 'N/A'
  const hour = peak.hour || peak.dimension2Name || peak.timeSlot
  return formatHour(hour)
})

const airportSummary = computed(() => {
  const airports = ['JFK', 'LGA', 'EWR']
  const summary = airports.map(code => {
    const airportData = props.data.filter(item =>
      (item.airportCode || item.dimension1Name || '').toUpperCase() === code
    )
    const tripCount = airportData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const revenue = airportData.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
    const peakItem = airportData.sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))[0]
    return {
      code,
      name: code === 'JFK' ? '肯尼迪机场' : code === 'LGA' ? '拉瓜迪亚机场' : '纽瓦克机场',
      tripCount,
      percentage: totalTrips.value > 0 ? ((tripCount / totalTrips.value) * 100).toFixed(1) : '0',
      revenue,
      peakHour: peakItem ? formatHour(peakItem.hour || peakItem.dimension2Name || peakItem.timeSlot) : 'N/A'
    }
  })
  return summary
})

const insights = computed(() => {
  if (props.data.length === 0) return []

  const result = []

  const jfkData = props.data.filter(item =>
    (item.airportCode || item.dimension1Name || '').toUpperCase() === 'JFK'
  )
  const lgaData = props.data.filter(item =>
    (item.airportCode || item.dimension1Name || '').toUpperCase() === 'LGA'
  )
  const ewrData = props.data.filter(item =>
    (item.airportCode || item.dimension1Name || '').toUpperCase() === 'EWR'
  )

  const jfkTrips = jfkData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const lgaTrips = lgaData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const ewrTrips = ewrData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  const maxTrips = Math.max(jfkTrips, lgaTrips, ewrTrips)
  if (maxTrips === jfkTrips && jfkTrips > 0) {
    result.push({
      icon: '✈️',
      type: 'highlight',
      title: 'JFK机场订单量领先',
      description: `JFK机场总订单 ${formatNumber(jfkTrips)} 单，占三大机场的 ${((jfkTrips / (jfkTrips + lgaTrips + ewrTrips)) * 100).toFixed(1)}%`
    })
  }

  const allSlots = [...jfkData, ...lgaData, ...ewrData]
  const morningData = allSlots.filter(item => {
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '0')
    return hour >= 6 && hour < 12
  })
  const eveningData = allSlots.filter(item => {
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '0')
    return hour >= 17 && hour < 21
  })
  const morningTrips = morningData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const eveningTrips = eveningData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (eveningTrips > morningTrips) {
    result.push({
      icon: '🌆',
      type: 'positive',
      title: '晚高峰订单更活跃',
      description: `晚高峰(17-21点)订单 ${formatNumber(eveningTrips)} 单，较早高峰(6-12点)的 ${formatNumber(morningTrips)} 单多 ${(((eveningTrips - morningTrips) / morningTrips) * 100).toFixed(1)}%`
    })
  }

  return result
})

const getAirportClass = (code: string): string => {
  if (!code) return ''
  return code.toUpperCase()
}

const renderHeatmap = () => {
  if (!heatmapRef.value || props.data.length === 0) return

  heatmapChart?.dispose()
  heatmapChart = echarts.init(heatmapRef.value)

  const airports = ['JFK', 'LGA', 'EWR']
  const hours = Array.from({ length: 24 }, (_, i) => i)

  const matrix: number[][] = []
  airports.forEach(airport => {
    const row: number[] = []
    hours.forEach(hour => {
      const item = props.data.find(d =>
        (d.airportCode || d.dimension1Name || '').toUpperCase() === airport &&
        parseInt(d.hour || d.dimension2Name || d.timeSlot || '-1') === hour
      )
      row.push(item?.tripCount || 0)
    })
    matrix.push(row)
  })

  const maxValue = Math.max(...matrix.flat())

  const option: echarts.EChartsOption = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `<strong>${airports[params[1]]} - ${params[2]}:00</strong><br/>
                订单数: ${formatNumber(params.value)}<br/>
                占比: ${((params.value / maxValue) * 100).toFixed(1)}%`
      }
    },
    grid: { left: '5%', right: '8%', bottom: '15%', top: '10%' },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}:00`),
      axisLabel: { interval: 2, rotate: 0 },
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: airports,
      axisLabel: { interval: 0 }
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#e6f7ff', '#1890ff', '#0050b3']
      }
    },
    series: [{
      type: 'heatmap',
      data: matrix.flatMap((row, i) => row.map((value, j) => [j, i, value])),
      label: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.3)' }
      }
    }]
  }

  heatmapChart.setOption(option)
}

watch(() => props.data, async () => {
  await nextTick()
  renderHeatmap()
}, { deep: true })

watch(() => props.loading, async (isLoading) => {
  if (!isLoading) {
    await nextTick()
    renderHeatmap()
  }
})

onMounted(() => {
  if (props.data.length > 0) {
    renderHeatmap()
  }
})
</script>

<style lang="scss" scoped>
.airport-time-analysis {
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

  .highlight-text {
    color: #3b82f6;
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
    height: 300px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.airport-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .airport-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    &.jfk {
      border-left: 3px solid #3b82f6;
    }

    &.lga {
      border-left: 3px solid #10b981;
    }

    &.ewr {
      border-left: 3px solid #f59e0b;
    }

    .airport-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .airport-code {
        font-size: 18px;
        font-weight: 700;
        color: #1f2937;
      }

      .airport-name {
        font-size: 12px;
        color: #6b7280;
      }
    }

    .airport-stats {
      display: flex;
      gap: 12px;
      margin-bottom: 8px;

      .stat-item {
        display: flex;
        flex-direction: column;

        .stat-label {
          font-size: 11px;
          color: #9ca3af;
        }

        .stat-value {
          font-size: 14px;
          font-weight: 600;
          color: #1f2937;
        }
      }
    }

    .peak-hours {
      font-size: 12px;

      .peak-label {
        color: #6b7280;
      }

      .peak-value {
        color: #ef4444;
        font-weight: 500;
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

  .airport-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;

    &.JFK {
      background: #dbeafe;
      color: #1d4ed8;
    }

    &.LGA {
      background: #dcfce7;
      color: #15803d;
    }

    &.EWR {
      background: #fef3c7;
      color: #b45309;
    }
  }

  .time-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #f3f4f6;
    color: #4b5563;
    border-radius: 4px;
    font-size: 12px;
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
    color: #1f2937;
  }
}
</style>
