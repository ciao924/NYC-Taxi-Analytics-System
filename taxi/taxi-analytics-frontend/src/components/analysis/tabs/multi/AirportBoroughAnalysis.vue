<template>
  <div class="airport-borough-analysis">
    <div class="panel-header">
      <h3 class="panel-title">机场 × 区域 交叉分析</h3>
      <p class="panel-desc">分析三大机场与纽约各区之间的出行流向与收入分布</p>
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
          <div class="summary-label">热门流向</div>
          <div class="summary-value highlight-text">{{ topFlow }}</div>
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
          <div ref="sankeyRef" class="chart-area"></div>
        </div>
      </div>

      <div class="flow-grid">
        <div v-for="flow in topFlows" :key="flow.key" class="flow-card">
          <div class="flow-header">
            <span class="flow-airport">{{ flow.airport }}</span>
            <span class="flow-arrow">→</span>
            <span class="flow-borough">{{ flow.borough }}</span>
          </div>
          <div class="flow-stats">
            <div class="stat-item">
              <span class="stat-label">订单</span>
              <span class="stat-value">{{ formatNumber(flow.tripCount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">收入</span>
              <span class="stat-value">${{ formatNumber(flow.revenue, 2) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>机场</th>
              <th>区域</th>
              <th>订单数</th>
              <th>占比</th>
              <th>总收入</th>
              <th>平均金额</th>
              <th>平均距离</th>
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
                <span class="borough-badge">{{ item.borough || item.dimension2Name || 'N/A' }}</span>
              </td>
              <td>{{ formatNumber(item.tripCount) }}</td>
              <td>{{ formatNumber(item.percentage, 1) }}%</td>
              <td class="amount-cell">${{ formatNumber(item.totalAmount || item.totalRevenue || 0, 2) }}</td>
              <td class="amount-cell">${{ formatNumber(item.avgAmount || item.avgFare || 0, 2) }}</td>
              <td class="amount-cell">{{ formatNumber(item.avgDistance || 0, 1) }} 英里</td>
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

const sankeyRef = ref<HTMLElement | null>(null)
let sankeyChart: echarts.ECharts | null = null

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

const topFlow = computed(() => {
  if (props.data.length === 0) return 'N/A'
  const sorted = [...props.data].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  const top = sorted[0]
  return `${top.airportCode || top.dimension1Name} → ${top.borough || top.dimension2Name}`
})

const topFlows = computed(() => {
  const sorted = [...props.data].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0)).slice(0, 6)
  return sorted.map(item => ({
    key: `${item.airportCode || item.dimension1Name}-${item.borough || item.dimension2Name}`,
    airport: item.airportCode || item.dimension1Name || 'N/A',
    borough: item.borough || item.dimension2Name || 'N/A',
    tripCount: item.tripCount || 0,
    revenue: item.totalAmount || item.totalRevenue || 0
  }))
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const jfkToManhattan = props.data.find(item =>
    (item.airportCode || item.dimension1Name || '').toUpperCase() === 'JFK' &&
    (item.borough || item.dimension2Name || '').toLowerCase().includes('manhattan')
  )

  if (jfkToManhattan) {
    result.push({
      icon: '✈️',
      type: 'highlight',
      title: 'JFK→曼哈顿最热门',
      description: `JFK机场到曼哈顿订单 ${formatNumber(jfkToManhattan.tripCount)} 单，收入 $${formatNumber(jfkToManhattan.totalAmount || jfkToManhattan.totalRevenue, 2)}`
    })
  }

  const lgaData = props.data.filter(item =>
    (item.airportCode || item.dimension1Name || '').toUpperCase() === 'LGA'
  )
  const lgaTrips = lgaData.reduce((sum, item) => sum + (item.tripCount || 0), 0)
  const lgaQueens = lgaData.filter(item =>
    (item.borough || item.dimension2Name || '').toLowerCase().includes('queens')
  ).reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (lgaTrips > 0) {
    const queensRatio = ((lgaQueens / lgaTrips) * 100).toFixed(1)
    result.push({
      icon: '🗺️',
      type: 'info',
      title: 'LGA本地出行活跃',
      description: `LGA机场 ${queensRatio}% 的订单流向Queens区域，显示本地出行特征明显`
    })
  }

  return result
})

const getAirportClass = (code: string): string => {
  if (!code) return ''
  return code.toUpperCase()
}

const renderSankey = () => {
  if (!sankeyRef.value || props.data.length === 0) return

  sankeyChart?.dispose()
  sankeyChart = echarts.init(sankeyRef.value)

  const airports = ['JFK', 'LGA', 'EWR']
  const boroughs = ['Manhattan', 'Brooklyn', 'Queens', 'Bronx', 'Staten Island']

  const nodes = [
    ...airports.map(name => ({ name })),
    ...boroughs.map(name => ({ name }))
  ]

  const linkData: Array<{ source: string; target: string; value: number }> = []
  props.data.forEach((item: any) => {
    const airport = item.airportCode || item.source || item.dimension1Name
    const borough = item.borough || item.target || item.dimension2Name
    if (airport && borough) {
      linkData.push({
        source: String(airport),
        target: String(borough),
        value: item.tripCount || item.flowCount || 1
      })
    }
  })

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      formatter: (params: any) => {
        if (params.dataType === 'edge') {
          return `<strong>${params.data.source} → ${params.data.target}</strong><br/>订单数: ${formatNumber(params.data.value)}`
        }
        return `<strong>${params.name}</strong>`
      }
    },
    series: [{
      type: 'sankey',
      emphasis: {
        focus: 'adjacency' as const
      },
      nodeAlign: 'justify' as const,
      nodeGap: 8,
      nodeWidth: 20,
      data: nodes,
      links: linkData,
      lineStyle: {
        curveness: 0.5
      }
    }]
  }

  sankeyChart.setOption(option)
}

watch(() => props.data, async () => {
  await nextTick()
  renderSankey()
}, { deep: true })

watch(() => props.loading, async (isLoading) => {
  if (!isLoading) {
    await nextTick()
    renderSankey()
  }
})

onMounted(() => {
  if (props.data.length > 0) {
    renderSankey()
  }
})
</script>

<style lang="scss" scoped>
.airport-borough-analysis {
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
    height: 300px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .flow-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px;

    .flow-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .flow-airport {
        font-size: 14px;
        font-weight: 600;
        color: #3b82f6;
      }

      .flow-arrow {
        color: #9ca3af;
      }

      .flow-borough {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .flow-stats {
      display: flex;
      gap: 16px;

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

  .borough-badge {
    display: inline-block;
    padding: 2px 8px;
    background: #ede9fe;
    color: #6d28d9;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }

  .amount-cell {
    font-family: 'SF Mono', Monaco, monospace;
  }
}
</style>
