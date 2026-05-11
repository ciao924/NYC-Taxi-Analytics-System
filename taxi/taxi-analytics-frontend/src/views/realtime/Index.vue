<template>
  <div class="realtime-container">
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">实时监控</h2>
        <div class="live-indicator">
          <span class="live-dot"></span>
          <span>实时更新中</span>
        </div>
      </div>
      <div class="header-controls">
        <el-select v-model="refreshInterval" size="small">
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
      </div>
    </div>

    <div class="realtime-grid">
      <div class="stat-card">
        <div class="stat-indicator" style="background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);"></div>
        <div class="stat-info">
          <p class="stat-label">当前订单数</p>
          <p class="stat-value">{{ realtimeData.orderCount.toLocaleString() }}</p>
        </div>
        <div class="stat-change positive">
          <span>+12.5%</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-indicator" style="background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);"></div>
        <div class="stat-info">
          <p class="stat-label">5分钟收入</p>
          <p class="stat-value">${{ realtimeData.totalFare.toLocaleString() }}</p>
        </div>
        <div class="stat-change positive">
          <span>+8.3%</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-indicator" style="background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);"></div>
        <div class="stat-info">
          <p class="stat-label">平均费用</p>
          <p class="stat-value">${{ realtimeData.avgFare.toFixed(2) }}</p>
        </div>
        <div class="stat-change negative">
          <span>-2.1%</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-indicator" style="background: linear-gradient(135deg, #909399 0%, #b4b8bf 100%);"></div>
        <div class="stat-info">
          <p class="stat-label">数据更新时间</p>
          <p class="stat-value">{{ lastUpdateTime }}</p>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-panel">
        <div class="panel-header">
          <h3>实时趋势</h3>
          <span class="time-range">最近24小时</span>
        </div>
        <div class="chart-wrapper">
          <v-chart :option="realtimeTrendOption" autoresize />
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-header">
          <h3>热点区域</h3>
          <el-select v-model="hotspotType" size="small">
            <el-option label="上车点" value="pickup" />
            <el-option label="下车点" value="dropoff" />
          </el-select>
        </div>
        <div class="hotspot-list">
          <div
            v-for="(spot, index) in hotspotData"
            :key="index"
            class="hotspot-item"
          >
            <span class="hotspot-rank">{{ index + 1 }}</span>
            <div class="hotspot-info">
              <p class="hotspot-name">{{ spot.zoneName }}</p>
              <p class="hotspot-count">{{ spot.tripCount }} 单</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-panel">
        <div class="panel-header">
          <h3>支付方式分布</h3>
        </div>
        <div class="chart-wrapper">
          <v-chart :option="paymentOption" autoresize />
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-header">
          <h3>实时告警</h3>
          <el-button size="small" type="primary" @click="viewAllAlerts">
            查看全部
          </el-button>
        </div>
        <div class="alert-list">
          <div
            v-for="(alert, index) in recentAlerts"
            :key="index"
            class="alert-item"
            :class="alert.severity"
          >
            <span class="alert-icon">{{ getAlertIcon(alert.severity) }}</span>
            <div class="alert-content">
              <p class="alert-title">{{ alert.title }}</p>
              <p class="alert-time">{{ alert.time }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
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

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const refreshInterval = ref(10000)
const isAutoRefresh = ref(true)
let timer: ReturnType<typeof setInterval> | null = null

const realtimeData = ref({
  orderCount: 1258,
  totalFare: 28567,
  avgFare: 22.71
})

const lastUpdateTime = ref('')

const hotspotData = ref([
  { zoneName: 'Midtown Center', tripCount: 156 },
  { zoneName: 'Lower Manhattan', tripCount: 134 },
  { zoneName: 'JFK Airport', tripCount: 98 },
  { zoneName: 'Times Square', tripCount: 87 },
  { zoneName: 'Central Park', tripCount: 65 }
])

const hotspotType = ref('pickup')

const recentAlerts = ref([
  { title: '数据延迟超过阈值', severity: 'warning', time: '2分钟前' },
  { title: '某区域订单异常激增', severity: 'info', time: '5分钟前' },
  { title: '支付服务响应变慢', severity: 'error', time: '8分钟前' }
])

const timeLabels = computed(() => {
  const labels: string[] = []
  const now = new Date()
  for (let i = 23; i >= 0; i--) {
    const hour = new Date(now.getTime() - i * 60 * 60 * 1000)
    labels.push(`${hour.getHours().toString().padStart(2, '0')}:00`)
  }
  return labels
})

const trendData = computed(() => {
  return Array.from({ length: 24 }, () => Math.floor(Math.random() * 500) + 800)
})

const realtimeTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: timeLabels.value, axisLabel: { rotate: 45 } },
  yAxis: { type: 'value', name: '订单数' },
  series: [{ type: 'line', data: trendData.value, smooth: true, areaStyle: {} }]
}))

const paymentOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}% ({d}%)' },
  series: [{ type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'], data: [
    { value: 65, name: '信用卡' },
    { value: 25, name: '现金' },
    { value: 10, name: '其他' }
  ]}]
}))

const getAlertIcon = (severity: string) => {
  switch (severity) {
    case 'error': return '!'
    case 'warning': return '⚠'
    default: return 'ℹ'
  }
}

const updateData = () => {
  realtimeData.value = {
    orderCount: realtimeData.value.orderCount + Math.floor(Math.random() * 50) - 20,
    totalFare: realtimeData.value.totalFare + Math.floor(Math.random() * 1000) - 400,
    avgFare: Math.max(15, realtimeData.value.avgFare + (Math.random() * 2 - 1))
  }
  const now = new Date()
  lastUpdateTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
}

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
  timer = setInterval(updateData, refreshInterval.value)
}

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const viewAllAlerts = () => {
  console.log('View all alerts')
}

onMounted(() => {
  updateData()
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border-radius: 12px;
  color: white;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 12px;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #67c23a;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.realtime-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-indicator {
  width: 8px;
  height: 44px;
  border-radius: 4px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.stat-change {
  font-size: 13px;
  font-weight: 500;

  &.positive {
    color: #67c23a;
  }

  &.negative {
    color: #f56c6c;
  }
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.panel-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.time-range {
  font-size: 12px;
  color: #6b7280;
}

.chart-wrapper {
  padding: 20px;
  height: 280px;
}

.hotspot-list {
  padding: 16px;
}

.hotspot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }
}

.hotspot-rank {
  width: 24px;
  height: 24px;
  background: #409eff;
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

.alert-list {
  padding: 16px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  &.error {
    background: #fef2f2;
    color: #dc2626;
  }

  &.warning {
    background: #fffbeb;
    color: #d97706;
  }

  &.info {
    background: #eff6ff;
    color: #2563eb;
  }
}

.alert-icon {
  font-size: 16px;
  font-weight: bold;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  margin: 0 0 4px 0;
}

.alert-time {
  font-size: 11px;
  opacity: 0.7;
  margin: 0;
}
</style>
