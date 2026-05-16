<template>
  <div class="realtime-container">
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="page-title">实时监控</h2>
        <p class="page-subtitle">实时监控出租车运营数据</p>
      </div>
      <div class="header-right">
        <el-select v-model="refreshInterval" size="small" style="width: 120px">
          <el-option label="5秒" :value="5000" />
          <el-option label="10秒" :value="10000" />
          <el-option label="30秒" :value="30000" />
        </el-select>
        <el-button
          :type="isAutoRefresh ? 'primary' : 'default'"
          size="small"
          @click="toggleAutoRefresh"
        >
          {{ isAutoRefresh ? '暂停' : '自动刷新' }}
        </el-button>
        <div class="live-indicator" :class="{ paused: !isAutoRefresh }">
          <span class="live-dot"></span>
          <span>{{ isAutoRefresh ? '实时更新中' : '已暂停' }}</span>
        </div>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-indicator" style="background: #409eff"></div>
        <div class="kpi-content">
          <p class="kpi-label">当前订单数</p>
          <p class="kpi-value">{{ kpiData.orderCount.toLocaleString() }}</p>
          <p class="kpi-unit">单</p>
        </div>
        <div class="kpi-trend" :class="kpiData.orderGrowth >= 0 ? 'positive' : 'negative'">
          <span>{{ kpiData.orderGrowth >= 0 ? '+' : '' }}{{ kpiData.orderGrowth.toFixed(1) }}%</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-indicator" style="background: #67c23a"></div>
        <div class="kpi-content">
          <p class="kpi-label">5分钟收入</p>
          <p class="kpi-value">${{ formatNumber(kpiData.totalFare) }}</p>
          <p class="kpi-unit">USD</p>
        </div>
        <div class="kpi-trend" :class="kpiData.fareGrowth >= 0 ? 'positive' : 'negative'">
          <span>{{ kpiData.fareGrowth >= 0 ? '+' : '' }}{{ kpiData.fareGrowth.toFixed(1) }}%</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-indicator" style="background: #e6a23c"></div>
        <div class="kpi-content">
          <p class="kpi-label">平均费用</p>
          <p class="kpi-value">${{ kpiData.avgFare.toFixed(2) }}</p>
          <p class="kpi-unit">USD</p>
        </div>
        <div class="kpi-trend" :class="kpiData.avgFareGrowth >= 0 ? 'positive' : 'negative'">
          <span>{{ kpiData.avgFareGrowth >= 0 ? '+' : '' }}{{ kpiData.avgFareGrowth.toFixed(1) }}%</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-indicator" style="background: #909399"></div>
        <div class="kpi-content">
          <p class="kpi-label">数据更新时间</p>
          <p class="kpi-value">{{ lastUpdateTime }}</p>
          <p class="kpi-unit">最后更新</p>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">实时趋势</h3>
          <div class="chart-actions">
            <span class="time-range">最近24小时</span>
          </div>
        </div>
        <div class="chart-body">
          <v-chart :option="realtimeTrendOption" autoresize />
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">热点区域</h3>
          <div class="chart-actions">
            <el-button size="small" @click="loadHotspot">
              刷新
            </el-button>
          </div>
        </div>
        <div class="chart-body">
          <div class="hotspot-list" v-if="hotspotData.length > 0">
            <div
              v-for="(spot, index) in hotspotData"
              :key="index"
              class="hotspot-item"
            >
              <span class="hotspot-rank">{{ index + 1 }}</span>
              <div class="hotspot-info">
                <p class="hotspot-name">{{ spot.zoneName || spot.zone || 'Unknown' }}</p>
                <p class="hotspot-count">{{ spot.trip_count || spot.tripCount || 0 }} 单</p>
              </div>
            </div>
          </div>
          <div v-else class="empty-data">
            <p>暂无热点数据</p>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">区域订单分布</h3>
          <div class="chart-actions">
            <span class="time-range">TOP 10</span>
          </div>
        </div>
        <div class="chart-body">
          <v-chart :option="zoneDistributionOption" autoresize />
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">数据状态</h3>
        </div>
        <div class="chart-body">
          <div class="status-grid">
            <div class="status-row">
              <span class="status-label">数据源</span>
              <span class="status-icon success"></span>
              <span class="status-value">实时数据库</span>
            </div>
            <div class="status-row">
              <span class="status-label">更新时间</span>
              <span class="status-icon success"></span>
              <span class="status-value">{{ dataTime }}</span>
            </div>
            <div class="status-row">
              <span class="status-label">数据状态</span>
              <span class="status-icon" :class="isDataFresh ? 'success' : 'warning'"></span>
              <span class="status-value" :class="isDataFresh ? 'success' : 'warning'">{{ isDataFresh ? '实时' : '延迟' }}</span>
            </div>
            <div class="status-row highlight">
              <span class="status-label">24h订单</span>
              <span class="status-icon success"></span>
              <span class="status-value primary">{{ totalOrders24h.toLocaleString() }}</span>
            </div>
            <div class="status-row">
              <span class="status-label">峰值时段</span>
              <span class="status-icon warning"></span>
              <span class="status-value">{{ peakHour }} ({{ peakCount.toLocaleString() }}单)</span>
            </div>
            <div class="status-row">
              <span class="status-label">低谷时段</span>
              <span class="status-icon" :class="lowCount > 0 ? 'info' : ''"></span>
              <span class="status-value">{{ lowHour }} ({{ lowCount.toLocaleString() }}单)</span>
            </div>
            <div class="status-row">
              <span class="status-label">平均订单</span>
              <span class="status-icon"></span>
              <span class="status-value">{{ avgOrdersPerHour.toFixed(1) }}单/小时</span>
            </div>
            <div class="status-row">
              <span class="status-label">活跃区域</span>
              <span class="status-icon success"></span>
              <span class="status-value">{{ activeZones }}个</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { realtimeApi } from '@/api/realtime'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

interface KpiData {
  orderCount: number
  totalFare: number
  avgFare: number
  orderGrowth: number
  fareGrowth: number
  avgFareGrowth: number
  windowEnd?: string
}

const refreshInterval = ref(10000)
const isAutoRefresh = ref(true)
let timer: ReturnType<typeof setInterval> | null = null

const kpiData = ref<KpiData>({
  orderCount: 0,
  totalFare: 0,
  avgFare: 0,
  orderGrowth: 0,
  fareGrowth: 0,
  avgFareGrowth: 0
})

const lastUpdateTime = ref('--:--:--')

const hotspotData = ref<Array<{ zone?: string; zoneName?: string; trip_count?: number; tripCount?: number }>>([])

const trendData = ref<Array<{ hour: string; trip_count: number; total_fare: number }>>([])

const isDataFresh = computed(() => {
  if (!kpiData.value.windowEnd) return false
  const lastUpdate = new Date(kpiData.value.windowEnd).getTime()
  const now = Date.now()
  return now - lastUpdate < 600000
})

const dataTime = computed(() => {
  if (!kpiData.value.windowEnd) return '--'
  return new Date(kpiData.value.windowEnd).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const totalOrders24h = computed(() => {
  return trendData.value.reduce((sum, item) => sum + (item.trip_count || 0), 0)
})

const avgOrdersPerHour = computed(() => {
  const validHours = trendData.value.filter(item => item.trip_count && item.trip_count > 0)
  if (validHours.length === 0) return 0
  return totalOrders24h.value / Math.max(trendData.value.length, 1)
})

const peakHour = computed(() => {
  let peak = { hour: '--:--', count: 0 }
  trendData.value.forEach(item => {
    const count = item.trip_count || 0
    if (count > peak.count) {
      peak = { hour: item.hour ? new Date(item.hour).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--:--', count }
    }
  })
  return peak.hour
})

const peakCount = computed(() => {
  let peak = { count: 0 }
  trendData.value.forEach(item => {
    const count = item.trip_count || 0
    if (count > peak.count) {
      peak = { count }
    }
  })
  return peak.count
})

const lowHour = computed(() => {
  let low = { hour: '--:--', count: Number.MAX_VALUE }
  trendData.value.forEach(item => {
    const count = item.trip_count || 0
    if (count > 0 && count < low.count) {
      low = { hour: item.hour ? new Date(item.hour).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--:--', count }
    }
  })
  return low.hour
})

const lowCount = computed(() => {
  let low = { count: Number.MAX_VALUE }
  trendData.value.forEach(item => {
    const count = item.trip_count || 0
    if (count > 0 && count < low.count) {
      low = { count }
    }
  })
  return low.count === Number.MAX_VALUE ? 0 : low.count
})

const activeZones = computed(() => {
  return hotspotData.value.filter(item => (item.trip_count || item.tripCount || 0) > 0).length
})

const zoneDistributionOption = computed(() => {
  const zoneNames = hotspotData.value.map(item => item.zoneName || item.zone || 'Unknown')
  const counts = hotspotData.value.map(item => item.trip_count || item.tripCount || 0)

  const colors = ['#ef4444', '#f59e0b', '#3b82f6', '#8b5cf6', '#06b6d4', '#10b981', '#84cc16', '#f97316', '#ec4899', '#6366f1']

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 单 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '2%',
      top: 'center',
      textStyle: {
        fontSize: 10
      },
      itemWidth: 10,
      itemHeight: 10
    },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 1
      },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b}',
        fontSize: 9,
        overflow: 'truncate',
        maxWidth: 60
      },
      labelLine: {
        show: true,
        lineStyle: {
          type: 'dotted'
        }
      },
      data: counts.length > 0 ? counts.map((count, index) => ({
        name: zoneNames[index] || `区域${index + 1}`,
        value: count,
        itemStyle: {
          color: colors[index % colors.length]
        }
      })) : [{ name: '暂无数据', value: 1, itemStyle: { color: '#6b7280' } }],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      }
    }]
  }
})

const formatNumber = (num: number): string => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toFixed(0)
}

const loadKpi = async () => {
  try {
    const data = await realtimeApi.getKpi()
    kpiData.value = {
      orderCount: data.orderCount || 0,
      totalFare: data.totalFare || 0,
      avgFare: data.avgFare || 0,
      orderGrowth: data.orderGrowth || 0,
      fareGrowth: data.fareGrowth || 0,
      avgFareGrowth: data.avgFareGrowth || 0,
      windowEnd: data.windowEnd
    }
    const now = new Date()
    lastUpdateTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  } catch (error) {
    console.error('Failed to load KPI data:', error)
    kpiData.value = {
      orderCount: 0,
      totalFare: 0,
      avgFare: 0,
      orderGrowth: 0,
      fareGrowth: 0,
      avgFareGrowth: 0
    }
  }
}

const loadHotspot = async () => {
  try {
    const data = await realtimeApi.getHotspot(10)
    hotspotData.value = data || []
  } catch (error) {
    console.error('Failed to load hotspot data:', error)
    hotspotData.value = []
  }
}

const loadTrend = async () => {
  try {
    const data = await realtimeApi.getTrend()
    trendData.value = data || []
  } catch (error) {
    console.error('Failed to load trend data:', error)
    trendData.value = []
  }
}

const loadAllData = async () => {
  await Promise.all([
    loadKpi(),
    loadHotspot(),
    loadTrend()
  ])
}

const realtimeTrendOption = computed(() => {
  const hours = trendData.value.map(item => {
    const date = new Date(item.hour)
    return `${date.getHours().toString().padStart(2, '0')}:00`
  })
  const counts = trendData.value.map(item => item.trip_count || 0)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c} 单'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: hours.length > 0 ? hours : ['00:00'],
      axisLabel: {
        rotate: 45,
        interval: Math.floor(hours.length / 8)
      }
    },
    yAxis: {
      type: 'value',
      name: '订单数'
    },
    series: [{
      type: 'line',
      data: counts.length > 0 ? counts : [0],
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.5)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.1)' }
          ]
        }
      },
      lineStyle: {
        color: '#3b82f6',
        width: 2
      },
      itemStyle: {
        color: '#3b82f6'
      }
    }]
  }
})

const toggleAutoRefresh = () => {
  isAutoRefresh.value = !isAutoRefresh.value
  if (isAutoRefresh.value) {
    startTimer()
  } else {
    stopTimer()
  }
}

const startTimer = () => {
  stopTimer()
  timer = setInterval(loadAllData, refreshInterval.value)
}

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

watch(refreshInterval, () => {
  if (isAutoRefresh.value) {
    startTimer()
  }
})

onMounted(() => {
  loadAllData()
  startTimer()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style lang="scss" scoped>
.realtime-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f0f9ff;
  border-radius: 20px;
  font-size: 12px;
  color: #0369a1;

  &.paused {
    background: #f3f4f6;
    color: #6b7280;

    .live-dot {
      background: #9ca3af;
      animation: none;
    }
  }
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.kpi-indicator {
  width: 8px;
  height: 48px;
  border-radius: 4px;
}

.kpi-content {
  flex: 1;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px 0;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.kpi-unit {
  font-size: 12px;
  color: #9ca3af;
  margin: 0;
}

.kpi-trend {
  font-size: 14px;
  font-weight: 500;

  &.positive {
    color: #67c23a;
  }

  &.negative {
    color: #f56c6c;
  }
}

.chart-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-range {
  font-size: 12px;
  color: #6b7280;
}

.chart-body {
  padding: 20px;
  height: 320px;
}

.hotspot-list {
  padding: 0;
  height: 100%;
  overflow-y: auto;
}

.hotspot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }
}

.hotspot-rank {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.hotspot-info {
  flex: 1;
}

.hotspot-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.hotspot-count {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.empty-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
}

.status-list {
  padding: 0;
}

.status-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }

  &.highlight {
    background: linear-gradient(90deg, rgba(59, 130, 246, 0.05) 0%, transparent 100%);
    border-radius: 4px;
  }
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }
}

.status-icon {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.success {
    background: #67c23a;
  }

  &.warning {
    background: #e6a23c;
  }

  &.error {
    background: #f56c6c;
  }

  &.info {
    background: #909399;
  }
}

.status-label {
  flex: 1;
  font-size: 14px;
  color: #6b7280;
}

.status-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  font-family: 'Monaco', 'Menlo', monospace;

  &.success {
    color: #67c23a;
  }

  &.warning {
    color: #e6a23c;
  }

  &.error {
    color: #f56c6c;
  }

  &.primary {
    color: #3b82f6;
    font-weight: 600;
    font-size: 16px;
  }
}
</style>