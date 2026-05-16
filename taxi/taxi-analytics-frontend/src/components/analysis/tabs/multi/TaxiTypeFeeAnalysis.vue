<template>
  <div class="taxi-type-fee-analysis">
    <div class="panel-header">
      <h3 class="panel-title">车型 × 费用 交叉分析</h3>
      <p class="panel-desc">分析不同车型在各类费用项目上的收入贡献与结构特征</p>
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
          <div class="summary-label">平均小费率</div>
          <div class="summary-value">{{ avgTipRate }}%</div>
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

      <div class="fee-breakdown">
        <div v-for="type in taxiTypeBreakdown" :key="type.name" class="type-card">
          <div class="type-header">
            <span class="type-icon">{{ type.icon }}</span>
            <span class="type-name">{{ type.name }}</span>
          </div>
          <div class="type-fees">
            <div class="fee-bar">
              <div 
                v-for="(fee, idx) in type.fees" 
                :key="idx" 
                class="fee-segment"
                :style="{ width: fee.percentage + '%', backgroundColor: fee.color }"
                :title="`${fee.name}: ${fee.amount}`"
              ></div>
            </div>
            <div class="fee-legend">
              <span v-for="(fee, idx) in type.fees" :key="idx" class="legend-item">
                <span class="legend-color" :style="{ backgroundColor: fee.color }"></span>
                <span class="legend-text">{{ fee.name }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>车型</th>
              <th>费用类型</th>
              <th>订单数</th>
              <th>费用金额</th>
              <th>占比</th>
              <th>平均金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="index">
              <td>
                <span class="taxi-type-badge">{{ item.taxiType || item.dimension1Name || 'N/A' }}</span>
              </td>
              <td>
                <span class="fee-type-badge">{{ item.feeType || item.feeCode || item.dimension2Name || 'N/A' }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue || 0, 2) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
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

const totalRevenue = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
})

const totalTip = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.totalTip || 0), 0)
})

const avgTipRate = computed(() => {
  if (totalRevenue.value === 0) return '0'
  return ((totalTip.value / totalRevenue.value) * 100).toFixed(1)
})

const taxiTypeBreakdown = computed(() => {
  const types = ['Yellow', 'Green', 'Uber', 'Lyft']
  const feeTypes = ['fare', 'tip', 'surcharge', 'tolls', 'misc']
  const feeNames = { fare: '车费', tip: '小费', surcharge: '附加费', tolls: '过路费', misc: '其他' }
  const feeColors = { fare: '#3b82f6', tip: '#10b981', surcharge: '#f59e0b', tolls: '#ef4444', misc: '#8b5cf6' }

  return types.map(typeName => {
    const typeData = props.data.filter(item =>
      (item.taxiType || item.dimension1Name || '').toLowerCase().includes(typeName.toLowerCase())
    )

    const fees: Record<string, number> = {}
    feeTypes.forEach(feeType => {
      fees[feeType] = typeData.filter(item =>
        (item.feeType || item.feeCode || item.dimension2Name || '').toLowerCase().includes(feeType)
      ).reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
    })

    const total = Object.values(fees).reduce((sum, val) => sum + val, 0)

    const feeList = feeTypes
      .filter(ft => fees[ft] > 0)
      .map(ft => ({
        name: feeNames[ft as keyof typeof feeNames],
        amount: `$${formatNumber(fees[ft], 2)}`,
        percentage: total > 0 ? (fees[ft] / total * 100).toFixed(1) : '0',
        color: feeColors[ft as keyof typeof feeColors]
      }))

    return {
      name: typeName,
      icon: typeName === 'Yellow' ? '🚕' : typeName === 'Green' ? '🚗' : typeName === 'Uber' ? '🚙' : '🚘',
      fees: feeList
    }
  }).filter(t => t.fees.length > 0)
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const uberData = props.data.filter(item =>
    (item.taxiType || item.dimension1Name || '').toLowerCase().includes('uber')
  )
  const uberTip = uberData.filter(item =>
    (item.feeType || item.feeCode || item.dimension2Name || '').toLowerCase().includes('tip')
  ).reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || item.totalTip || 0), 0)
  const uberTotal = uberData.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)

  if (uberTotal > 0) {
    const uberTipRate = ((uberTip / uberTotal) * 100).toFixed(1)
    result.push({
      icon: '💰',
      type: 'info',
      title: 'Uber小费率',
      description: `Uber平均小费率 ${uberTipRate}%，反映平台用户小费习惯`
    })
  }

  const yellowData = props.data.filter(item =>
    (item.taxiType || item.dimension1Name || '').toLowerCase().includes('yellow')
  )
  const yellowSurcharge = yellowData.filter(item =>
    (item.feeType || item.feeCode || item.dimension2Name || '').toLowerCase().includes('surcharge')
  ).reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
  const yellowTotal = yellowData.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)

  if (yellowTotal > 0 && yellowSurcharge / yellowTotal > 0.1) {
    result.push({
      icon: '📈',
      type: 'highlight',
      title: 'Yellow Taxi附加费占比高',
      description: `Yellow Taxi附加费占总收入 ${((yellowSurcharge / yellowTotal) * 100).toFixed(1)}%，需关注定价策略`
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

  const types = ['Yellow', 'Green', 'Uber', 'Lyft']
  const feeTypes = ['fare', 'tip', 'surcharge']
  const feeNames = { fare: '车费', tip: '小费', surcharge: '附加费' }
  const feeColors = { fare: '#3b82f6', tip: '#10b981', surcharge: '#f59e0b' }

  const series: echarts.SeriesOption[] = feeTypes.map(feeType => ({
    name: feeNames[feeType as keyof typeof feeNames],
    type: 'bar',
    stack: 'total',
    data: types.map(typeName => {
      const data = props.data.filter(item =>
        (item.taxiType || item.dimension1Name || '').toLowerCase().includes(typeName.toLowerCase()) &&
        (item.feeType || item.feeCode || item.dimension2Name || '').toLowerCase().includes(feeType)
      )
      return data.reduce((sum, item) => sum + (item.totalAmount || item.totalRevenue || 0), 0)
    }),
    itemStyle: { color: feeColors[feeType as keyof typeof feeColors] }
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        let result = `<strong>${params[0].axisValue}</strong><br/>`
        let total = 0
        params.forEach((p: any) => {
          if (p.value > 0) {
            total += p.value
            result += `${p.marker} ${p.seriesName}: $${formatNumber(p.value, 2)}<br/>`
          }
        })
        result += `合计: $${formatNumber(total, 2)}`
        return result
      }
    },
    legend: { data: ['车费', '小费', '附加费'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: types },
    yAxis: { type: 'value', name: '金额($)' },
    series
  }

  barChart.setOption(option)
}

const renderPieChart = () => {
  if (!pieChartRef.value || props.data.length === 0) return

  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const feeGroups: Record<string, number> = {}
  props.data.forEach(item => {
    const feeType = item.feeType || item.feeCode || item.dimension2Name || 'Unknown'
    feeGroups[feeType] = (feeGroups[feeType] || 0) + (item.totalAmount || item.totalRevenue || 0)
  })

  const sorted = Object.entries(feeGroups).sort((a, b) => b[1] - a[1]).slice(0, 6)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `<strong>${params.name}</strong><br/>金额: $${formatNumber(params.value, 2)}<br/>占比: ${params.percent.toFixed(1)}%`
      }
    },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['40%', '50%'],
      label: { show: true, formatter: '{b}: {d}%' },
      data: sorted.map(([fee, amount], idx) => ({
        name: fee,
        value: amount,
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
.taxi-type-fee-analysis {
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

.fee-breakdown {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .type-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .type-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .type-icon {
        font-size: 18px;
      }

      .type-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .type-fees {
      .fee-bar {
        display: flex;
        height: 6px;
        border-radius: 3px;
        overflow: hidden;
        background: #e5e7eb;
        margin-bottom: 6px;
      }

      .fee-legend {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: #6b7280;

          .legend-color {
            width: 8px;
            height: 8px;
            border-radius: 2px;
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

  .taxi-type-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #dcfce7;
    color: #15803d;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

  .fee-type-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #fef3c7;
    color: #b45309;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
  }
}
</style>
