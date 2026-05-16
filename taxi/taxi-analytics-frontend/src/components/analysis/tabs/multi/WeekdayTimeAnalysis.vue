<template>
  <div class="weekday-time-analysis">
    <div class="panel-header">
      <h3 class="panel-title">星期 × 时段 交叉分析</h3>
      <p class="panel-desc">分析工作日与周末在不同时段的订单分布与出行特征</p>
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
          <div class="summary-label">周末订单占比</div>
          <div class="summary-value">{{ weekendPercentage }}%</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">工作日高峰</div>
          <div class="summary-value">{{ weekdayPeak }}</div>
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

      <div class="weekday-summary">
        <div class="summary-row">
          <div class="summary-item">
            <span class="summary-label">工作日总订单</span>
            <span class="summary-value">{{ formatNumber(weekdayTrips) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">周末总订单</span>
            <span class="summary-value">{{ formatNumber(weekendTrips) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">工作日/周末比</span>
            <span class="summary-value">{{ weekdayWeekendRatio }}:1</span>
          </div>
        </div>
      </div>

      <div class="data-table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>星期</th>
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
                <span class="weekday-badge" :class="getWeekdayClass(item.dayOfWeek || item.dimension1Name)">
                  {{ item.dayName || item.dimension1Name || 'N/A' }}
                </span>
              </td>
              <td>
                <span class="time-badge">{{ formatHour(item.hour || item.dimension2Name || item.timeSlot) }}</span>
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
  if (!hour) return 'N/A'
  const h = typeof hour === 'string' ? parseInt(hour) : hour
  if (isNaN(h)) return hour.toString()
  return `${h}:00`
}

const tableData = computed(() => props.data.slice(0, 20))

const totalCombinations = computed(() => props.data.length)

const totalTrips = computed(() => {
  return props.data.reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const weekdayTrips = computed(() => {
  const weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', '周一', '周二', '周三', '周四', '周五', '1', '2', '3', '4', '5']
  return props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    return weekdays.some(w => day.includes(w))
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const weekendTrips = computed(() => {
  const weekends = ['Saturday', 'Sunday', '周六', '周日', '6', '7']
  return props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    return weekends.some(w => day.includes(w))
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)
})

const weekendPercentage = computed(() => {
  return totalTrips.value > 0 ? ((weekendTrips.value / totalTrips.value) * 100).toFixed(1) : '0'
})

const weekdayWeekendRatio = computed(() => {
  return weekendTrips.value > 0 ? (weekdayTrips.value / weekendTrips.value).toFixed(1) : '0'
})

const weekdayPeak = computed(() => {
  const weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', '周一', '周二', '周三', '周四', '周五', '1', '2', '3', '4', '5']
  const weekdayData = props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    return weekdays.some(w => day.includes(w))
  })
  if (weekdayData.length === 0) return 'N/A'
  const sorted = [...weekdayData].sort((a, b) => (b.tripCount || 0) - (a.tripCount || 0))
  return formatHour(sorted[0].hour || sorted[0].dimension2Name || sorted[0].timeSlot)
})

const insights = computed(() => {
  if (props.data.length === 0) return []
  const result = []

  const weekdayMorning = props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    const isWeekday = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', '周一', '周二', '周三', '周四', '周五'].some(w => day.includes(w))
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '-1')
    return isWeekday && hour >= 7 && hour <= 9
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)

  const weekendMorning = props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    const isWeekend = ['Saturday', 'Sunday', '周六', '周日'].some(w => day.includes(w))
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '-1')
    return isWeekend && hour >= 7 && hour <= 9
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (weekdayMorning > weekendMorning * 2) {
    result.push({
      icon: '🌅',
      type: 'highlight',
      title: '工作日早高峰显著',
      description: `工作日早高峰(7-9点)订单 ${formatNumber(weekdayMorning)} 单，是周末的 ${((weekdayMorning / weekendMorning) || 0).toFixed(1)} 倍`
    })
  }

  const weekendNight = props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    const isWeekend = ['Saturday', 'Sunday', '周六', '周日'].some(w => day.includes(w))
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '-1')
    return isWeekend && hour >= 22
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)

  const weekdayNight = props.data.filter(item => {
    const day = item.dayName || item.dimension1Name || ''
    const isWeekday = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', '周一', '周二', '周三', '周四', '周五'].some(w => day.includes(w))
    const hour = parseInt(item.hour || item.dimension2Name || item.timeSlot || '-1')
    return isWeekday && hour >= 22
  }).reduce((sum, item) => sum + (item.tripCount || 0), 0)

  if (weekendNight > weekdayNight) {
    result.push({
      icon: '🌙',
      type: 'positive',
      title: '周末夜间出行活跃',
      description: `周末夜间(22点后)订单 ${formatNumber(weekendNight)} 单，比工作日多 ${(((weekendNight - weekdayNight) / weekdayNight) * 100 || 0).toFixed(1)}%`
    })
  }

  return result
})

const getWeekdayClass = (day: string): string => {
  if (!day) return ''
  const d = day.toLowerCase()
  if (d.includes('sat') || d.includes('sun') || d.includes('六') || d.includes('日') || d.includes('6') || d.includes('7')) {
    return 'weekend'
  }
  return 'weekday'
}

const renderHeatmap = () => {
  if (!heatmapRef.value || props.data.length === 0) return

  heatmapChart?.dispose()
  heatmapChart = echarts.init(heatmapRef.value)

  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const hours = Array.from({ length: 24 }, (_, i) => i)

  const matrix: number[][] = []
  weekdays.forEach((_, dayIdx) => {
    const row: number[] = []
    hours.forEach(hour => {
      const dayName = weekdays[dayIdx]
      const item = props.data.find(d => {
        const dataDay = d.dayName || d.dimension1Name || ''
        const dataHour = parseInt(d.hour || d.dimension2Name || d.timeSlot || '-1')
        return (dataDay.includes(dayName) || dataDay === `${dayIdx + 1}`) && dataHour === hour
      })
      row.push(item?.tripCount || 0)
    })
    matrix.push(row)
  })

  const maxValue = Math.max(...matrix.flat())

  const option: echarts.EChartsOption = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `<strong>${weekdays[params[1]]} ${params[2]}:00</strong><br/>
                订单数: ${formatNumber(params.value)}<br/>
                占比: ${((params.value / maxValue) * 100).toFixed(1)}%`
      }
    },
    grid: { left: '8%', right: '8%', bottom: '15%', top: '10%' },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}:00`),
      axisLabel: { interval: 2 },
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: weekdays,
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
.weekday-time-analysis {
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
    height: 320px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

.weekday-summary {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  .summary-row {
    display: flex;
    justify-content: space-around;

    .summary-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .summary-label {
        font-size: 12px;
        color: #6b7280;
        margin-bottom: 4px;
      }

      .summary-value {
        font-size: 24px;
        font-weight: 600;
        color: #1f2937;
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

  .weekday-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;

    &.weekday {
      background: #dbeafe;
      color: #1d4ed8;
    }

    &.weekend {
      background: #dcfce7;
      color: #15803d;
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
  }
}
</style>
