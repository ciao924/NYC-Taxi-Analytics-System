<template>
  <div class="vendor-taxi-type-analysis">
    <div class="panel-header">
      <h3 class="panel-title">供应商 × 车型 交叉分析</h3>
      <p class="panel-desc">分析不同供应商在各车型上的运营表现与收入贡献</p>
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
          <div class="summary-label">平均小费</div>
          <div class="summary-value">${{ formatNumber(avgTip, 2) }}</div>
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

      <div class="taxi-type-summary">
        <div v-for="type in taxiTypeSummary" :key="type.name" class="taxi-type-card">
          <div class="taxi-type-header">
            <span class="taxi-type-icon">{{ type.icon }}</span>
            <span class="taxi-type-name">{{ type.name }}</span>
          </div>
          <div class="taxi-type-stats">
            <div class="stat-item">
              <span class="stat-label">订单</span>
              <span class="stat-value">{{ formatNumber(type.tripCount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">收入</span>
              <span class="stat-value">${{ formatNumber(type.revenue, 2) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">小费率</span>
              <span class="stat-value">{{ type.tipRate }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>供应商</th>
              <th>车型</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总收入</th>
              <th>平均金额</th>
              <th>小费率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
              <td>
                <span class="vendor-badge">{{ item.vendorName || item.dimension1Name || 'N/A' }}</span>
              </td>
              <td>
                <span class="taxi-type-badge">{{ item.taxiType || item.dimension2Name || 'N/A' }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue || 0, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.avgAmount || item.avgFare || 0, 2) }}</td>
              <td class="amount-cell">{{ formatNumber(item.tipRate || 0, 1) }}%</td>
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

const totalRevenue = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
})

const totalTip = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalTip || 0), 0)
})

const avgTip = computed(() => {
  if (totalTrips.value === 0) return 0
  return totalTip.value / totalTrips.value
})

const taxiTypeSummary = computed(() => {
  const types = ['Yellow', 'Green', 'Uber', 'Lyft']
  return types.map(name => {
    const data = props.data.filter(item =>
      (item.taxiType || item.dimension2Name || '').toLowerCase().includes(name.toLowerCase())
    )
    const tripCount = data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
    const revenue = data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
    const tips = data.reduce((sum, item) => sum + (item.totalTip || 0), 0)
    const tipRate = revenue > 0 ? ((tips / revenue) * 100).toFixed(1) : '0'
    return {
      name,
      icon: name === 'Yellow' ? '🚕' : name === 'Green' ? '🚗' : name === 'Uber' ? '🚙' : '🚘',
      tripCount,
      revenue,
      tipRate
    }
  }).filter(t => t.tripCount > 0)
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const yellowData = props.data.filter(item =>
    (item.taxiType || item.dimension2Name || '').toLowerCase().includes('yellow')
  )
  const greenData = props.data.filter(item =>
    (item.taxiType || item.dimension2Name || '').toLowerCase().includes('green')
  )
  const yellowTrips = yellowData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const greenTrips = greenData.reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (yellowTrips > greenTrips) {
    result.push({
      icon: '🚕',
      type: 'highlight',
      title: 'Yellow Taxi主导',
      description: `Yellow Taxi订单 ${formatNumber(yellowTrips)} 单，Green Taxi ${formatNumber(greenTrips)} 单，Yellow领先 ${(((yellowTrips - greenTrips) / greenTrips) * 100).toFixed(1)}%`
    })
  }

  const uberData = props.data.filter(item =>
    (item.taxiType || item.dimension2Name || '').toLowerCase().includes('uber')
  )
  const uberRevenue = uberData.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)

  if (uberRevenue > 0) {
    result.push({
      icon: '🚙',
      type: 'info',
      title: 'Uber收入贡献',
      description: `Uber总营收 $${formatNumber(uberRevenue, 2)}，占比 ${((uberRevenue / totalRevenue.value) * 100).toFixed(1)}%`
    })
  }

  return result
})

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
    `${(item.vendorName || item.dimension1Name || 'N/A').substring(0, 8)}×${(item.taxiType || item.dimension2Name || 'N/A').substring(0, 6)}`
  )

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const idx = params[0].dataIndex
        const item = topData[idx]
        return `<strong>${item.vendorName || item.dimension1Name} × ${item.taxiType || item.dimension2Name}</strong><br/>
                订单数: ${formatNumber(item.tripCount)}<br/>
                总收入: $${formatNumber(item.totalAmount || item.totalRevenue, 2)}<br/>
                小费率: ${formatNumber(item.tipRate || 0, 1)}%`
      }
    },
    legend: { data: ['订单数', '收入(千$)'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { rotate: 30 }
    },
    yAxis: [
      { type: 'value', name: '订单数', position: 'left' },
      { type: 'value', name: '收入(千$)', position: 'right' }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: topData.map(item => item.tripCount),
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '收入(千$)',
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

  const typeGroups: Record<string, number> = {}
  props.data.forEach(item => {
    const type = item.taxiType || item.dimension2Name || 'Unknown'
    typeGroups[type] = (typeGroups[type] || 0) + (item.tripCount || 0)
  })

  const sortedTypes = Object.entries(typeGroups).sort((a, b) => b[1] - a[1]).slice(0, 6)

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
      data: sortedTypes.map(([type, count], idx) => ({
        name: type,
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
.vendor-taxi-type-analysis {
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

.taxi-type-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .taxi-type-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .taxi-type-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .taxi-type-icon {
        font-size: 18px;
      }

      .taxi-type-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .taxi-type-stats {
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

  .vendor-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #dbeafe;
    color: #1d4ed8;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

  .taxi-type-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #dcfce7;
    color: #15803d;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
  }
}
</style>
